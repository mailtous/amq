package com.artfii.amq.core;

import com.artfii.amq.core.aio.AioPipe;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/18.
 */
public interface MqProcessor {

    /**
     * 收到消息的逻辑处理
     * @param pipe
     * @param message
     */
    void onMessage(AioPipe<Message> pipe, Message message);

    /**
     * 把任务发送到工作线程池做进一步处理
     * @param message
     */
    void publishJobToWorker(AioPipe<Message> pipe,Message message);

    void shutdown();

}
