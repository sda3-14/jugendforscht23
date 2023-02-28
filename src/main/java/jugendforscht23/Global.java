package jugendforscht23;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Global {
	public static final Inet4Address ADDR1;
	static {
		Inet4Address a = null;
		try {
			a = (Inet4Address) InetAddress.getByName("172.16.99.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		ADDR1 = a;
	}
	public static final Inet4Address ADDR2;
	static {
		Inet4Address a = null;
		try {
			a = (Inet4Address) InetAddress.getByName("172.16.99.2");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		ADDR2 = a;
	}
	public static final short PORT = 9999;
}
