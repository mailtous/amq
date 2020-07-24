package com.artfii.amq.core.aio.plugin;

import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.State;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface Plugin<T> {

    /**
     * 对请求消息进行预处理，并决策是否进行后续的MessageProcessor处理。
     * 若返回false，则当前消息将被忽略。
     * 若返回true，该消息会正常秩序MessageProcessor.process.
     * @param pipe 通道
     * @param message 收到的消息
     * @return
     */
    boolean preProcess(AioPipe<T> pipe, T message);

    public void stateEvent(State State, AioPipe<T> pipe, Throwable throwable);

}
