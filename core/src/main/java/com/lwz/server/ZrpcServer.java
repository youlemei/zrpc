package com.lwz.server;

import com.lwz.codec.ZZPDecoder;
import com.lwz.codec.ZZPEncoder;
import com.lwz.filter.InboundExceptionHandler;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author liweizhou 2020/4/6
 */
@Slf4j
public class ZrpcServer implements ApplicationRunner {

    private ServerProperties serverConfig;

    private DispatcherHandler dispatcherHandler;

    private EventLoopGroup selectorGroup;

    private EventLoopGroup codecGroup;

    private EventExecutorGroup handlerGroup;

    private ChannelFuture channel;

    public ZrpcServer(ServerProperties serverConfig, DispatcherHandler dispatcherHandler) {
        this.serverConfig = serverConfig;
        this.dispatcherHandler = dispatcherHandler;
    }

    @PostConstruct
    public void init() throws Exception {
        selectorGroup = new NioEventLoopGroup(1);
        codecGroup = new NioEventLoopGroup();
        handlerGroup = new DefaultEventExecutorGroup(NettyRuntime.availableProcessors() * 4);
        ServerBootstrap server = new ServerBootstrap();
        server.group(selectorGroup, codecGroup)
                .channel(NioServerSocketChannel.class)
                //.childOption(ChannelOption.SO_TIMEOUT, serverConfig.getTimeout())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                //TODO: 一些常用handler
                                .addLast("decoder", new ZZPDecoder())
                                .addLast("encoder", new ZZPEncoder())
                                .addLast(handlerGroup, "dispatcher", dispatcherHandler)
                                .addLast("exception", new InboundExceptionHandler());
                    }
                });
        channel = server.bind(serverConfig.getPort()).sync();
        log.info("zrpc server bind {} success", serverConfig.getPort());
    }

    @PreDestroy
    public void destroy(){
        try {
            channel.channel().close().sync();
            log.info("zrpc server {} close success", serverConfig.getPort());
        } catch (Exception e) {
            log.error("zrpc server close fail. err:{}", e.getMessage(), e);
        } finally {
            selectorGroup.shutdownGracefully();
            codecGroup.shutdownGracefully();
            handlerGroup.shutdownGracefully();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //TODO: 注册
    }

}
