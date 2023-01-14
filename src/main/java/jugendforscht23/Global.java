package jugendforscht23;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Global {
	public static final Inet4Address ADDR;
	static {
		Inet4Address a = null;
		try {
			a = (Inet4Address) InetAddress.getByName("192.168.178.178");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		ADDR = a;
	}
	public static final short PORT = 9999;
}
