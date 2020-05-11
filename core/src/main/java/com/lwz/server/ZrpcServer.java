package com.lwz.server;

import com.lwz.codec.ZrpcDecoder;
import com.lwz.codec.ZrpcEncoder;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import com.lwz.util.IPUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.NettyRuntime;
import org.apache.zookeeper.common.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liweizhou 2020/4/6
 */
public class ZrpcServer implements ApplicationRunner, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ZrpcServer.class);

    private ServerProperties serverProperties;

    private HandlerRegistrar handlerRegistrar;

    private Registrar registrar;

    private EventLoopGroup selectorGroup;

    private EventLoopGroup codecGroup;

    private EventLoopGroup handlerGroup;

    private ChannelFuture channel;

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ZrpcServer(ServerProperties serverProperties, HandlerRegistrar handlerRegistrar, Registrar registrar) {
        this.serverProperties = serverProperties;
        this.handlerRegistrar = handlerRegistrar;
        this.registrar = registrar;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //启动服务
        try {
            selectorGroup = NettyUtils.newNioOrEpollEventLoopGroup(1);
            codecGroup = NettyUtils.newNioOrEpollEventLoopGroup();
            handlerGroup = new DefaultEventLoopGroup(NettyRuntime.availableProcessors() * 4);
            ServerBootstrap server = new ServerBootstrap();
            server.group(selectorGroup, codecGroup)
                    .channel(NettyUtils.nioOrEpollServerSocketChannel())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //.addLast("log", new LoggingHandler(LogLevel.DEBUG))
                                    //.addLast("ssl", new SSLHandler(engine))
                                    //.addLast("idleState", new IdleStateHandler(0, 0, serverProperties.getTimeout(), TimeUnit.SECONDS))
                                    //.addLast("IdleChannelClose", new IdleChannelCloseHandler()); //handle IdleStateEvent
                                    //.addLast("ipFilter", new IPFilterHandler())
                                    .addLast("decoder", new ZrpcDecoder())
                                    .addLast("encoder", new ZrpcEncoder())
                                    .addLast("dispatcher", new DispatcherHandler(handlerRegistrar, handlerGroup));
                        }
                    });

            channel = server.bind(serverProperties.getPort()).sync();
            log.info("server bind {} success", serverProperties.getPort());
        } catch (Exception e) {
            throw new RuntimeException(String.format("server bind %d fail", serverProperties.getPort()), e);
        }
    }

    private Duration shutdownWait = Duration.ofSeconds(10);

    @PreDestroy
    public void destroy(){
        try {
            if (stop.compareAndSet(false, true)) {
                registrar.unRegister();

                // 优雅停机
                if (channel != null) {
                    channel.channel().close().sync();
                }
                log.info("server {} close success", serverProperties.getPort());
            }
        } catch (Exception e) {
            log.warn("server {} close fail. err:{}", serverProperties.getPort(), e.getMessage());
        } finally {
            handlerGroup.shutdownGracefully();
            codecGroup.shutdownGracefully();
            selectorGroup.shutdownGracefully();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //注册
        ServerInfo serverInfo = new ServerInfo(IPUtils.getIp(), serverProperties.getPort());
        registrar.register(serverProperties.getServerName(), serverInfo);
    }
}
