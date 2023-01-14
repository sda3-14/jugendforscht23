package com.valvesoftware.gamenetworkingsockets.ll;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({ "eType" })
public class SteamNetworkingIdentity extends Structure {
	public int eType;
}
