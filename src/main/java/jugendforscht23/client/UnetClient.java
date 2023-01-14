package jugendforscht23.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import de.yeetus.ue4.net.IpNetDriver;
import de.yeetus.ue4.net.NetChType;
import de.yeetus.ue4.net.NetChannel;
import de.yeetus.ue4.net.NetConnection;
import de.yeetus.ue4.net.NetHandler;
import de.yeetus.ue4.net.NetMsg;
import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import jugendforscht23.Global;

public class UnetClient {
	public static void main(String[] args) throws IOException {
		IpNetDriver driver = new IpNetDriver(false, conn -> {
			return new NetHandler(conn) {
				protected boolean acceptNewChannel(NetChannel ch) {
					return false;
				}
			};
		});
		NetConnection conn = driver.connect(new InetSocketAddress(Global.ADDR, Global.PORT));
		NetChannel ch = new NetChannel(conn, NetChType.CONTROL);
		ch.pipeline().addLast(new MessageToMessageCodec<NetMsg, ByteBuf>() {
			protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
				NetMsg outMsg = new NetMsg(conn);
				outMsg.writeBits(new Archive(msg, conn.endianness));
				out.add(outMsg);
			}

			protected void decode(ChannelHandlerContext ctx, NetMsg msg, List<Object> out) throws Exception {
				out.add(msg.buf.retain());
			}
		});
		Benchmark.init(ch.pipeline());
		ch.pipeline().fireChannelActive();
		while (!conn.isDead())
			driver.tick();
	}
}
