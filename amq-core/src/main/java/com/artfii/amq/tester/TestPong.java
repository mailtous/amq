package com.artfii.amq.tester;

import com.artfii.amq.core.AioMqClient;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqClientProcessor;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioProtocol;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/3/1.
 */
public class TestPong {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        final int threadSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new AioProtocol(), processor);
        client.start(channelGroup);
        //
        TestUser user = new TestUser(2, "alice");
        String jobTopc = "topic_get_userById";
        processor.acceptJob(jobTopc, (Message job)->{
            if (job != null) {
                System.err.println(job);// 收到的 JOB
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    processor.<TestUser>pongJob(jobTopc, user);
                }
            }
        });




    }


}
