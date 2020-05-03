package com.lwz.server;

import com.lwz.codec.ZrpcDecoder;
import com.lwz.codec.ZrpcEncoder;
import com.lwz.registry.DirectRegistrar;
import com.lwz.registry.Registrar;
import com.lwz.registry.ServerInfo;
import com.lwz.registry.ZooKeeperRegistrar;
import com.lwz.util.IPUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liweizhou 2020/4/6
 */
@Slf4j
public class ZrpcServer implements ApplicationRunner {

    private ServerProperties serverProperties;

    private DispatcherHandler dispatcherHandler;

    private EventLoopGroup selectorGroup;

    private EventLoopGroup codecGroup;

    private EventExecutorGroup handlerGroup;

    private ChannelFuture channel;

    private Registrar registrar;

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ZrpcServer(ServerProperties serverProperties, DispatcherHandler dispatcherHandler) {
        this.serverProperties = serverProperties;
        this.dispatcherHandler = dispatcherHandler;
    }

    @PostConstruct
    public void initServerChannel() {
        try {
            selectorGroup = new NioEventLoopGroup(1);
            codecGroup = new NioEventLoopGroup();
            handlerGroup = new DefaultEventExecutorGroup(NettyRuntime.availableProcessors() * 4);
            ServerBootstrap server = new ServerBootstrap();
            server.group(selectorGroup, codecGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //TODO: 一些常用handler
                                    .addLast("decoder", new ZrpcDecoder())
                                    .addLast("encoder", new ZrpcEncoder())
                                    .addLast(handlerGroup, "dispatcher", dispatcherHandler);
                        }
                    });

            channel = server.bind(serverProperties.getPort()).sync();
            log.info("server bind {} success", serverProperties.getPort());
        } catch (Exception e) {
            //启动失败, 手动结束
            destroy();
            throw new BeanCreationException(String.format("ZrpcServer:%d", serverProperties.getPort()), e);
        }
    }

    @PreDestroy
    public void destroy(){
        try {
            if (stop.compareAndSet(false, true)) {
                registrar.signOut();
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
        if (serverProperties.getRegistry() == null || serverProperties.getRegistry().getRegistryType() == null) {
            registrar = new DirectRegistrar();
        } else {
            switch (serverProperties.getRegistry().getRegistryType()) {
                case ZOOKEEPER:
                    registrar = new ZooKeeperRegistrar(serverProperties.getRegistry());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("registryType:%s not implement",
                            serverProperties.getRegistry().getRegistryType()));
            }
        }

        //注册
        ServerInfo serverInfo = new ServerInfo(IPUtils.getIp(), serverProperties.getPort());
        registrar.signIn(serverInfo);
    }

}
