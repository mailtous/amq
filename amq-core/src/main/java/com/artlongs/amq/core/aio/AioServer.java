package com.artlongs.amq.core.aio;

import com.artlongs.amq.core.aio.plugin.*;
import com.artlongs.amq.tools.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Func : Aio 服务端
 *
 * @author: leeton on 2019/2/22.
 */
public class AioServer<T> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AioServer.class);
    /**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected AioServerConfig<T> config = new AioServerConfig<>(true);

    // 读线程池
    private ExecutorService readExecutorService ;
    /**
     * 读回调事件处理
     */
    protected Reader<T> reader;
    /**
     * 写回调事件处理
     */
    protected Writer<T> writer = new Writer<>();
    private Function<AsynchronousSocketChannel, AioPipe<T>> aioPipeFunction;

    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelThreadPool;

    /**
     * 客户端存活列表,ConcurrentHashMap[PipeID,AioPipe]
     */
    private static ConcurrentHashMap<Integer, AioPipe> channelAliveMap = new ConcurrentHashMap<>(2000);

    private boolean checkAlive = false;

    private MonitorPlugin monitor;

    /**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioServer(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        config.setHost(host);
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
        if (checkAlive) {//运行检测心跳
            new ChannelAliveCheckPlugin(this.channelAliveMap).run();
        }
    }

    @Override
    public void run() {
    }

    /**
     * 启动Server端的AIO服务
     *
     * @throws IOException
     */
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            LOGGER.info(config.BANNER + "\r\n :: amq-socket ::\t(" + config.VERSION + ")");
        }
        start0((AsynchronousSocketChannel channel)->new AioPipe<T>(channel, config));
    }

    /**
     * 内部启动逻辑
     *
     * @throws IOException
     */
    protected final void start0(Function<AsynchronousSocketChannel, AioPipe<T>> aioPipeFunction) throws IOException {
        try {
            readExecutorService = IOUtils.createFixedThreadPool(config.getServerThreadNum(), "AIO:read-");

            this.aioPipeFunction = aioPipeFunction;
            asynchronousChannelThreadPool = AsynchronousChannelGroup.withFixedThreadPool(config.getServerThreadNum(), new ThreadFactory() {
                byte index = 0;
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Aio:accept-" + (++index));
                }
            });
            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelThreadPool);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (SocketOption option : config.getSocketOptions()) {
                    this.serverSocketChannel.setOption(option, option.type());
                }
            }else {
                setDefSocketOptions();
            }

            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), 1000);
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), 1000);
            }
            serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
                @Override
                public void completed(final AsynchronousSocketChannel channel, AsynchronousServerSocketChannel serverSocketChannel) {
                    String remoteAddressStr = IOUtils.getRemoteAddressStr(channel);
                    if (IpPlugin.findInBlackList(remoteAddressStr)) {
                        IOUtils.closeChannel(channel);
                        LOGGER.warn("[X]Find black IP ({}),so close connetion.", remoteAddressStr);
                    } else {
                        readExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                createPipe(channel);
                            }
                        });
//                        createPipe(channel);
                    }
                    serverSocketChannel.accept(serverSocketChannel, this);
                }

                @Override
                public void failed(Throwable exc, AsynchronousServerSocketChannel serverSocketChannel) {
                    LOGGER.error("amq-socket server accept fail", exc);
                }
            });
        } catch (IOException e) {
            shutdown();
            throw e;
        }
        LOGGER.warn("amq-socket server started on {} {}", config.getHost(), config.getPort());
        LOGGER.info("amq-socket server config is {}", config);
    }



    /**
     * 为每个新建立的连接创建 AioPipe 对象
     *
     * @param channel
     */
    private AioPipe createPipe(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        AioPipe<T> pipe = null;
        try {
            pipe = aioPipeFunction.apply(channel);
            pipe.initSession();
//            System.err.println("create pipid = "+ pipe.getId());
            if (null != pipe) {
                channelAliveMap.putIfAbsent(pipe.getId(), pipe);
            }
        } catch (Exception e1) {
            LOGGER.debug(e1.getMessage(), e1);
            if (null == pipe) {
                try {
                    channel.shutdownInput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.shutdownOutput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.close();
                } catch (IOException e) {
                    LOGGER.debug("close channel exception", e);
                }
            } else {
                pipe.close();
            }

        }
        return pipe;
    }

    /**
     * 停止服务端
     */
    public final void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
            channelAliveMap.clear();
            channelAliveMap = null;
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        //先尝试关闭服务,但运中的任务还在跑...
        asynchronousChannelThreadPool.shutdown();
        // 真正关闭服务
        if (!asynchronousChannelThreadPool.isTerminated()) {
            try {
                asynchronousChannelThreadPool.shutdownNow();
            } catch (IOException e) {
                LOGGER.error("shutdown exception", e);
            }
        }
/*        try {
            asynchronousChannelThreadPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("shutdown exception", e);
        }*/
        // 关闭流量统计
        Monitor monitor = this.config.getProcessor().getMonitor();
        if (monitor != null) {
            ((MonitorPlugin) monitor).cancel();
        }


    }


    /**
     * 设置处理线程数量
     *
     * @param num 线程数
     */
    public final AioServer<T> setThreadNum(int num) {
        this.config.setServerThreadNum(num);
        return this;
    }


    /**
     * 设置输出队列缓冲区长度
     *
     * @param size 缓存队列长度
     */
    public final AioServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final AioServer<T> setReadBufferSize(int size) {
        this.config.setDirctBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     */
    public final AioServer<T> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 设置Socket的TCP参数配置。
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * </p>
     *
     * @return
     */
    public final <V> AioServer<T> setOption(SocketOption options) {
        config.setSocketOptions(options);
        return this;
    }

    public final AioServer<T> addPlugin(Plugin plugin) {
        config.getProcessor().addPlugin(plugin);
        return this;
    }

    public AioServer<T> startCheckAlive(boolean tf) {
        this.checkAlive = tf;
        return this;
    }

    public AioServer<T> startMonitorPlugin(boolean tf) {
        if (tf){
            this.monitor = new MonitorPlugin();
            addPlugin(this.monitor);
        }
        return this;
    }

    public MonitorPlugin getMonitor() {
        return monitor;
    }

    private void setDefSocketOptions(){
        try {
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<Integer, AioPipe> getChannelAliveMap() {
        return channelAliveMap;
    }

}
