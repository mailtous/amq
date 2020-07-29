/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslDemo.java
 * Date: 2020-04-16
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/


import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioProtocol;
import com.artfii.amq.ssl.SslClientProcessor;
import com.artfii.amq.ssl.SslPlugin;
import com.artfii.amq.ssl.SslServerProcessor;
import com.artfii.amq.transport.AioSSLMqClient;
import com.artfii.amq.transport.AioSSLMqServer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslDemo {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        SslServerProcessor serverProcessor = new SslServerProcessor();
        serverProcessor.addPlugin(new SslPlugin());
        AioSSLMqServer sslQuickServer = new AioSSLMqServer("localhost" ,8080, new AioProtocol(), serverProcessor);
        sslQuickServer.start();


        final int threadSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));

        SslClientProcessor clientProcessor = new SslClientProcessor();
        clientProcessor.addPlugin(new SslPlugin());
        AioSSLMqClient sslQuickClient = new AioSSLMqClient(new AioProtocol(), clientProcessor);
        AioPipe aioSession = sslQuickClient.start(channelGroup);
        aioSession.write(123);

    }


/*    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        IntegerServerProcessor serverProcessor = new IntegerServerProcessor();
        serverProcessor.addPlugin(new SslPlugin().initForServer());
        AioSSLQuickServer sslQuickServer = new AioSSLQuickServer("localhost" ,8080, new IntegerProtocol(), serverProcessor);
        sslQuickServer.start();

        IntegerClientProcessor clientProcessor = new IntegerClientProcessor();
        clientProcessor.addPlugin(new SslPlugin().initForClinet());
        AioSSLQuickClient sslQuickClient = new AioSSLQuickClient("localhost", 8080, new IntegerProtocol(), clientProcessor);
        AioPipe aioSession = sslQuickClient.start();
        aioSession.write(123);

    }*/
}
