package com.lwz.client;

import com.lwz.client.pool.ClientPool;
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

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liweizhou 2020/4/12
 */
@Slf4j
public class ZrpcClient {

    private ClientPool clientPool;

    private ServerInfo serverInfo;

    private int timeout;

    private ChannelFuture channel;

    private NioEventLoopGroup codec;

    private AtomicInteger seq = new AtomicInteger(ThreadLocalRandom.current().nextInt(100000000));

    private ConcurrentMap<Integer, ResponseFutureImpl> responseFutureMap = new ConcurrentHashMap<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ZrpcClient(ClientPool clientPool, ServerInfo serverInfo, int timeout) throws InterruptedException {
        this.clientPool = clientPool;
        this.serverInfo = serverInfo;
        this.timeout = timeout > 0 ? timeout : ClientProperties.DEFAULT_TIMEOUT;
        initChannel();
    }

    private void initChannel() throws InterruptedException {
        codec = new NioEventLoopGroup(1);
        Bootstrap client = new Bootstrap();
        client.group(codec)
                .channel(NioSocketChannel.class)
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

    public void close() {
        log.info("close remainSize:{}", responseFutureMap.size());
        if (responseFutureMap.isEmpty()) {
            doClose();
        } else {
            //等待回应超时
            codec.schedule(() -> doClose(), timeout, TimeUnit.SECONDS);
        }
    }

    private void doClose() {
        try {
            if (stop.compareAndSet(false, true)) {
                channel.channel().close().sync();
                responseFutureMap.clear();
                log.info("client close success. {}:{}", serverInfo.getHost(), serverInfo.getPort());
            }
        } catch (Exception e) {
            log.warn("client close fail. {}:{} err:{}", serverInfo.getHost(), serverInfo.getPort(), e.getMessage(), e);
        } finally {
            codec.shutdownGracefully();
        }
    }

    public ResponseFuture request(ZZPMessage zzpMessage, Class<?> returnType) {
        int seq = this.seq.incrementAndGet();
        zzpMessage.getHeader().setSeq(seq);
        ResponseFutureImpl responseFuture = new ResponseFutureImpl<>(returnType);
        registryResponseFuture(seq, responseFuture);
        channel.channel().writeAndFlush(zzpMessage);
        return responseFuture;
    }

    private ResponseFutureImpl registryResponseFuture(int seq, ResponseFutureImpl responseFuture) {
        //注册超时
        codec.schedule(() -> {
            ResponseFutureImpl future = responseFutureMap.remove(seq);
            if (future != null) {
                future.fail(new TimeoutException(String.format("request timeout. begin:%s now:%s", future.getCreate(), LocalDateTime.now())));
                //n次后
                //clientPool.invalidateObject(this);
            }
        }, timeout, TimeUnit.SECONDS);
        return responseFutureMap.put(seq, responseFuture);
    }

    public ResponseFutureImpl getResponseFuture(int seq) {
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
        registryResponseFuture(seq, responseFuture);
        channel.channel().writeAndFlush(zzpMessage);
        responseFuture.get();
    }

    public boolean isOpen() {
        //isActive底层会同步判断状态, 效率较低
        return channel.channel().isOpen();
    }

    public void disablePool() {
        clientPool.disable();
        clientPool.invalidateObject(this);
    }

}
