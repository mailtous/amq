/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerServerProcessor.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/


import com.artfii.amq.core.aio.AioBaseProcessor;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.State;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class IntegerServerProcessor extends AioBaseProcessor<Integer> {
    @Override
    public void process0(AioPipe<Integer> session, Integer msg) {
        Integer respMsg = msg + 1;
        System.out.println("receive data from client: " + msg + " ,rsp:" + (respMsg));
        session.write(respMsg);
    }

    @Override
    public void stateEvent0(AioPipe<Integer> session, State stateMachineEnum, Throwable throwable) {

    }
}
