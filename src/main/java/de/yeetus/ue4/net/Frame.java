package de.yeetus.ue4.net;

import java.nio.ByteOrder;

import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBufAllocator;

public abstract class Frame {
	public abstract String toString();
	
	public abstract void write(Archive ar, boolean isServer);
	public abstract void read(Archive ar, boolean isServer);

	public int calcBitCount() {
		Archive archive = new Archive(ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_MESSAGE), ByteOrder.nativeOrder());
		write(archive, false);
		int bitCount = archive.readableBits();
		archive.release();
		return bitCount;
	}
}
