package de.yeetus.ue4.net;

import de.yeetus.ue4.serialization.Archive;

public class Ack extends Frame {
	public static final int TYPE = 1;

	public int packetId;

	public Ack() {}
	public Ack(int packetId) {
		this.packetId = packetId;
	}

	public String toString() {
		return "Ack{"+packetId+"}";
	}

	public void write(Archive ar, boolean isServer) {
		ar.writeBit(TYPE);
		ar.writePIntWrapped(NetDriver.MAX_PACKETID, packetId);
		if(!isServer && packetId % NetDriver.PING_ACK_PACKET_INTERVAL == 0) {
			ar.writeBoolean(false); // No PingAcks for you!
		}
	}
	public void read(Archive ar, boolean isServer) {
		packetId = ar.readPInt(NetDriver.MAX_PACKETID);
		if(isServer && packetId % NetDriver.PING_ACK_PACKET_INTERVAL == 0) {
			if(ar.readBoolean()) {
				ar.readInt();
			}
		}
	}
}
