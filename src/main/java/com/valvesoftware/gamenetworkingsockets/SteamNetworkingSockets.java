package com.valvesoftware.gamenetworkingsockets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.valvesoftware.gamenetworkingsockets.ll.GameNetworkingSockets;
import com.valvesoftware.gamenetworkingsockets.ll.SteamNetConnectionStatusChangedCallbackInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteamNetworkingSockets {
	private static Logger LOG = LoggerFactory.getLogger(SteamNetworkingSockets.class);
	public static SteamNetworkingSockets INSTANCE;
	public static SteamNetworkingSockets SERVER_INSTANCE;

	static {
		byte[] error = new byte[1024];
		if(!GameNetworkingSockets.INSTANCE.GameNetworkingSockets_Init(null, error)) {
			throw new Error(Native.toString(error));
		}
		INSTANCE = new SteamNetworkingSockets(GameNetworkingSockets.INSTANCE.SteamNetworkingSockets());
		SERVER_INSTANCE = new SteamNetworkingSockets(GameNetworkingSockets.INSTANCE.SteamNetworkingSocketsGameServer());
	}

	Pointer instance;
	Map<Integer, SteamListenSocket> listenSockets = new HashMap<Integer, SteamListenSocket>();
	Map<Integer, SteamConnection> connections = new HashMap<Integer, SteamConnection>();
	private Thread updateThread;
	@SuppressWarnings("unused") private Callback logCallback; // We need to save this here so it doesn't get garbage collected

	private SteamNetworkingSockets(Pointer instance) {
		this.instance = instance;
		GameNetworkingSockets.INSTANCE.SteamNetworkingSockets_SetDebugOutputFunction(instance, logCallback = new Callback() {
			@SuppressWarnings("unused")
			public void invoke(int type, String message) {
				if(type >= 8) LOG.trace(message);
				else if(type >= 6) LOG.debug(message);
				else if(type >= 4) LOG.info(message);
				else if(type >= 3) LOG.warn(message);
				else if(type >= 1) LOG.error(message);
			}
		});
	}

	private void startUpdateThread() {
		if(updateThread == null) {
			updateThread = new Thread() {
				public void run() {
					try {
						IntByReference ctx = new IntByReference(69420);
						Callback callback = new Callback() {
							@SuppressWarnings("unused")
							public void invoke(SteamNetConnectionStatusChangedCallbackInfo info, IntByReference context) {
								if(connections.get(info.hConn) == null) {
									connections.put(info.hConn, new SteamConnection(SteamNetworkingSockets.this, info.hConn));
								}
								SteamConnection conn = connections.get(info.hConn);
								if(conn.getListenSocket() != null && conn.getState() == ConnectionState.Connecting) {
									// Someone is trying to connect
									conn.getListenSocket().connecting.add(conn);
								}
							}
						};
						while(true) {
							GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_RunConnectionStatusChangedCallbacks(instance, callback, ctx);
							Thread.sleep(10);
						}
					} catch (Throwable e) {
						LOG.error("Update thread crashed!", e);
					}
				}
			};
			updateThread.start();
		}
	}

	public SteamListenSocket listen(Inet4Address ip, short port) {
		startUpdateThread();
		int hSocket = GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_CreateListenSocket(instance, -1, ipToInt(ip), port);
		if(hSocket == 0) throw new Error("Could not initialize listen socket");
		SteamListenSocket socket = new SteamListenSocket(this, hSocket);
		listenSockets.put(hSocket, socket);
		return socket;
	}

	public SteamConnection connect(Inet4Address ip, short port) {
		startUpdateThread();
		int hConn = GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_ConnectByIPv4Address(instance, ipToInt(ip), port);
		if(hConn == 0) throw new Error("Could not initialize connection");
		SteamConnection conn = new SteamConnection(this, hConn);
		connections.put(hConn, conn);
		return conn;
	}

	static int ipToInt(Inet4Address ip) {
		return ByteBuffer.allocate(4).put(ip.getAddress()).position(0).getInt();
	}
	static Inet4Address intToIp(int ip) {
		if(ip == 0) return null;
		byte[] bytes = new byte[4];
		ByteBuffer.allocate(4).putInt(ip).position(0).get(bytes);
		try {
			return (Inet4Address) InetAddress.getByAddress(bytes);
		} catch (IOException e) {
			// this should never happen
			throw new UncheckedIOException(e);
		}
	}
}
