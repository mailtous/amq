package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioClient;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.Protocol;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class AioMqClient<T> extends AioClient<T> {

    private AioPipe aioPipe;

    public AioMqClient(String host, Integer port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
    }

    public AioMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
    }

    @Override
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        aioPipe = super.start(asynchronousChannelGroup);
        return aioPipe;
    }

}
