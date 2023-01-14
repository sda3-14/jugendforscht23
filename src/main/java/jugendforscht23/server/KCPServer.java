package jugendforscht23.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jugendforscht23.Global;
import io.jpower.kcp.netty.UkcpChannel;
import io.jpower.kcp.netty.UkcpServerChannel;
import io.netty.bootstrap.UkcpServerBootstrap;
import io.netty.channel.Channel;


public class KCPServer {
	public static void main(String[] args) {
		EventLoopGroup worker = new NioEventLoopGroup();
		UkcpServerBootstrap b = new UkcpServerBootstrap();
		b.group(worker)
			.channel(UkcpServerChannel.class)
			.childHandler(new ChannelInitializer<UkcpChannel>() {
				protected void initChannel(UkcpChannel ch) throws Exception {
					PingHandler.init(ch.pipeline());
				}
			});
        Channel ch = b.bind(Global.ADDR, Global.PORT).syncUninterruptibly().channel();
        ch.closeFuture().syncUninterruptibly();
        worker.shutdownGracefully();
	}
}
