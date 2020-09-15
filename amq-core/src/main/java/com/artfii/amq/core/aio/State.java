package com.artfii.amq.core.aio;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public enum State {
    /**
     * 连接已建立并构建 pipe 对象
     */
    NEW_PIPE,
    /**
     * 读通道已被关闭。
     */
    INPUT_SHUTDOWN,
    /**
     * 业务处理异常。
     */
    PROCESS_EXCEPTION,

    /**
     * 协议解码异常。
     * <p>执行{@link Protocol#decode(ByteBuffer)}期间发生未捕获的异常。</p>
     */
    DECODE_EXCEPTION,
    /**
     * 读操作异常。
     *
     * <p>在底层服务执行read操作期间因发生异常情况出发了{@link java.nio.channels.CompletionHandler#failed(Throwable, Object)}。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    INPUT_EXCEPTION,
    /**
     * 写操作异常。
     * <p>在底层服务执行write操作期间因发生异常情况出发了{@link java.nio.channels.CompletionHandler#failed(Throwable, Object)}。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    OUTPUT_EXCEPTION,
    /**
     * 会话正在关闭中。
     *
     */
    PIPE_CLOSING,
    /**
     * 会话关闭成功。
     *
     * <p> AIO-PIPE 关闭成功</p>
     */
    PIPE_CLOSED,
    /**
     * 流控,仅服务端有效。
     *
     * <p>服务端启用了输出缓存队列，且消息积压达到一定阈值时触发流控。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    FLOW_LIMIT,
    /**
     * 释放流控,仅服务端有效。
     *
     * <p>处于流控状态下的服务端，当输出队列积压量下降到安全阈值后，释放流控状态。</p>
     * <b>未来该状态机可能会废除，并转移至NetMonitor</b>
     */
    RELEASE_FLOW_LIMIT,

    /**
     * 认证失败
     */
    AUTH_FAIL;

}
