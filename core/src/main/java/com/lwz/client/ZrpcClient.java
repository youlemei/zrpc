package com.lwz.client;

import com.lwz.client.pool.ClientPool;
import com.lwz.codec.ZrpcDecoder;
import com.lwz.codec.ZrpcEncoder;
import com.lwz.message.EncodeObj;
import com.lwz.message.Header;
import com.lwz.registry.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.apache.zookeeper.common.NettyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liweizhou 2020/4/12
 */
public class ZrpcClient {

    private static final Logger log = LoggerFactory.getLogger(ZrpcClient.class);

    private ClientPool clientPool;

    private ServerInfo serverInfo;

    private int timeout;

    private ChannelFuture channel;

    private EventLoopGroup codec;

    private AtomicInteger seq = new AtomicInteger(ThreadLocalRandom.current().nextInt(100000000));

    private ConcurrentMap<Integer, ResponseFutureImpl> responseFutureMap = new ConcurrentHashMap<>();

    private AtomicBoolean stop = new AtomicBoolean(false);

    public ZrpcClient(ClientPool clientPool, ServerInfo serverInfo, int timeout) throws Exception {
        this.clientPool = clientPool;
        this.serverInfo = serverInfo;
        this.timeout = timeout > 0 ? timeout : ClientConfig.DEFAULT_TIMEOUT;
        initChannel();
    }

    private void initChannel() throws Exception {
        codec = NettyUtils.newNioOrEpollEventLoopGroup(1);
        Bootstrap client = new Bootstrap();
        client.group(codec)
                .channel(NettyUtils.nioOrEpollSocketChannel())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                //.addLast("log", new LoggingHandler(LogLevel.DEBUG))
                                //.addLast("ssl", new SSLHandler(engine))
                                .addLast("encoder", new ZrpcEncoder())
                                .addLast("decoder", new ZrpcDecoder())
                                .addLast("response", new ResponseHandler(ZrpcClient.this));
                    }
                });
        channel = client.connect(serverInfo.getHost(), serverInfo.getPort()).sync();
        log.info("client connect {}:{} success.", serverInfo.getHost(), serverInfo.getPort());
    }

    public void close() {
        log.info("close client remainSize:{}", responseFutureMap.size());
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
                if (channel != null) {
                    channel.channel().close().sync();
                }
                //每个Future都fail?
                responseFutureMap.clear();
                log.info("client close success. {}:{}", serverInfo.getHost(), serverInfo.getPort());
            }
        } catch (Exception e) {
            log.warn("client close fail. {}:{} err:{}", serverInfo.getHost(), serverInfo.getPort(), e.getMessage(), e);
        } finally {
            codec.shutdownGracefully();
        }
    }

    /**
     * TODO: 调用链
     */
    public ResponseFuture request(int uri, Object[] args, Type[] argsTypes, Type returnType) {
        int seq = this.seq.incrementAndGet();
        Header header = new Header();
        header.setUri(uri);
        header.setSeq(seq);
        EncodeObj encodeObj = new EncodeObj();
        encodeObj.setHeader(header);
        encodeObj.setBodys(args);
        encodeObj.setBodyTypes(argsTypes);

        ResponseFutureImpl responseFuture = new ResponseFutureImpl(returnType);
        registryResponseFuture(seq, responseFuture);
        channel.channel().writeAndFlush(encodeObj);
        return responseFuture;
    }

    private ResponseFutureImpl registryResponseFuture(int seq, ResponseFutureImpl responseFuture) {
        //注册超时
        codec.schedule(() -> {
            ResponseFutureImpl future = getResponseFuture(seq);
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
        Header header = new Header();
        header.setSeq(seq);
        header.setExt(Header.PING);
        EncodeObj encodeObj = new EncodeObj();
        encodeObj.setHeader(header);

        ResponseFutureImpl responseFuture = new ResponseFutureImpl();
        registryResponseFuture(seq, responseFuture);
        channel.channel().writeAndFlush(encodeObj);
        responseFuture.get();
    }

    public boolean isOpen() {
        //isActive底层会同步判断状态, 效率较低
        return channel != null && channel.channel().isOpen();
    }

    public void disablePool() {
        clientPool.disable();
        clientPool.invalidateObject(this);
    }

}
