package de.yeetus.ue4.net;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

public class NetConnection {
	static final Logger LOG = LoggerFactory.getLogger(NetConnection.class);

	public final NetDriver<?> driver;
	public ByteOrder endianness = ByteOrder.nativeOrder();
	final NetChannel[] channels = new NetChannel[NetDriver.MAX_CHANNELS];
	public NetHandler handler;
	private boolean isDead;

	// Outbound
	private int outPacketId;
	private int outAckPacketId = -1;
	private long outLastPacketTs = 0;
	private List<Frame> outPacket = new ArrayList<Frame>();
	private boolean isTimeSensitive;
	int[] outReliable = new int[NetDriver.MAX_CHANNELS];
	int outPartial = -1;

	// Inbound
	private int inPacketId = -1;
	private long inLastPacketTs = System.currentTimeMillis();
	int[] inReliable = new int[NetDriver.MAX_CHANNELS];
	private int inPartial = -1;

	protected NetConnection(NetDriver<?> driver) {
		this.driver = driver;
	}

	protected void tick() {
		if(System.currentTimeMillis() - inLastPacketTs > driver.timeoutTime) {
			cleanUp();
			driver.kill(this, true);
		}

		if(isDead) return;

		handler.tick();

		if(isTimeSensitive || System.currentTimeMillis() - outLastPacketTs > driver.keepAliveTime) {
			flush();
		}
	}

	int write(Frame frame) {
		int currentBitCount = 3 << 3;
		for(Frame f : outPacket) currentBitCount += f.calcBitCount();

		int frameBitCount = frame.calcBitCount();
		if(currentBitCount + frameBitCount > NetDriver.MAX_PACKET) {
			flush();
		}

		if(!(frame instanceof Ack)) isTimeSensitive = true;
		outPacket.add(frame);
		int id = outPacketId;
		if(isDead) flush(); // if the connection is closed, we only send if we need to. This way, a dropped close bunch can get retransmitted.
		return id;
	}
	private void flush() {
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_PACKET);
		Archive ar = new Archive(buf, endianness);
		ar.writePIntWrapped(NetDriver.MAX_PACKETID, outPacketId);

		while(!outPacket.isEmpty()) {
			Frame frame = outPacket.remove(0);
			frame.write(ar, driver.isServer);
			ReferenceCountUtil.release(frame);
		}

		ar.writeBoolean(true);
		ar.byteAlign();
		assert buf.readableBytes() < NetDriver.MAX_PACKET : "Packet exceeds maximum packet size";
		driver.output(this, buf);

		outPacketId++;
		outLastPacketTs = System.currentTimeMillis();
		isTimeSensitive = false;
	}

	private void receivedNak(int packetId) {
		for(NetChannel ch : channels) if(ch != null) ch.receivedNak(packetId);
	}
	private void receivedAck(int packetId) {
		for(NetChannel ch : channels) if(ch != null) ch.receivedAck(packetId);
	}
	private void receivedPacket(Archive ar) {
		if(isDead) return;

		inLastPacketTs = System.currentTimeMillis();

		int packetId = ar.readPInt(NetDriver.MAX_PACKETID);
		packetId = NetDriver.makeRelative(packetId, inPacketId, NetDriver.MAX_PACKETID);

		if(inPacketId >= packetId) return;
		inPacketId = packetId;

		while(true) {
			int type = ar.readBit();
			
			if(type == 1 && ar.readableBits() < 8) break;
			if(type == Ack.TYPE) {
				Ack frame = new Ack();
				frame.read(ar, driver.isServer);
				frame.packetId = NetDriver.makeRelative(frame.packetId, outAckPacketId, NetDriver.MAX_PACKETID);

				if(frame.packetId > outAckPacketId) {
					for(int nakPacketId = outAckPacketId + 1; nakPacketId < frame.packetId; nakPacketId++) {
						receivedNak(nakPacketId);
					}
					outAckPacketId = frame.packetId;
				}

				receivedAck(frame.packetId);
			} else if(type == Bunch.TYPE) {
				Bunch frame = new Bunch();
				frame.read(ar, driver.isServer);
				if(frame.reliable) frame.seqNum = NetDriver.makeRelative(frame.seqNum, inReliable[frame.channel], NetDriver.MAX_CHSEQUENCE);
				else if(frame.partial) {
					frame.seqNum = NetDriver.makeRelative(frame.seqNum, inPartial, NetDriver.MAX_CHSEQUENCE);
					if(frame.seqNum > inPartial) inPartial = frame.seqNum;
				} else frame.seqNum = 0;
				if(frame.reliable && frame.seqNum <= inReliable[frame.channel]) {
					frame.release();
					continue;
				}

				NetChannel channel = channels[frame.channel];

				if(channel == null && !frame.reliable) {
					boolean validUnreliableOpen = frame.open && (frame.close || frame.partial);
					if(!validUnreliableOpen) {
						frame.release();
						continue;
					}
				}

				if(channel == null) {
					channel = new NetChannel(this, frame.channelType);
					channel.index = frame.channel;
					channel.state = NetChannel.OPEN;
					channels[frame.channel] = channel;
					if(handler.acceptNewChannel(channel)) {
						LOG.debug("Opening " + channel + " from remote");
					} else {
						LOG.warn("Rejected " + channel + " from remote");
						channel.close(false);
					}
				}

				channel.receivedRawBunch(frame);
			} else throw new UnsupportedOperationException("Unknown frame type " + type);
		}

		write(new Ack(packetId));
	}
	void receivedRawPacket(ByteBuf buf) {
		// TODO error handling
		receivedPacket(new Archive(buf, endianness));
		buf.release();
	}

	int getNewChannelId(NetChannel channel) {
		switch(channel.type) {
			case CONTROL: 
				assert channels[0] == null : "Tried to open a control channel, but we already have a control channel";
				channels[0] = channel;
				return 0;
			case BATTLEYE: 
				assert channels[3] == null : "Tried to open a BattlEye channel, but we already have a BattlEye channel";
				channels[3] = channel;
				return 3;
			case ACTOR: 
				for(int i = 4; i < channels.length; i++) {
					if(channels[i] == null) {
						channels[i] = channel;
						return i;
					}
				}
				throw new AssertionError("too many channels!");
			default: 
				throw new AssertionError("unknown channel type " + channel.type);
		}
	}

	public void cleanUp() {
		if(!isDead) {
			handler.cleanUp();
			flush();
			driver.kill(this, false);
		}
		isDead = true;
	}

	public boolean isDead() { return isDead; }
}
