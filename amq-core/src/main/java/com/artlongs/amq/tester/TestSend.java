package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestSend {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        final int groupSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(groupSize, (r) -> new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.inst.host, MqConfig.inst.port, new MqProtocol(), processor);
        client.start(channelGroup);

        runWithNums(processor,10);
//
//        runWithTimes(processor, 10);

    }

    private static void runWithNums(MqClientProcessor processor, int nums) throws InterruptedException {
        long s = System.currentTimeMillis();
        for (int i = 0; i < nums; i++) { // 测试时,最好把 aioServer.setWriteQueueSize 的大小设置为 >= 测试次数
//            Thread.sleep(2,500);
            TestUser user = new TestUser(i, "alice");
            processor.publish("topic_hello", user);
            System.err.println("send : " + user.toString());
        }
        System.err.println("Time(ms):" + (System.currentTimeMillis() - s));
    }

    private static void runWithTimes(MqClientProcessor processor, int seconds) throws InterruptedException {
        long s = System.currentTimeMillis();
        final long end = seconds * 1000;
        int i = 0;
        while (System.currentTimeMillis() - s < end) {
            Thread.sleep(0, 500);
            TestUser user = new TestUser(++i, "alice");
            processor.publish("topic_hello", user);
        }
        System.err.println("Time(ms):" + (System.currentTimeMillis() - s));
    }

}
