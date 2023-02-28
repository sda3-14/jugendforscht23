package jugendforscht23.server;

import io.jpower.kcp.netty.ChannelOptionHelper;
import io.jpower.kcp.netty.UkcpChannel;
import io.jpower.kcp.netty.UkcpServerChannel;
import io.netty.bootstrap.UkcpServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jugendforscht23.Global;


public class KCPServer {
	public static void main(String[] args) {
		EventLoopGroup worker = new NioEventLoopGroup();
		UkcpServerBootstrap bootstrap = new UkcpServerBootstrap()
			.group(worker)
			.channel(UkcpServerChannel.class)
			.childHandler(new ChannelInitializer<UkcpChannel>() {
				protected void initChannel(UkcpChannel ch) throws Exception {
					PingHandler.init(ch.pipeline());
				}
			});
		bootstrap = ChannelOptionHelper.nodelay(bootstrap, true, 20, 2, true);
		Channel ch = bootstrap.bind(Global.ADDR2, Global.PORT).syncUninterruptibly().channel();
		ch.closeFuture().syncUninterruptibly();
		worker.shutdownGracefully();
	}
}
