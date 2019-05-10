package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.tools.FastList;

import java.util.List;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/18.
 */
public interface Processor {

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
    void publishJobToWorker(Message message);

    /**
     * 按 TOPIC 前缀式匹配消息
     * @param topic
     * @return
     */
    FastList<Subscribe> subscribeMatchOfTopic(String topic);

    /**
     * 消息发送给订阅方
     * @param message
     * @param subscribeList 订阅列表
     */
    void sendMessageToSubcribe(Message message, List<Subscribe> subscribeList);

    void shutdown();

}
