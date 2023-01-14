package com.valvesoftware.gamenetworkingsockets.ll;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public interface GameNetworkingSockets extends Library {
	public static GameNetworkingSockets INSTANCE = Native.load("GameNetworkingSockets", GameNetworkingSockets.class);

	public boolean GameNetworkingSockets_Init(SteamNetworkingIdentity identity, byte[] error);
	public void GameNetworkingSockets_Kill();
	public Pointer SteamNetworkingSockets();
	public Pointer SteamNetworkingSocketsGameServer();
	public void SteamNetworkingSockets_SetDebugOutputFunction(Pointer instance, Callback callback);

	public int SteamAPI_ISteamNetworkingSockets_CreateListenSocket(Pointer instance, int nSteamConnectVirtualPort, int nIP, short nPort);
	public int SteamAPI_ISteamNetworkingSockets_ConnectByIPv4Address(Pointer instance, int nIP, short nPort);

	public int SteamAPI_ISteamNetworkingSockets_AcceptConnection(Pointer instance, int hConn);

	public boolean SteamAPI_ISteamNetworkingSockets_CloseConnection(Pointer instance, int hPeer, int nReason, byte[] pszDebug, boolean bEnableLinger);
	public boolean SteamAPI_ISteamNetworkingSockets_CloseListenSocket(Pointer instance, int hSocket, byte[] pszNotifyRemoteReason);

	public boolean SteamAPI_ISteamNetworkingSockets_GetConnectionInfo(Pointer instance, int hConn, SteamNetConnectionInfo pInfo);
	public boolean SteamAPI_ISteamNetworkingSockets_GetListenSocketInfo(Pointer instance, int hSocket, IntByReference pnIP, ShortByReference pnPort);

	public boolean SteamAPI_ISteamNetworkingSockets_SetConnectionUserData(Pointer instance, int hPeer, long nUserData);
	public long SteamAPI_ISteamNetworkingSockets_GetConnectionUserData(Pointer instance, int hPeer);

	public void SteamAPI_ISteamNetworkingSockets_SetConnectionName(Pointer instance, int hPeer, byte[] pszName);
	public void SteamAPI_ISteamNetworkingSockets_GetConnectionName(Pointer instance, int hPeer, byte[] ppszName, int nMaxLen);

	public int SteamAPI_ISteamNetworkingSockets_SendMessageToConnection(Pointer instance, int hConn, byte[] pData, int cbData, int eSendType);
	public int SteamAPI_ISteamNetworkingSockets_FlushMessagesOnConnection(Pointer instance, int hConn);
	public int SteamAPI_ISteamNetworkingSockets_ReceiveMessagesOnConnection(Pointer instance, int hConn, PointerByReference ptrPtr, int nMaxMessages);
	// public int SteamAPI_ISteamNetworkingSockets_ReceiveMessagesOnListenSocket(Pointer instance, int hSocket, Pointer ppOutMessages, int nMaxMessages);

	public void SteamAPI_ISteamNetworkingSockets_RunConnectionStatusChangedCallbacks(Pointer instance, Callback pCallbacks, IntByReference context);
}
