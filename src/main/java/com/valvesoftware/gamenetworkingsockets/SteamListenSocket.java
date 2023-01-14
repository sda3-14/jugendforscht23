package com.valvesoftware.gamenetworkingsockets;

import java.net.Inet4Address;
import java.util.LinkedList;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;
import com.valvesoftware.gamenetworkingsockets.ll.GameNetworkingSockets;

public class SteamListenSocket {
	private final SteamNetworkingSockets parent;
	final int handle;
	final List<SteamConnection> connecting = new LinkedList<SteamConnection>();

	SteamListenSocket(SteamNetworkingSockets parent, int handle) {
		this.parent = parent;
		this.handle = handle;
	}

	public Inet4Address getIp() {
		IntByReference ip = new IntByReference();
		GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_GetListenSocketInfo(parent.instance, handle, ip, null);
		return SteamNetworkingSockets.intToIp(ip.getValue());
	}
	public short getPort() {
		ShortByReference port = new ShortByReference();
		GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_GetListenSocketInfo(parent.instance, handle, null, port);
		return port.getValue();
	}
	public void close(String disconnectDebug) {
		if(!GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_CloseListenSocket(parent.instance, handle, Native.toByteArray(disconnectDebug))) {
			throw new Error("Could not close listen socket");
		}
	}
	public synchronized SteamConnection accept(boolean blocking) {
		if(blocking) while(connecting.isEmpty());
		if(!connecting.isEmpty()) {
			SteamConnection conn = connecting.remove(0);
			GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_AcceptConnection(parent.instance, conn.handle);
			return conn;
		} else return null;
	}
}
