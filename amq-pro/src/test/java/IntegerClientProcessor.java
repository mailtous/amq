/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerClientProcessor.java
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
public class IntegerClientProcessor extends AioBaseProcessor<Integer> {

    @Override
    public void process0(AioPipe<Integer> session, Integer msg) {
        System.out.println("receive data from server：" + msg);
    }

    @Override
    public void stateEvent0(AioPipe<Integer> session, State stateMachineEnum, Throwable throwable) {
        System.out.println("other state:" + stateMachineEnum);
        if (stateMachineEnum == State.OUTPUT_EXCEPTION) {
            throwable.printStackTrace();
        }
    }

}
