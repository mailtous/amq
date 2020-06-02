package com.artfii.amq.core.aio;

import com.artfii.amq.core.MqConfig;

import java.net.SocketOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Func : AIO 配置项
 *
 * @author: leeton on 2019/2/22.
 */
public class AioServerConfig<T> {
    private boolean isServer;
    public static final String BANNER = "\n" +
            "   _              ____ \n" +
            "  /_\\    /\\/\\    /___ \\\n" +
            " //_\\\\  /    \\  //  / /\n" +
            "/  _  \\/ /\\/\\ \\/ \\_/ / \n" +
            "\\_/ \\_/\\/    \\/\\___,_\\ \n" +
            "                       ";

    public static final String VERSION = "v0.0.1";

    private List<SocketOption> socketOptions = new ArrayList<>();

    public String host ;
    public int port;

    private AioProcessor<T> processor;
    private Protocol<T> protocol;

    /**
     * buffer队列初始容量大小,最好根据IO请求的并发量来设置(2的倍数)
     */
    public int queueSize = 10_000;
    /**
     * 消息体缓存大小,字节(这个实际上是一条消息的最大容量)
     */
    private int dirctBufferSize = 2048;


    /**
     * 服务器处理线程数
     */
    private int serverThreadNum = MqConfig.inst.server_channel_event_thread_size;
//    private int serverThreadNum = Runtime.getRuntime().availableProcessors() + 1;
    private float limitRate = 0.9f;
    private float releaseRate = 0.6f;
    /**
     * 流控指标线
     */
    private int flowLimitLine = (int) (queueSize * limitRate);
    /**
     * 释放流控指标线
     */
    private int releaseLine = (int) (queueSize * releaseRate);
    /**
     * 是否启用控制台banner
     */
    private boolean bannerEnabled = true;

    private long clinet_break_reconnect_period_ms=5000;

    public AioServerConfig(boolean isServer) {
        this.isServer = isServer;
    }

    public void setWriteQueueSize(int queueSize) {
        this.queueSize = queueSize;
        flowLimitLine = (int) (queueSize * limitRate);
        releaseLine = (int) (queueSize * releaseRate);
    }


    public void setProtocol(Protocol<T> protocol) {
        this.protocol = protocol;
    }


    public final void setProcessor(AioProcessor<T> processor) {
        this.processor = processor;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getDirctBufferSize() {
        return dirctBufferSize;
    }

    public AioProcessor<T> getProcessor() {
        return processor;
    }

    public Protocol<T> getProtocol() {
        return protocol;
    }

    public int getFlowLimitLine() {
        return flowLimitLine;
    }

    public int getReleaseLine() {
        return releaseLine;
    }

    public boolean isServer() {
        return isServer;
    }

    public String getHost() {
        return host;
    }

    public AioServerConfig<T> setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public AioServerConfig<T> setPort(int port) {
        this.port = port;
        return this;
    }
    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public int getServerThreadNum() {
        return serverThreadNum;
    }

    public List<SocketOption> getSocketOptions() {
        return socketOptions;
    }

    public AioServerConfig<T> setServerThreadNum(int serverThreadNum) {
        this.serverThreadNum = serverThreadNum;
        return this;
    }

    public AioServerConfig<T> setDirctBufferSize(int dirctBufferSize) {
        this.dirctBufferSize = dirctBufferSize;
        return this;
    }

    public AioServerConfig<T> setBannerEnabled(boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
        return this;
    }

    public void setBreakReconnect(long periodMs) {
        this.clinet_break_reconnect_period_ms = periodMs;
    }
    public long getBreakReconnect() {
        return this.clinet_break_reconnect_period_ms;
    }


    public void setSocketOptions(SocketOption option) {
        this.socketOptions.add(option);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AioServerConfig{");
        sb.append("isServer=").append(isServer);
        sb.append(", socketOptions=").append(socketOptions);
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", queueSize=").append(queueSize);
        sb.append(", dirctBufferSize=").append(dirctBufferSize);
        sb.append(", serverThreadNum=").append(serverThreadNum);
        sb.append(", limitRate=").append(limitRate);
        sb.append(", releaseRate=").append(releaseRate);
        sb.append(", flowLimitLine=").append(flowLimitLine);
        sb.append(", releaseLine=").append(releaseLine);
        sb.append(", bannerEnabled=").append(bannerEnabled);
        sb.append('}');
        return sb.toString();
    }
}
