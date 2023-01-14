package de.yeetus.ue4.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class IpNetDriver extends NetDriver<SocketAddress> {
	private DatagramChannel channel;
	private DatagramSocket socket;
	private ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_PACKET);

	public IpNetDriver(boolean isServer, Function<NetConnection, ? extends NetHandler> handler) throws IOException {
		super(isServer, handler);
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		socket = channel.socket();
	}

	public void bind(SocketAddress addr) throws SocketException {
		socket.bind(addr);
	}

	protected void receive(List<Map.Entry<SocketAddress, ByteBuf>> output) {
		try {
			while(true) {
				buffer.clear();
				SocketAddress addr = channel.receive(buffer);
				if(addr == null) break;
				buffer.flip();
				output.add(Map.entry(addr, ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_PACKET).writeBytes(buffer)));
			}
		} catch (IOException e) {
			throw new RuntimeException("IpNetDriver Receive Error", e);
		}
	}
	protected void send(SocketAddress addr, ByteBuf buf) {
		try {
			buffer.clear();
			buffer.limit(buf.readableBytes());
			buf.readBytes(buffer);
			buffer.flip();
			channel.send(buffer, addr);
		} catch (IOException e) {
			throw new RuntimeException("IpNetDriver Send Error", e);
		} finally {
			buf.release();
		}
	}
	protected void killLowLevel(SocketAddress addr) {
		// We don't support any "close" message.
		// We'll have to rely on closing the control channel or timing out.
	}
	protected void shutdown() {
		try {
			socket.close();
			channel.close();
		} catch (IOException e) {
			throw new RuntimeException("IpNetDriver Close Error", e);
		}
	}
}
