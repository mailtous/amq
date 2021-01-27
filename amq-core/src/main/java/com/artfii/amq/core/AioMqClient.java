package com.artfii.amq.core;

import com.artfii.amq.core.aio.*;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

/**
 * Func : Mq Client
 *
 * @author: leeton on 2019/2/25.
 */
public class AioMqClient<T> extends AioClient<T> implements Serializable {

    private AioPipe aioPipe;
    private MqClientProcessor processor;

    public AioMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        this(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
    }

    public AioMqClient(String host, Integer port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
        this.processor = (MqClientProcessor) messageProcessor;
    }

    @Override
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        aioPipe = super.start(asynchronousChannelGroup);
        return aioPipe;
    }



}
