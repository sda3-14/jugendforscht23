package de.yeetus.ue4.net;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;

public class NetChannel {
	static final int PRE_OPEN = 0;
	static final int OPENING = 1;
	static final int OPEN = 2;
	static final int CLOSING = 3;
	static final int CLOSED = 4;

	public final NetConnection connection;
	public final NetChType type;
	private EmbeddedChannel internalChannel;
	private boolean unreliable;
	int state = PRE_OPEN;
	private boolean dormant;
	Integer index;
	private NetMsg reliablePartial;
	private NetMsg unreliablePartial;
	private int unreliablePartialSeqNum;
	private List<Bunch> inWaiting = new LinkedList<Bunch>();
	private List<OutReliableBunchInfo> outWaiting = new LinkedList<OutReliableBunchInfo>();

	public NetChannel(NetConnection connection, NetChType type) {
		this.connection = connection;
		this.type = type;
		internalChannel = new EmbeddedChannel(new ChannelId() {
			public int compareTo(ChannelId o) { return asLongText().compareTo(o.asLongText()); }
			public String asShortText() { return type.toString(); }
			public String asLongText() { return NetChannel.this.toString(); }
		}) {
			protected void handleOutboundMessage(Object msg) {
				if(msg instanceof NetMsg) {
					sendMessage((NetMsg) msg);
				}
			}
		};
	}

	public String toString() {
		return "Channel{" + connection + "," + type + "," + state + "," + index + "}";
	}

	public boolean isOpen() { return state <= OPEN; }
	public void assertValid() {
		if(!isOpen()) throw new IllegalStateException("channel is not open");
	}

	private void sendMessage(NetMsg msg) {
		if(unreliable) assert !msg.reliable : "Tried to send reliable message through unreliable channel";

		boolean openChannel = state < OPENING;
		if(openChannel) {
			index = connection.getNewChannelId(this);

			NetConnection.LOG.debug("Opening " + this);
			state = OPENING;
		}

		List<Bunch> bunches = new ArrayList<Bunch>();

		// Process Bytes
		while(msg.readableBits()>0) {
			Bunch bunch = new Bunch();
			bunch.channel = index;
			bunch.channelType = type;
			bunch.reliable = msg.reliable;
			Archive bunchContent = new Archive(ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_PACKET - 10), msg.endianness);
			bunchContent.writeBits(msg, Math.min(msg.readableBits(), (NetDriver.MAX_PACKET - 10) << 3));
			bunch.content = new Bunch.Content.Bytes(bunchContent);
			bunches.add(bunch);
		}

		// Add empty bunch if no data is present (we might still need to transmit state information)
		if(bunches.isEmpty()) {
			Bunch bunch = new Bunch();
			bunch.channel = index;
			bunch.channelType = type;
			bunch.reliable = msg.reliable;
			bunches.add(bunch);
		}

		// If >1 bunch, mark as partial
		if(bunches.size() > 1) {
			for(Bunch bunch : bunches) bunch.partial = true;
			bunches.get(0).partialFirst = true;
			bunches.get(bunches.size()-1).partialLast = true;
		}
		// Add sequence numbers
		for(Bunch bunch : bunches) {
			if(bunch.reliable) bunch.seqNum = ++connection.outReliable[index];
			else if(bunch.partial) bunch.seqNum = ++connection.outPartial;
			else bunch.seqNum = 0;
		}
		// Mark open/close bunches
		if(openChannel) bunches.get(0).open = true;
		if(state == CLOSING) {
			bunches.get(bunches.size()-1).close = true;
			bunches.get(bunches.size()-1).dormant = dormant;
		}

		for(Bunch bunch : bunches) {
			OutReliableBunchInfo info = new OutReliableBunchInfo(bunch.retain(), connection.write(bunch));
			if(bunch.reliable) outWaiting.add(info);
			else info.bunch.release();
		}

