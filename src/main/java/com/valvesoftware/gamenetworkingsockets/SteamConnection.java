package com.valvesoftware.gamenetworkingsockets;

import java.net.Inet4Address;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import com.valvesoftware.gamenetworkingsockets.ll.EResult;
import com.valvesoftware.gamenetworkingsockets.ll.GameNetworkingSockets;
import com.valvesoftware.gamenetworkingsockets.ll.SteamNetConnectionInfo;
import com.valvesoftware.gamenetworkingsockets.ll.SteamNetworkingMessage;

public class SteamConnection {
	private final SteamNetworkingSockets parent;
	final int handle;

	SteamConnection(SteamNetworkingSockets parent, int handle) {
		this.parent = parent;
		this.handle = handle;
	}

	private SteamNetConnectionInfo getInfo() {
		SteamNetConnectionInfo info = new SteamNetConnectionInfo();
		GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_GetConnectionInfo(parent.instance, handle, info);
		return info;
	}

	public ConnectionState getState() {
		return ConnectionState.find(getInfo().eState);
	}
	public SteamListenSocket getListenSocket() {
		return parent.listenSockets.get(getInfo().hListenSocket);
	}
	public Inet4Address getRemoteIp() {
		return SteamNetworkingSockets.intToIp(getInfo().ipRemote);
	}
	public short getRemotePort() {
		return getInfo().portRemote;
	}
	public int getDisconnectReason() {
		return getInfo().eEndReason;
	}
	public String getDisconnectDebug() {
		return Native.toString(getInfo().szEndDebug);
	}

	public String toString() {
		return "SteamConnection{" + getState() + (getListenSocket()!=null?","+getListenSocket():"") + (getRemoteIp()!=null?","+getRemoteIp()+":"+getRemotePort():"") + "}";
	}

	public void send(byte[] data, boolean reliable) {
		EResult result = EResult.find(GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_SendMessageToConnection(parent.instance, handle, data, data.length, reliable?8:0));
		if(result != EResult.OK) throw new Error("Could not send message: " + result);
	}
	public void flush() {
		EResult result = EResult.find(GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_FlushMessagesOnConnection(parent.instance, handle));
		if(result != EResult.OK) throw new Error("Could not flush: " + result);
	}
	public byte[] receive() {
		PointerByReference ptrPtr = new PointerByReference();
		int count = GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_ReceiveMessagesOnConnection(parent.instance, handle, ptrPtr, 1);
		assert count >= 0 && count <= 1;
		if(count >= 1) {
			SteamNetworkingMessage msg = Structure.newInstance(SteamNetworkingMessage.class, ptrPtr.getValue());
			msg.read();
			byte[] bytes = msg.data.getByteArray(0, msg.size);
			Function.getFunction(msg.releaseMethod).invoke(new Object[] { msg });
			return bytes;
		} else return null;
	} 

	public void close(int disconnectReason, String disconnectDebug) {
		if(!GameNetworkingSockets.INSTANCE.SteamAPI_ISteamNetworkingSockets_CloseConnection(parent.instance, handle, disconnectReason, Native.toByteArray(disconnectDebug), false)) {
			throw new Error("Could not close connection");
		}
	}
}
