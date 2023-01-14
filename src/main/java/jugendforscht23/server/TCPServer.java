package jugendforscht23.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jugendforscht23.Global;

public class TCPServer {
	public static void main(String[] args) {
		EventLoopGroup worker = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap()
			.group(worker)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<NioSocketChannel>() {
				protected void initChannel(NioSocketChannel ch) throws Exception {
					PingHandler.init(ch);
				}
			});
		NioServerSocketChannel ch = (NioServerSocketChannel) bootstrap.bind(Global.ADDR, Global.PORT).syncUninterruptibly().channel();
		ch.closeFuture().syncUninterruptibly();
		worker.shutdownGracefully();
	}
}
