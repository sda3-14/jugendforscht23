package jugendforscht23.server;

import java.net.UnknownHostException;

import com.valvesoftware.gamenetworkingsockets.ConnectionState;
import com.valvesoftware.gamenetworkingsockets.SteamConnection;
import com.valvesoftware.gamenetworkingsockets.SteamListenSocket;
import com.valvesoftware.gamenetworkingsockets.SteamNetworkingSockets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import jugendforscht23.Global;

public class SteamP2PServer {
	public static void main(String[] args) throws UnknownHostException {
		SteamListenSocket server = SteamNetworkingSockets.SERVER_INSTANCE.listen(Global.ADDR, Global.PORT);
		while(true) {
			SteamConnection client = server.accept(false); // We only need to accept one client at a time anyways
			if(client != null) {
				System.out.println(client);
				EmbeddedChannel ch = new EmbeddedChannel();
				ch.pipeline().addLast(
					new ChannelOutboundHandlerAdapter() {
						public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
							assert msg instanceof ByteBuf;
							ByteBuf buf = (ByteBuf) msg;
							byte[] x = new byte[buf.readableBytes()];
							buf.readBytes(x);
							client.send(x, true);
							client.flush();
							buf.release();
						}
					}
				);
				PingHandler.init(ch);
				while(client.getState() != ConnectionState.ClosedByPeer) {
					byte[] packet = client.receive();
					if(packet != null) ch.writeOneInbound(Unpooled.wrappedBuffer(packet));
				}
			}
		}
	}
}
