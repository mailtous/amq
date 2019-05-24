package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : Mq 消息处理
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<BaseMessage> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);

    @Override
    public void process0(AioPipe<BaseMessage> pipe, BaseMessage baseMessage) {
        if(BaseMessage.isHeart(baseMessage.getHead())){
          if(pipe.isClose()){
              pipe.reConnetion();
          }
        }else {
            directSend(pipe, baseMessage.getBody());
        }
    }
    private void directSend(AioPipe pipe, Message message) {
        logger.debug("[AIO] direct send buffer to MQ");
        ProcessorImpl.INST.onMessage(pipe, message);
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


}
