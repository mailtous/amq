package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Func : Mq 消息处理
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<Message> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);
    ISerializer serializer = ISerializer.Serializer.INST.of();

    @Override
    public void process0(AioPipe<Message> pipe, Message buffer) {
//        decodeAndSend(pipe, buffer);
        directSend(pipe, buffer);
    }

    @Override
    public void stateEvent0(AioPipe session, State state, Throwable throwable) {
        if (!isSkipState(state)) {
            logger.debug("[AIO] 消息处理,状态:{}, EX:{}",state.toString(),throwable);
        }
    }

    private boolean isSkipState(State state){
        State[] skip = {State.NEW_PIPE, State.PIPE_CLOSED, State.INPUT_SHUTDOWN};
        for (State s : skip) {
            if(s.equals(state)) return true;
        }
        return false;
    }

    private void directSend(AioPipe pipe, Message message) {
        logger.debug("[AIO] direct send buffer to MQ");
        ProcessorImpl.INST.onMessage(pipe, message);
    }

    private void decodeAndSend(AioPipe pipe, ByteBuffer buffer) {
        Message message = decode(buffer);
        logger.debug("[AIO] decode and send:" + message.toString());
        ProcessorImpl.INST.onMessage(pipe, message);
    }

    private void setRead(ByteBuffer buffer) {
        buffer.position(buffer.limit());
    }

    private Message decode(ByteBuffer buffer) {
        try {
            return serializer.getObj(buffer, Message.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
