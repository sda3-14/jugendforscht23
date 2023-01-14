package com.valvesoftware.gamenetworkingsockets.ll;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({ "hListenSocket", "steamIdRemote", "userData", "ipRemote", "portRemote", "_pad1", "idPOPRemote", "idPOPRelay", "eState", "eEndReason", "szEndDebug" })
public class SteamNetConnectionInfo extends Structure {
	public int hListenSocket;
	public long steamIdRemote;
	public long userData;
	public int ipRemote;
	public short portRemote;
	public short _pad1;
	public int idPOPRemote;
	public int idPOPRelay;
	public int eState;
	public int eEndReason;
	public byte[] szEndDebug = new byte[128];
}
