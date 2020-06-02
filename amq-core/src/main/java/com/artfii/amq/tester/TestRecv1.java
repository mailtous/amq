package com.artfii.amq.tester;

import com.artfii.amq.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestRecv1 {
    private static Logger logger = LoggerFactory.getLogger(TestRecv1.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.client_connect_thread_pool_size);

        final int groupSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(groupSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new MqProtocol(), processor);
        pool.submit(client);
        client.start(channelGroup);

        Call<Message> callback = (msg)->{
            execBack(msg);
        };
        processor.subscribe("topic_hello",callback);

    }

    private static void execBack(Message message) {
        long s = System.currentTimeMillis();
        logger.debug(message.getV().toString());
//        logger.debug("Useed Time(ms):"+(s-message.getStat().getCtime()));

    }



}
