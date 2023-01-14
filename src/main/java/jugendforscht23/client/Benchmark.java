package jugendforscht23.client;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Benchmark {
	public static void init(Channel ch) {
		ch.pipeline().addLast(
			new ProtobufVarint32FrameDecoder(),
			new ProtobufVarint32LengthFieldPrepender(),
			new LoggingHandler(LogLevel.INFO),
			new ChannelInboundHandlerAdapter() {
				public void channelActive(ChannelHandlerContext ctx) throws Exception {
					super.channelActive(ctx);
					ch.writeAndFlush(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("1234567890")));
				}
			}
		);
	}
}
