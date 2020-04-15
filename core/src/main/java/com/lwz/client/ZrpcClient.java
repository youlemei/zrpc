package com.lwz.client;

import com.lwz.codec.ZZPDecoder;
import com.lwz.codec.ZZPEncoder;
import com.lwz.message.ZZPMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class ZrpcClient {

    private ClientConfig clientConfig;

    private ChannelFuture channel;

    private AtomicInteger seq = new AtomicInteger((int) (Math.random() * 100000000));

    private ConcurrentMap<Integer, ResponseFutureImpl> responseFutureMap = new ConcurrentHashMap<>();

    public ZrpcClient(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        init();
    }

    private void init() {
        NioEventLoopGroup codec = new NioEventLoopGroup(1);
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
        try {
            channel = client.connect(clientConfig.getHost(), clientConfig.getPort()).sync();
            log.info("client connect {}:{} success.", clientConfig.getHost(), clientConfig.getPort());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //关闭问题->Factory
        new Thread(()->{
            try {
                channel.channel().closeFuture().sync();
                log.info("client close {}:{} success.", clientConfig.getHost(), clientConfig.getPort());
            } catch (Exception e) {
                log.error("client close {}:{} fail. err:{}", clientConfig.getHost(), clientConfig.getPort(), e.getMessage(), e);
            } finally {
                codec.shutdownGracefully();
            }
        }).start();
    }

    public ResponseFuture request(ZZPMessage zzpMessage) {
        int seq = this.seq.incrementAndGet();
        zzpMessage.getHeader().setSeq(seq);
        channel.channel().writeAndFlush(zzpMessage);
        ResponseFutureImpl responseFuture = new ResponseFutureImpl<>();
        responseFutureMap.put(seq, responseFuture);
        return responseFuture;
    }

    public ResponseFutureImpl getResponseFuture(int seq) {
        //TODO: 防止OOM问题
        return responseFutureMap.remove(seq);
    }

}
