package com.lwz.client;

import com.lwz.codec.ZZPDecoder;
import com.lwz.codec.ZZPEncoder;
import com.lwz.message.ZZPHeader;
import com.lwz.message.ZZPMessage;
import com.lwz.registry.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class ZrpcClient implements Closeable {

    private ServerInfo serverInfo;

    private int timeout;

    private ChannelFuture channel;

    private NioEventLoopGroup codec;

    private AtomicInteger seq = new AtomicInteger(ThreadLocalRandom.current().nextInt(100000000));

    private ConcurrentMap<Integer, ResponseFutureImpl> responseFutureMap = new ConcurrentHashMap<>();

    public ZrpcClient(ServerInfo serverInfo, int timeout) throws InterruptedException {
        this.serverInfo = serverInfo;
        this.timeout = timeout;
        init();
    }

    private void init() throws InterruptedException {
        codec = new NioEventLoopGroup(1);
        Bootstrap client = new Bootstrap();
        client.group(codec)
                .channel(NioSocketChannel.class)
                //.option(ChannelOption.SO_TIMEOUT, clientConfig.getTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("encoder", new ZZPEncoder())
                                .addLast("decoder", new ZZPDecoder())
                                .addLast("response", new ResponseHandler(ZrpcClient.this));
                    }
                });
        channel = client.connect(serverInfo.getHost(), serverInfo.getPort()).sync();
        log.info("client connect {}:{} success.", serverInfo.getHost(), serverInfo.getPort());
    }

    @Override
    public void close() {
        try {
            channel.channel().close().sync();
            log.info("client close {}:{} success.", serverInfo.getHost(), serverInfo.getPort());
        } catch (Exception e) {
            log.warn("client close {}:{} fail. err:{}", serverInfo.getHost(), serverInfo.getPort(), e.getMessage(), e);
        } finally {
            codec.shutdownGracefully();
        }
    }

    public ResponseFuture request(ZZPMessage zzpMessage, Class<?> returnType) {
        int seq = this.seq.incrementAndGet();
        zzpMessage.getHeader().setSeq(seq);
        ResponseFutureImpl responseFuture = new ResponseFutureImpl<>(returnType);
        responseFutureMap.put(seq, responseFuture);
        channel.channel().writeAndFlush(zzpMessage);
        return responseFuture;
    }

    public ResponseFutureImpl getResponseFuture(int seq) {
        //TODO: 防止OOM问题
        return responseFutureMap.remove(seq);
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void ping() throws Exception {
        int seq = this.seq.incrementAndGet();
        ZZPHeader zzpHeader = new ZZPHeader();
        zzpHeader.setSeq(seq);
        zzpHeader.setExt(ZZPHeader.PING);
        ZZPMessage zzpMessage = new ZZPMessage();
        zzpMessage.setHeader(zzpHeader);
        ResponseFutureImpl responseFuture = new ResponseFutureImpl<>();
        responseFutureMap.put(seq, responseFuture);
        channel.channel().writeAndFlush(zzpMessage);
        responseFuture.get();
    }
}
