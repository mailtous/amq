package com.artlongs.amq.core.aio;

import com.artlongs.amq.core.aio.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * Func : Aio 客户端
 *
 * @author: leeton on 2019/2/22.
 */
public class AioClient<T> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AioClient.class);

    /**
     * 客户端服务配置。
     * <p>调用AioQuickClient的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected AioServerConfig<T> config = new AioServerConfig<>(false);
    /**
     * 网络连接的会话对象
     *
     * @see AioPipe
     */
    protected AioPipe<T> pipe;
    /**
     * IO事件处理线程组。
     * <p>
     * 作为客户端，该AsynchronousChannelGroup只需保证2个长度的线程池大小即可满足通信读写所需。
     * </p>
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 当前构造方法设置了启动Aio客户端的必要参数，基本实现开箱即用。
     *
     * @param host             远程服务器地址
     * @param port             远程服务器端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioClient(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        config.setHost(host);
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    /**
     * 启动客户端。
     * <p>
     * 在与服务端建立连接期间，该方法处于阻塞状态。直至连接建立成功，或者发生异常。
     * </p>
     * <p>
     * 该start方法支持外部指定AsynchronousChannelGroup，实现多个客户端共享一组线程池资源，有效提升资源利用率。
     * </p>
     *
     * @param asynchronousChannelGroup IO事件处理线程组
     * @see {@link AsynchronousSocketChannel#connect(SocketAddress)}
     */
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        //set socket options
        if (config.getSocketOptions() != null) {
            for (SocketOption option :config.getSocketOptions()) {
                socketChannel.setOption(option,option.type());
            }
        }else {
            setDefSocketOptions(socketChannel);
        }
        //bind host
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造AIOSession对象
        pipe = new AioPipe<>(socketChannel, config);
        pipe.initSession();
        logger.warn("amq-socket client started on {} {}", config.getHost(), config.getPort());
        return pipe;
    }

    /**
     * 启动客户端。
     *
     * <p>
     * 本方法会构建线程数为2的{@code asynchronousChannelGroup}，并通过调用{@link AioClient#start(AsynchronousChannelGroup)}启动服务。
     * </p>
     *
     * @see {@link AioClient#start(AsynchronousChannelGroup)
     */
    public final AioPipe<T> start() throws IOException, ExecutionException, InterruptedException {
        this.asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        return start(asynchronousChannelGroup);
    }

    /**
     * 停止客户端服务.
     * <p>
     * 调用该方法会触发AioSession的close方法，并且如果当前客户端若是通过执行{@link AioClient#start()}方法构建的，同时会触发asynchronousChannelGroup的shutdown动作。
     * </p>
     */
    public final void shutdown() {
        if (pipe != null) {
            pipe.close();
            pipe = null;
        }
        //仅Client内部创建的ChannelGroup需要shutdown
        if (asynchronousChannelGroup != null) {
            asynchronousChannelGroup.shutdown();
        }
    }


    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final AioClient<T> setReadBufferSize(int size) {
        this.config.setDirctBufferSize(size);
        return this;
    }

    /**
     * 设置输出队列缓冲区长度。输出缓冲区的内存大小取决于size个ByteBuffer的大小总和。
     *
     * @param size 缓冲区数组长度
     */
    public final AioClient<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置Socket的TCP参数配置
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 1. StandardSocketOptions.SO_SNDBUF<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 3. StandardSocketOptions.SO_KEEPALIVE<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * 5. StandardSocketOptions.TCP_NODELAY
     * </p>
     *
     * @param socketOption 配置项
     * @return
     */
    public final <V> AioClient<T> setOption(SocketOption socketOption) {
        config.setSocketOptions(socketOption);
        return this;
    }

    public final AioClient<T> addPlugin(Plugin plugin) {
        config.getProcessor().addPlugin(plugin);
        return this;
    }

    public AioServerConfig<T> getConfig() {
        return config;
    }

    public AioPipe<T> getPipe() {
        return pipe;
    }

    private void setDefSocketOptions(AsynchronousSocketChannel socketChannel){
        try {
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
            socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // nothing to do 只是方便运行在守护模式
    }
}
