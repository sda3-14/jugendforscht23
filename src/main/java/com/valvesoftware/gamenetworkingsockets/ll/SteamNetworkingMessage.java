package com.valvesoftware.gamenetworkingsockets.ll;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({ "steamIdSender", "connUserData", "usecTimeReceived", "messageNumber", "releaseMethod", "data", "size", "hConn", "channel", "_pad1" })
public class SteamNetworkingMessage extends Structure {
	public long steamIdSender;
	public long connUserData;
	public long usecTimeReceived;
	public long messageNumber;
	public Pointer releaseMethod;
	public Pointer data;
	public int size;
	public int hConn;
	public int channel;
	public int _pad1;
}
