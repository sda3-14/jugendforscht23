package de.yeetus.ue4.net;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;

import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ReferenceCountUpdater;

public class Bunch extends Frame implements ReferenceCounted {
	public static final int TYPE = 0;

	private static final long REFCNT_FIELD_OFFSET = ReferenceCountUpdater.getUnsafeOffset(Bunch.class, "refCnt");
	private static final AtomicIntegerFieldUpdater<Bunch> AIF_UPDATER = AtomicIntegerFieldUpdater.newUpdater(Bunch.class, "refCnt");
	private static final ReferenceCountUpdater<Bunch> updater = new ReferenceCountUpdater<Bunch>() {
		protected long unsafeOffset() { return REFCNT_FIELD_OFFSET; }
		protected AtomicIntegerFieldUpdater<Bunch> updater() { return AIF_UPDATER; }
	};
	@SuppressWarnings("unused")
	private volatile int refCnt = updater.initialValue();

	public int channel;
	public boolean reliable;
	public boolean partial;
	public boolean partialFirst;
	public boolean partialLast;
	public int seqNum;
	public boolean open;
	public boolean close;
	public boolean dormant;
	public NetChType channelType;
	public Bunch.Content content;

	public String toString() {
		return "Bunch{"+channel+(channelType!=null?"("+channelType+")":"")+","+(reliable?"R":"X")+(partial?(partialFirst^partialLast?(partialFirst?"F":"L"):"P"):"X")+(reliable||partial?","+seqNum:"")+(open||close?","+(open?"O":"X")+(close?(dormant?"D":"C"):"X"):"")+","+content+"}";
	}

	public void write(Archive ar, boolean isServer) {
		ar.writeBit(TYPE);
		ar.writeBoolean(open || close);
		if(open || close) {
			ar.writeBoolean(open);
			ar.writeBoolean(close);
			if(close) ar.writeBoolean(dormant);
		}
		ar.writeBoolean(reliable);
		ar.writePInt(NetDriver.MAX_CHANNELS, channel);
		ar.writeBoolean(content != null ? content.hasGUIDs() : false);
		ar.writeBoolean(content != null ? content.hasMustBeMappedGUIDs() : false);
		ar.writeBoolean(partial);
		if(reliable || partial) {
			ar.writePIntWrapped(NetDriver.MAX_CHSEQUENCE, seqNum);
			if(partial) {
				ar.writeBoolean(partialFirst);
				ar.writeBoolean(partialLast);
			}
		}
		if(reliable || open) ar.writePInt(NetChType.MAX, channelType.id);

		ar.writePInt(NetDriver.MAX_PACKET << 3, content != null ? content.calcBitCount() : 0);
		if(content != null) content.write(ar);
	}
	public void read(Archive ar, boolean isServer) {
		boolean hasControl = ar.readBoolean();
		open = hasControl ? ar.readBoolean() : false;
		close = hasControl ? ar.readBoolean() : false;
		dormant = close ? ar.readBoolean() : false;
		reliable = ar.readBoolean();
		channel = ar.readPInt(NetDriver.MAX_CHANNELS);
		boolean hasGUIDs = ar.readBoolean();
		boolean hasMustBeMappedGUIDs = ar.readBoolean();
		partial = ar.readBoolean();
		seqNum = reliable || partial ? ar.readPInt(NetDriver.MAX_CHSEQUENCE) : 0;
		partialFirst = partial ? ar.readBoolean() : false;
		partialLast = partial ? ar.readBoolean() : false;
		if(reliable || open) channelType = NetChType.getById(ar.readPInt(NetChType.MAX));
		int bitCount = ar.readPInt(NetDriver.MAX_PACKET << 3);
		
		if(bitCount > 0) {
			Archive contentAr = new Archive(ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_PACKET), ar.endianness);
			ar.readBits(contentAr, bitCount);
			assert !(hasGUIDs && hasMustBeMappedGUIDs) : "Invalid bunch content type";
			if(hasGUIDs) {
				// TODO read GUIDs
				contentAr.release();
			} else if(hasMustBeMappedGUIDs) {
				// TODO read must be mapped GUIDs
				contentAr.release();
			} else {
				content = new Bunch.Content.Bytes(contentAr);
			}
		} else content = null;
	}

	public int refCnt() { return updater.refCnt(this); }
	public Bunch retain() { updater.retain(this); return this; }
	public Bunch retain(int increment) { updater.retain(this, increment); return this; }
	public Bunch touch() { return this; }
	public Bunch touch(Object hint) { return this; }
	public boolean release() { boolean r = updater.release(this); if(r) ReferenceCountUtil.release(content); return r; }
	public boolean release(int decrement) { boolean r = updater.release(this, decrement); if(r) ReferenceCountUtil.release(content); return r; }

	public static abstract class Content {
		public abstract void write(Archive dst);
		public abstract String toString();
		
		public int calcBitCount() {
			Archive archive = new Archive(ByteBufAllocator.DEFAULT.buffer(NetDriver.MAX_MESSAGE), ByteOrder.nativeOrder());
			write(archive);
			int bitCount = archive.readableBits();
			archive.release();
			return bitCount;
		}

		public abstract boolean hasGUIDs();
		public abstract boolean hasMustBeMappedGUIDs();

		public static class Bytes extends Bunch.Content implements ReferenceCounted {
			public final Archive content;

			public Bytes(Archive content) {
				this.content = content;
			}

			public void write(Archive dst) {
				int readerIndex = content.readerIndex();
				dst.writeBits(content);
				content.readerIndex(readerIndex);
			}

			public String toString() { return content.hexDump(); }

			public int calcBitCount() { return content.readableBits(); }

			public boolean hasGUIDs() { return false; }
			public boolean hasMustBeMappedGUIDs() { return false; }

			public int refCnt() { return content.refCnt(); }
			public Bunch.Content.Bytes retain() { content.retain(); return this; }
			public Bunch.Content.Bytes retain(int increment) { content.retain(increment); return this; }
			public Bunch.Content.Bytes touch() { content.touch(); return this; }
			public Bunch.Content.Bytes touch(Object hint) { content.touch(hint); return this; }
			public boolean release() { return content.release(); }
			public boolean release(int decrement) { return content.release(decrement); }
		}
	}
}
