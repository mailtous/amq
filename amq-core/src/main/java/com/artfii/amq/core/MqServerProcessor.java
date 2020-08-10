package com.artfii.amq.core;

import com.artfii.amq.core.aio.AioBaseProcessor;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.State;
import org.osgl.util.C;
import org.osgl.util.S;
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
        if(BaseMessage.isReConnectReq(baseMessage.getHead())){
            byte[] includeByte = baseMessage.getHead().getSlot();
            String includeStr = new String(includeByte).trim();
            C.List<String> pipeIds = S.split(includeStr, ",");
            Integer oldPipeId = Integer.valueOf(null==pipeIds.get(0)?"0":pipeIds.get(0));
            Integer newPipeId =Integer.valueOf(null==pipeIds.get(1)?"0":pipeIds.get(1));
            if (oldPipeId>0 && newPipeId >0) {
                logger.debug(" Request to replace pipeid :{}-{}",oldPipeId,newPipeId);
                ProcessorImpl.INST.replacePipeIdOnReconnect(oldPipeId, newPipeId);
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
