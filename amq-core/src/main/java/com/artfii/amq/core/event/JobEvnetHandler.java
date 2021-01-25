package com.artfii.amq.core.event;

import com.artfii.amq.disruptor.EventHandler;
import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.ProcessorImpl;
import com.artfii.amq.disruptor.EventHandler;
import com.artfii.amq.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Func : 消息任务的分派
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvnetHandler implements EventHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(JobEvnetHandler.class);
    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        logger.debug("执行 JobEvnetHandler:" + event.getMessage());
        ProcessorImpl.INST.onMessage(event.getPipe(), event.getMessage());
    }


}
