package jugendforscht23.client;

import io.jpower.kcp.netty.ChannelOptionHelper;
import io.jpower.kcp.netty.UkcpChannel;
import io.jpower.kcp.netty.UkcpClientChannel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jugendforscht23.Global;

public class KCPClient {
	public static void main(String[] args) {
		EventLoopGroup worker = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap()
			.group(worker)
			.channel(UkcpClientChannel.class)
			.handler(new ChannelInitializer<UkcpChannel>(){
				public void initChannel(UkcpChannel ch) throws Exception {
					Benchmark.init(ch.pipeline());
				}
			});
		bootstrap = ChannelOptionHelper.nodelay(bootstrap, true, 20, 2, true);
		Channel ch = bootstrap.connect(Global.ADDR1, Global.PORT).syncUninterruptibly().channel();
		ch.closeFuture().syncUninterruptibly();
		worker.shutdownGracefully();
	}
}
