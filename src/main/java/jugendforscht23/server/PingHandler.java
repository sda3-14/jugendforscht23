package jugendforscht23.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class PingHandler {
	public static void init(ChannelPipeline pipeline) {
		pipeline.addLast(
			new ProtobufVarint32FrameDecoder(),
			new ProtobufVarint32LengthFieldPrepender(),
			new LoggingHandler(LogLevel.INFO),
			new SimpleChannelInboundHandler<ByteBuf>() {
				protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
					ctx.writeAndFlush(msg.retain());
				}
			}
		);
	}
}
