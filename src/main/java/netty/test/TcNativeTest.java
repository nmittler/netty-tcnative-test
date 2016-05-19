package netty.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import java.util.List;

/**
 * Basic test of an SSL client using OpenSSL.
 */
public class TcNativeTest {

  public static void main(String[] args) throws InterruptedException {
    if(!printOpenSslDetails()) {
      return;
    }

    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          // Add the SslHandler.
          SslContext sslContext = SslContextBuilder.forClient().sslProvider(SslProvider.OPENSSL).build();
          ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));

          ch.pipeline().addLast(new HttpRequestEncoder());
          ch.pipeline().addLast(new HttpResponseDecoder());
          ch.pipeline().addLast(new MessageToMessageDecoder<HttpResponse>() {
            @Override
            protected void decode(ChannelHandlerContext ctx, HttpResponse httpResponse, List<Object> list) throws Exception {
              System.out.println(httpResponse);
              ctx.channel().close();
            }
          });
        }
      });

      // Start the client.
      Channel channel = b.connect("www.google.com", 443).sync().channel();

      // Now send a GET request to the server.
      channel.writeAndFlush(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));

      // Wait until the connection is closed.
      channel.closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
    }
  }

  private static boolean printOpenSslDetails() {
    System.err.println("available: " + OpenSsl.isAvailable());
    if (OpenSsl.isAvailable()) {
      System.err.println("alpn: " + OpenSsl.isAlpnSupported());
      System.err.println("version: " + Integer.toHexString(OpenSsl.version()));
      System.err.println("versionString: " + OpenSsl.versionString());
      return true;
    }

    OpenSsl.unavailabilityCause().printStackTrace();
    return false;
  }
}
