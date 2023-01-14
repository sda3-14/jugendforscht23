package de.yeetus.ue4.net;

import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class NetMsg extends Archive {
	public boolean reliable = true;

	public NetMsg(NetConnection connection, boolean reliable) {
		this(connection);
		this.reliable = reliable;
	}
	public NetMsg(NetConnection connection) {
		this(ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_MESSAGE), connection.endianness);
	}
	private NetMsg(ByteBuf buf, ByteOrder endianness) {
		super(buf, endianness);
	}
	
	public String toString() {
		return "Msg{"
		+(reliable?"R,":"")
		+hexDump()
		+"}";
	}

	void addBunch(Bunch bunch) {
		assert reliable == bunch.reliable;
		if(bunch.content != null) {
			if(bunch.content instanceof Bunch.Content.Bytes) {
				((Bunch.Content.Bytes) bunch.content).write(this);
			} else throw new UnsupportedOperationException(bunch.content.getClass().getName());
		}
	}
}
