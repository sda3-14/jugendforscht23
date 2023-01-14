package com.valvesoftware.gamenetworkingsockets.ll;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({ "hConn", "info" })
public class SteamNetConnectionStatusChangedCallbackInfo extends Structure {
	public int hConn;
	public SteamNetConnectionInfo info;
}
