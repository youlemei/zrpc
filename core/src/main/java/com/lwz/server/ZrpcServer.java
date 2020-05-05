package com.lwz.server;

import com.lwz.codec.ZrpcDecoder;
import com.lwz.codec.ZrpcEncoder;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import com.lwz.util.IPUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liweizhou 2020/4/6
 */
public class ZrpcServer implements ApplicationRunner {

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

    @PostConstruct
    public void initServerChannel() {
        try {
            selectorGroup = new NioEventLoopGroup(1);
            codecGroup = new NioEventLoopGroup();
            handlerGroup = new DefaultEventLoopGroup(NettyRuntime.availableProcessors() * 4);
            ServerBootstrap server = new ServerBootstrap();
            server.group(selectorGroup, codecGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //.addLast("log", new LoggingHandler(LogLevel.DEBUG))
                                    //.addLast("idleState", new IdleStateHandler(0, 0, serverProperties.getTimeout(), TimeUnit.SECONDS))
                                    //.addLast("IdleChannelCloseHandler", new IdleChannelCloseHandler()); //handle IdleStateEvent
                                    //.addLast("ipFilter", new IPFilterHandler())
                                    .addLast("decoder", new ZrpcDecoder())
                                    .addLast("encoder", new ZrpcEncoder())
                                    .addLast("dispatcher", new DispatcherHandler(handlerRegistrar, handlerGroup));
                        }
                    });

            channel = server.bind(serverProperties.getPort()).sync();
            log.info("server bind {} success", serverProperties.getPort());
        } catch (Exception e) {
            //启动失败, 手动结束 TODO: 某些情况还是结束不了
            destroy();
            throw new BeanCreationException(String.format("ZrpcServer:%d", serverProperties.getPort()), e);
        }
    }

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
            selectorGroup.shutdownGracefully();
            codecGroup.shutdownGracefully();
            handlerGroup.shutdownGracefully();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //注册
        ServerInfo serverInfo = new ServerInfo(IPUtils.getIp(), serverProperties.getPort());
        registrar.register(serverProperties.getServerName(), serverInfo);
    }

}
