package jugendforscht23.client;

import com.valvesoftware.gamenetworkingsockets.ConnectionState;
import com.valvesoftware.gamenetworkingsockets.SteamConnection;
import com.valvesoftware.gamenetworkingsockets.SteamNetworkingSockets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import jugendforscht23.Global;

public class SteamP2PClient {
	public static void main(String[] args) throws InterruptedException {
		SteamConnection client = SteamNetworkingSockets.INSTANCE.connect(Global.ADDR, Global.PORT);
		while(client.getState() == ConnectionState.Connecting) Thread.sleep(1);
		assert client.getState() == ConnectionState.Connected;
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
		Benchmark.init(ch);
		ch.pipeline().fireChannelActive();
		while(client.getState() != ConnectionState.ClosedByPeer) {
			byte[] packet = client.receive();
			if(packet != null) ch.writeOneInbound(Unpooled.wrappedBuffer(packet));
		}
	}
}
