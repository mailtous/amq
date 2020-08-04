/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SslDemo.java
 * Date: 2020-04-16
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/


/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslDemo {



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
