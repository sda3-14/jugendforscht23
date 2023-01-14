package com.valvesoftware.gamenetworkingsockets;

public enum ConnectionState {
	None(0),
	Connecting(1),
	FindingRoute(2),
	Connected(3),
	ClosedByPeer(4),
	ProblemDetectedLocally(5),
	FinWait(-1),
	Linger(-2),
	Dead(-3);

	public final int num;

	private ConnectionState(int num) {
		this.num = num;
	}

	public static ConnectionState find(int num) {
		for(ConnectionState state : values()) {
			if(state.num == num) return state;
		}
		return null;
	}
}
