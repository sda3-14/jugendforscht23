package de.yeetus.ue4.serialization;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.google.common.reflect.TypeToken;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public class Archive implements ReferenceCounted {
	public final ByteBuf buf;
	private int readerIndexBits = 0;
	private int writerIndexBits = 0;
	public ByteOrder endianness;

	public Archive(ByteBuf buf, ByteOrder endianness) {
		this.buf = buf;
		this.endianness = endianness;
	}

	// ReferenceCount Stuff
	public int refCnt() { return buf.refCnt(); }
	public Archive retain() { buf.retain(); return this; }
	public Archive retain(int increment) { buf.retain(increment); return this; }
	public Archive touch() { buf.touch(); return this; }
	public Archive touch(Object hint) { buf.touch(hint); return this; }
	public boolean release() { return buf.release(); }
	public boolean release(int decrement) { return buf.release(decrement); }

	// Misc
	public String hexDump() {
		Archive ar2 = new Archive(buf.alloc().buffer(writerIndex()-readerIndex()/8+2), endianness);

		int currReaderIndex = readerIndex();
		ar2.writeBits(this);
		readerIndex(currReaderIndex);

		int len = ar2.writerIndexBits != 0 ? ar2.writerIndex() : 0;
		ar2.byteAlign();

		String hexDump = ByteBufUtil.hexDump(ar2.buf);
		if(len != 0) hexDump += "("+len+")";

		ar2.release();

		return hexDump;
	}

	// Pointer info & modification
	public int readerIndex() { return buf.readerIndex() * 8 + readerIndexBits; }
	public Archive readerIndex(int readerIndex) { if(readerIndex > writerIndex()) throw new IndexOutOfBoundsException("readerIndex(" + readerIndex + ") exceeds writerIndex(" + writerIndex() + ")"); buf.readerIndex(readerIndex / 8); readerIndexBits = readerIndex % 8; return this; }
	public int writerIndex() { return buf.writerIndex() * 8 + writerIndexBits; }
	public Archive writerIndex(int writerIndex) { if(readerIndex() > writerIndex) throw new IndexOutOfBoundsException("readerIndex(" + readerIndex() + ") exceeds writerIndex(" + writerIndex + ")"); buf.writerIndex(writerIndex / 8); writerIndexBits = writerIndex % 8; return this; }
	public long maxCapacity() { return (long) buf.maxCapacity() * 8; }
	public int readableBits() { return writerIndex() - readerIndex(); }

	public void byteAlign() { while(writerIndex() % 8 != 0) writeBoolean(false); }

	// Internal Stuff
	public boolean isReadable(int bits) { return readerIndex() + bits <= writerIndex(); }
	private void checkReadable(int bits) { if(readerIndex() + bits > writerIndex()) throw new IndexOutOfBoundsException("readerIndex(" + readerIndex() + ") + length(" + bits + ") exceeds writerIndex(" + writerIndex() + ")"); }
	public boolean isWritable(int bits) { return writerIndex() + bits <= maxCapacity(); }
	private void checkWritable(int bits) { if(writerIndex() + bits > maxCapacity()) throw new IndexOutOfBoundsException("writerIndex(" + writerIndex() + ") + length(" + bits + ") exceeds capacity(" + maxCapacity() + ")"); }

	// Content
	public boolean readBoolean() {
		checkReadable(1);

		byte fullByte = buf.getByte(buf.readerIndex());
		int mask = (int) Math.pow(2, readerIndexBits) & 0xff;
		boolean value = (fullByte & mask) != 0;

		readerIndexBits++;
		while(readerIndexBits >= 8) {
			readerIndexBits -= 8;
			buf.readerIndex(buf.readerIndex() + 1);
		}

		return value;
	}
	public Archive writeBoolean(boolean value) {
		checkWritable(1);

		byte fullByte = buf.getByte(buf.writerIndex());
		int mask = (int) Math.pow(2, writerIndexBits) & 0xff;
		fullByte &= ~mask;
		fullByte |= value?mask:0;
		buf.setByte(buf.writerIndex(), fullByte);
		
		writerIndexBits++;
		while(writerIndexBits >= 8) {
			writerIndexBits -= 8;
			buf.writerIndex(buf.writerIndex() + 1);
		}

		return this;
	}
	public int readBit() { checkReadable(1); return readBoolean()?1:0; }
	public Archive writeBit(int value) { assert value>=0&&value<=1 : "value for type \"bit\" must be between 0 and 1"; checkWritable(1); return writeBoolean(value!=0); }
	public byte readByte() {
		checkReadable(8);
		byte value = 0;
		for(int i = 0; i < 8; i++) if(readBoolean()) value |= (byte) Math.pow(2,i);
		return value;
	}
	public Archive writeByte(byte value) {
		checkWritable(8);
		for(int i = 0; i < 8; i++) writeBoolean(((byte) Math.pow(2,i) & value) != 0);
		return this;
	}
	public Archive readBytes(byte[] dst) {
		checkReadable(dst.length * 8);
		for(int i = 0; i < dst.length; i++) dst[i] = readByte();
		return this;
	}
	public Archive writeBytes(byte[] src) {
		checkWritable(src.length * 8);
		for(int i = 0; i < src.length; i++) writeByte(src[i]);
		return this;
	}

	public short readShort() {
		byte[] b = new byte[Short.BYTES];
		readBytes(b);
		return ByteBuffer.wrap(b).order(endianness).getShort();
	}
	public Archive writeShort(short value) {
		byte[] b = new byte[Short.BYTES];
		ByteBuffer.wrap(b).order(endianness).putShort(value);
		writeBytes(b);
		return this;
	}
	public int readInt() {
		byte[] b = new byte[Integer.BYTES];
		readBytes(b);
		return ByteBuffer.wrap(b).order(endianness).getInt();
	}
	public Archive writeInt(int value) {
		byte[] b = new byte[Integer.BYTES];
		ByteBuffer.wrap(b).order(endianness).putInt(value);
		writeBytes(b);
		return this;
	}
	public long readLong() {
		byte[] b = new byte[Long.BYTES];
		readBytes(b);
		return ByteBuffer.wrap(b).order(endianness).getLong();
	}
	public Archive writeLong(long value) {
		byte[] b = new byte[Long.BYTES];
		ByteBuffer.wrap(b).order(endianness).putLong(value);
		writeBytes(b);
		return this;
	}
	public float readFloat() {
		byte[] b = new byte[Float.BYTES];
		readBytes(b);
		return ByteBuffer.wrap(b).order(endianness).getFloat();
	}
	public Archive writeFloat(float value) {
		byte[] b = new byte[Float.BYTES];
		ByteBuffer.wrap(b).order(endianness).putFloat(value);
		writeBytes(b);
		return this;
	}
	public double readDouble() {
		byte[] b = new byte[Double.BYTES];
		readBytes(b);
		return ByteBuffer.wrap(b).order(endianness).getDouble();
	}
	public Archive writeDouble(double value) {
		byte[] b = new byte[Double.BYTES];
		ByteBuffer.wrap(b).order(endianness).putDouble(value);
		writeBytes(b);
		return this;
	}

	public int readPInt(int max) {
		if(max > 1) {
			int bit = 1;
			int value = 0;
			do {
				if(bit == 0) break;
				if(readBoolean()) value |= bit;
				bit *= 2;
			} while(bit + value < max);
			return value;
		} else return 0;
	}
	public Archive writePInt(int max, int value) {
		if(value > max) throw new IllegalArgumentException("value(" + value + ") higher than maximum(" + max + ")");
		return writePIntWrapped(max, value);
	}
	public Archive writePIntWrapped(int max, int value) {
		int iVar7 = 1;
		int iVar4 = 0;
		if(max > 1) {
			do {
				if(iVar7 == 0) return this;
				writeBoolean((value & iVar7) != 0);
				if((value & iVar7) != 0) {
					iVar4 += iVar7;
				}
				iVar7 = iVar7 * 2;
			} while(iVar7 + iVar4 < max);
		}
		return this;
	}
	public int readTInt() {
		int result = 0;
		byte cur;
		int count = 0;
		do {
			cur = readByte();
			if(count > 0 && cur == 0) throw new AssertionError("invalid TInt");
			result |= (cur & 0xfe) >> 1 << count * 7;
			count++;
		} while((cur & 0b1) != 0);
		return result;
	}
	public Archive writeTInt(int value) {
		if(value < 0) throw new IllegalArgumentException("value must be positive");
		while(true) {
			byte cur = (byte) (value & 0x7f);
			value = value >> 7;
			if(value == 0) {
				writeByte((byte) (cur << 1));
				return this;
			} else {
				writeByte((byte) (cur << 1 | 0b1));
			}
		}
	}

	// TODO clean up string serialization
	public String readString() {
		int len = readInt();
		if(len==0) return null;
		boolean wide = len < 0;
		len = Math.abs(len);
		byte[] raw = new byte[len*(wide?2:1)];
		readBytes(raw);
		if(!wide&&raw[raw.length-1]!=0x00) throw new IllegalArgumentException("invalid array");
		if(wide&&raw[raw.length-2]!=0x00) throw new IllegalArgumentException("invalid array");
		byte[] s = new byte[raw.length-(wide?2:1)];
		System.arraycopy(raw, 0, s, 0, s.length);
		return new String(s, wide?(endianness==ByteOrder.LITTLE_ENDIAN?StandardCharsets.UTF_16LE:StandardCharsets.UTF_16BE):StandardCharsets.UTF_8);
	}
	public Archive writeString(String value) {
		if(value==null) {
			writeInt(0);
			return this;
		}
		writeInt(value.length()+1);
		writeBytes(value.getBytes());
		writeByte((byte)0x00);
		return this;
	}

	public UUID readUUID() {
		return new UUID((long) readInt() << 32 | (readInt() & 0xffffffffl), (long) readInt() << 32 | (readInt() & 0xffffffffl));
	}
	public Archive writeUUID(UUID value) {
		writeInt((int) (value.getMostSignificantBits() >> 32));
		writeInt((int) value.getMostSignificantBits());
		writeInt((int) (value.getLeastSignificantBits() >> 32));
		writeInt((int) value.getLeastSignificantBits());
		return this;
	}

	public Object readEnum(TypeToken<?> type) {
		Object[] values = type.getRawType().getEnumConstants();
		int max = values.length;
		Object value = values[readPInt(max)];
		return value;
	}
	public Archive writeEnum(TypeToken<?> type, Object value) {
		Object[] values = type.getRawType().getEnumConstants();
		int max = values.length;
		int index = Arrays.asList(values).indexOf(value);
		writePInt(max, index);
		return this;
	}

	public Archive readBits(Archive dst) {
		while(dst.isWritable(1) && isReadable(1)) dst.writeBit(readBit());
		return this;
	}
	public Archive readBits(Archive dst, int bits) {
		checkReadable(bits);
		dst.checkWritable(bits);
		for(int i = 0; i < bits; i++) dst.writeBit(readBit());
		return this;
	}
	public Archive writeBits(Archive src) { src.readBits(this); return this; }
	public Archive writeBits(Archive src, int bits) { src.readBits(this, bits); return this; }

	@SuppressWarnings("unchecked")
	public <T> T read(TypeToken<T> type) {
		type = type.unwrap();
		if(boolean.class.isAssignableFrom(type.getRawType())) return (T) (Boolean) readBoolean();
		else if(byte.class.isAssignableFrom(type.getRawType())) return (T) (Byte) readByte();
		else if(short.class.isAssignableFrom(type.getRawType())) return (T) (Short) readShort();
		else if(int.class.isAssignableFrom(type.getRawType())) return (T) (Integer) readInt();
		else if(long.class.isAssignableFrom(type.getRawType())) return (T) (Long) readLong();
		else if(float.class.isAssignableFrom(type.getRawType())) return (T) (Float) readFloat();
		else if(double.class.isAssignableFrom(type.getRawType())) return (T) (Double) readDouble();
		else if(String.class.isAssignableFrom(type.getRawType())) return (T) readString();
		else if(UUID.class.isAssignableFrom(type.getRawType())) return (T) readUUID();
		else if(SimpleSerializable.class.isAssignableFrom(type.getRawType())) return (T) SimpleSerializable.read(this, (Class<? extends SimpleSerializable>) (Class<?>) type.getRawType());
		else if(Enum.class.isAssignableFrom(type.getRawType())) return (T) readEnum(type);
		else throw new AssertionError("Can't serialize object of type " + type);
	}
	public <T> Archive write(T value, TypeToken<T> type) {
		type = type.unwrap();
		if(boolean.class.isAssignableFrom(type.getRawType())) writeBoolean((Boolean) value);
		else if(byte.class.isAssignableFrom(type.getRawType())) writeByte((Byte) value);
		else if(short.class.isAssignableFrom(type.getRawType())) writeShort((Short) value);
		else if(int.class.isAssignableFrom(type.getRawType())) writeInt((Integer) value);
		else if(long.class.isAssignableFrom(type.getRawType())) writeLong((Long) value);
		else if(float.class.isAssignableFrom(type.getRawType())) writeFloat((Float) value);
		else if(double.class.isAssignableFrom(type.getRawType())) writeDouble((Double) value);
		else if(String.class.isAssignableFrom(type.getRawType())) writeString((String) value);
		else if(UUID.class.isAssignableFrom(type.getRawType())) writeUUID((UUID) value);
		else if(SimpleSerializable.class.isAssignableFrom(type.getRawType())) ((SimpleSerializable) value).write(this);
		else if(Enum.class.isAssignableFrom(type.getRawType())) return writeEnum(type, value);
		else throw new AssertionError("Can't serialize object of type " + type);
		return this;
	}
}
