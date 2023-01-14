package de.yeetus.ue4.net;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;

public abstract class NetDriver<Id> {
	private static final Logger LOG = LoggerFactory.getLogger(NetDriver.class);
	public static final int MAX_PACKET = 512;
	public static final int MAX_MESSAGE = MAX_PACKET * 128;
	public static final int MAX_PACKETID = 16384;
	public static final int MAX_CHANNELS = 10240;
	public static final int MAX_CHSEQUENCE = 8192;
	public static final int PING_ACK_PACKET_INTERVAL = 16;

	public final boolean isServer;
	private final BiMap<Id, NetConnection> connections = HashBiMap.create();
	private final List<Map.Entry<Id, ByteBuf>> incomingPackets = new ArrayList<Map.Entry<Id, ByteBuf>>();
	private Function<NetConnection, ? extends NetHandler> handler;
	private boolean hasClosed;
	public long keepAliveTime = 200;
	public long timeoutTime = 120000;
	public long netspeedTime = 30000;

	public NetDriver(boolean isServer, Function<NetConnection, ? extends NetHandler> handler) {
		this.isServer = isServer;
		this.handler = handler;
	}

	public void tick() {
		if(hasClosed) return;

		try {
			handleTick();
		} catch (Throwable e) {
			LOG.error("Error ticking NetDriver", e);
			LOG.error("Shutting down.");
			close();
		}
	}

	private void handleTick() {
		// Receive packets
		receive(incomingPackets);

		// Hand the packets over to the corresponding connections
		while(!incomingPackets.isEmpty()) {
			Map.Entry<Id, ByteBuf> packet = incomingPackets.remove(0);
			Id addr = packet.getKey();
			ByteBuf content = packet.getValue();

			NetConnection conn = connections.get(addr);
			
			// If we don't have an active connection to this peer, check if we should create one
			if(conn == null) {
				if(!acceptIncomingConnection(addr)) {
					content.release();
					continue;
				}

				conn = new NetConnection(this);
				connections.put(addr, conn);
				LOG.debug("Accepting connection from " + addr);
				handler.apply(conn);
			}

			conn.receivedRawPacket(content);
		}

		// Tick all connections
		for(NetConnection conn : new HashSet<NetConnection>(connections.values())) {
			conn.tick();
		}
	}

	public NetConnection connect(Id addr) {
		if(isServer) throw new IllegalStateException("can't connect to server because we are a server");
		if(connections.containsKey(addr)) throw new IllegalStateException(String.format("already have an active connection to %s", addr));
		LOG.debug("Connecting to " + addr);
		NetConnection conn = new NetConnection(this);
		connections.put(addr, conn);
		handler.apply(conn);
		return conn;
	}
	
	public void close() {
		hasClosed = true;
		// Kill all connections
		for(NetConnection conn : new HashSet<NetConnection>(connections.values())) {
			conn.cleanUp();
		}
		// Purge any incoming packets that were never processed
		while(!incomingPackets.isEmpty()) {
			Map.Entry<Id, ByteBuf> packet = incomingPackets.remove(0);
			ByteBuf content = packet.getValue();
			content.release();
		}
	}

	// By default, we will accept connections as the server and reject them as the client.
	protected boolean acceptIncomingConnection(Id addr) { return isServer; }

	void output(NetConnection conn, ByteBuf buf) {
		send(connections.inverse().get(conn), buf);
	}

	void kill(NetConnection conn, boolean fullKill) {
		killLowLevel(connections.inverse().get(conn));
		if(fullKill) connections.inverse().remove(conn);
	}
	
	protected abstract void receive(List<Map.Entry<Id, ByteBuf>> output);
	protected abstract void send(Id addr, ByteBuf buf);
	protected abstract void killLowLevel(Id addr);
	protected abstract void shutdown();

	public static int bestSignedDifference(int value, int reference, int max) {
		return ((value-reference+max/2) & (max-1)) - max/2;
	}
	public static int makeRelative(int value, int reference, int max) {
		return reference + bestSignedDifference(value, reference, max);
	}
}
