package de.yeetus.ue4.net;

public enum NetChType {
	CONTROL(1),
	ACTOR(2),
	FILE(3),
	VOICE(4),
	BATTLEYE(6);

	public static final int MAX = 8;
	
	public final int id;

	private NetChType(int id) {
		this.id = id;
	}

	public static NetChType getById(int id) {
		for(NetChType type : NetChType.values()) {
			if(type.id == id) return type;
		}
		return null;
	}
}
