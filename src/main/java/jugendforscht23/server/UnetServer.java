package jugendforscht23.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import de.yeetus.ue4.net.IpNetDriver;
import de.yeetus.ue4.net.NetChannel;
import de.yeetus.ue4.net.NetHandler;
import de.yeetus.ue4.net.NetMsg;
import de.yeetus.ue4.serialization.Archive;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import jugendforscht23.Global;

public class UnetServer {
	public static void main(String[] args) throws IOException, InterruptedException {
		IpNetDriver driver = new IpNetDriver(true, conn -> {
			return new NetHandler(conn) {
				protected boolean acceptNewChannel(NetChannel ch) {
					ch.pipeline().addLast(new MessageToMessageCodec<NetMsg,ByteBuf>() {
						protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
							NetMsg outMsg = new NetMsg(connection);
							outMsg.writeBits(new Archive(msg, connection.endianness));
							out.add(outMsg);
						}
						protected void decode(ChannelHandlerContext ctx, NetMsg msg, List<Object> out) throws Exception {
							out.add(msg.buf.retain());
						}
					});
					PingHandler.init(ch.pipeline());
					return true;
				}
			};
		});
		driver.bind(new InetSocketAddress(Global.ADDR, Global.PORT));
		while(true) {
			driver.tick();
			Thread.sleep(1);
		}
	}
}
