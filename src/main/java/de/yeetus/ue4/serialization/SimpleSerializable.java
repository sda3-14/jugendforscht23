package de.yeetus.ue4.serialization;

import java.lang.reflect.Constructor;

public interface SimpleSerializable {
	public SimpleSerializable read(Archive ar);
	public SimpleSerializable write(Archive ar);

	public static <T extends SimpleSerializable> T read(Archive ar, Class<T> type) {
		T value;
		try {
			Constructor<T> constructor = type.getConstructor();
			value = constructor.newInstance();
		} catch (Throwable e) {
			throw new AssertionError("Could not find zero-parameter constructur for " + type, e);
		}
		value.read(ar);
		return value;
	}
}
