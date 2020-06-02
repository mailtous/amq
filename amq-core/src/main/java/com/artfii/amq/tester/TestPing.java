package com.artfii.amq.tester;

import com.artfii.amq.core.*;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/3/1.
 */
public class TestPing {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        final int threadSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new MqProtocol(), processor);
        client.start(channelGroup);
        //
        Message message = processor.publishJob("topic_get_userById",2);
        System.err.println(message);





    }

}