		msg.release();
	}
	public void close(boolean dormant) {
		assertValid();

		NetConnection.LOG.debug("Closing channel " + this + (dormant?" as dormant":""));
		this.dormant = dormant;
		state = CLOSING;
		internalChannel.pipeline().fireUserEventTriggered(new ChannelCloseEvent());

		sendMessage(new NetMsg(connection));
	}

	private void receivedNextBunch(Bunch bunch) {
		if(bunch.reliable) {
			connection.inReliable[index] = bunch.seqNum;
		}
		
		if(bunch.partial) {
			if(bunch.reliable) {
				// Reliabe Partial
				if(bunch.partialFirst) {
					assert reliablePartial == null : "Received first partial bunch but still have active partial";
					reliablePartial = new NetMsg(connection, bunch.reliable);
				}
				assert reliablePartial != null : "Received partial bunch but no active partial";
				reliablePartial.addBunch(bunch);
				if(bunch.partialLast) {
					internalChannel.writeInbound(reliablePartial);
					reliablePartial = null;
				}
			} else {
				// Unreliable Partial
				if(bunch.partialFirst) {
					if(unreliablePartial != null) unreliablePartial.release();
					unreliablePartial = new NetMsg(connection, bunch.reliable);
					unreliablePartialSeqNum = bunch.seqNum-1;
				}
				if(unreliablePartial == null) return;
				if(unreliablePartialSeqNum+1 != bunch.seqNum) return;
				unreliablePartial.addBunch(bunch);
				unreliablePartialSeqNum++;
				if(bunch.partialLast) {
					internalChannel.writeInbound(unreliablePartial);
					unreliablePartial = null;
				}
			}
		} else {
			NetMsg msg = new NetMsg(connection, bunch.reliable);
			msg.addBunch(bunch);
			internalChannel.writeInbound(msg);
		}

		if(bunch.close) {
			NetConnection.LOG.debug("Closing channel " + this + (bunch.dormant?" as dormant":"") + " from remote");
			dormant = bunch.dormant;
			cleanUp();
		}

		bunch.release();
	}
	void receivedRawBunch(Bunch bunch) {
		if(bunch.reliable && bunch.seqNum != connection.inReliable[index]+1) {
			int i = 0;
			for(; i < inWaiting.size(); i++) {
				if(bunch.seqNum == inWaiting.get(i).seqNum) { // already have this one
					bunch.release();
					return;
				}
				if(bunch.seqNum < inWaiting.get(i).seqNum) { // put before this one
					break;
				}
			}
			inWaiting.add(i, bunch);
		} else {
			receivedNextBunch(bunch);
			while(!inWaiting.isEmpty()) {
				if(inWaiting.get(0).seqNum == connection.inReliable[index]+1) receivedNextBunch(inWaiting.remove(0));
				else break;
			}
		}
	}
	void receivedAck(int packetId) {
		List<OutReliableBunchInfo> affectedBunches = outWaiting.stream().filter(info -> info.packetId == packetId).toList();
		outWaiting.removeAll(affectedBunches);
		for(OutReliableBunchInfo info : affectedBunches) {
			if(info.bunch.open && state < OPEN) state = OPEN;
			if(info.bunch.close && state < CLOSED) cleanUp(); // Close message was acked, we can kill this channel now.
			info.bunch.release();
		}
	}
	void receivedNak(int packetId) {
		List<OutReliableBunchInfo> affectedBunches = outWaiting.stream().filter(info -> info.packetId == packetId).toList();
		for(OutReliableBunchInfo info : affectedBunches) info.packetId = connection.write(info.bunch.retain());
	}

	private void cleanUp() {
		if(state <= OPEN) internalChannel.pipeline().fireUserEventTriggered(new ChannelCloseEvent());
		state = CLOSED;
		internalChannel.close();
		if(unreliablePartial != null) unreliablePartial.release();
		if(reliablePartial != null) reliablePartial.release();
		if(!inWaiting.isEmpty()) NetConnection.LOG.warn("Closed channel while packets out of order");
		for(Bunch b : inWaiting) b.release();
		if(index != null) connection.channels[index] = null;
	}

	private static class OutReliableBunchInfo {
		public Bunch bunch;
		public int packetId;
		
		public OutReliableBunchInfo(Bunch bunch, int packetId) {
			this.bunch = bunch;
			this.packetId = packetId;
		}
	}

	public ChannelPipeline pipeline() { return internalChannel.pipeline(); }
}
