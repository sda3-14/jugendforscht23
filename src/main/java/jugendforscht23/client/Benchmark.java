package jugendforscht23.client;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Benchmark {
	private static final Logger LOG = LoggerFactory.getLogger(Benchmark.class);

	public static ReentrantLock init(ChannelPipeline pipeline) {
		ReentrantLock lock = new ReentrantLock();
		pipeline.addLast(
			new ProtobufVarint32FrameDecoder(),
			new ProtobufVarint32LengthFieldPrepender(),
			new LoggingHandler(LogLevel.DEBUG),
			new ChannelInboundHandlerAdapter() {
				public void channelActive(ChannelHandlerContext ctx) throws Exception {
					super.channelActive(ctx);
					new Thread(new Runnable() {
						public void run() {
							try {
								while(true) {
									lock.lock();
									long t = System.nanoTime();
									ByteBuf b = ctx.alloc().buffer();
									b.writeLong(t);
									ctx.writeAndFlush(b);
									lock.unlock();
									Thread.sleep(5);
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
				public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
					assert msg instanceof ByteBuf;
					ByteBuf buf = (ByteBuf) msg;
					long t = buf.readLong();
					t = System.nanoTime() - t;
					LOG.info("delay:" + t/1000000d);
				}
			}
		);
		return lock;
	}
}
