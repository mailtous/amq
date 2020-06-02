package com.artfii.amq.core.event;

import com.artfii.amq.disruptor.EventHandler;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.ProcessorImpl;
import com.artfii.amq.core.Subscribe;
import com.artfii.amq.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Func : 业务事件 Handler
 *
 * @author: leeton on 2019/2/13.
 */
public class BizEventHandler implements EventHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(BizEventHandler.class);

    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        Message message = event.getMessage();
        logger.debug("[MQ]执行业务消息的匹配与发送 ......");
        String topic = message.getK().getTopic();
        List<Subscribe> subscribeList = ProcessorImpl.INST.subscribeMatchOfTopic(topic);
        ProcessorImpl.INST.sendMessageToSubcribe(message, subscribeList);
    }

}
