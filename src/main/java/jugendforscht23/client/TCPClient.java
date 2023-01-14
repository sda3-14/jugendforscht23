package jugendforscht23.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import jugendforscht23.Global;

public class TCPClient {
	public static void main(String[] args) {
		EventLoopGroup worker = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap()
			.group(worker)
			.channel(NioSocketChannel.class)
			.handler(new ChannelInitializer<NioSocketChannel>() {
				protected void initChannel(NioSocketChannel ch) throws Exception {
					Benchmark.init(ch);
				}
			});
		NioSocketChannel ch = (NioSocketChannel) bootstrap.connect(Global.ADDR, Global.PORT).syncUninterruptibly().channel();
		ch.closeFuture().syncUninterruptibly();
		worker.shutdownGracefully();
	}
}
