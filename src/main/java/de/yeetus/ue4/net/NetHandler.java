package de.yeetus.ue4.net;

public abstract class NetHandler {
	public final NetConnection connection;

	public NetHandler(NetConnection connection) {
		this.connection = connection;
		assert connection.handler == null : "Tried to create handler but already have handler for connection";
		connection.handler = this;
	}

	protected abstract boolean acceptNewChannel(NetChannel channel);
	
	protected void tick() {}
	protected void cleanUp() {}
}
