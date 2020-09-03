package com.artfii.amq.transport;

import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioClient;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioProcessor;
import com.artfii.amq.core.aio.Protocol;
import com.artfii.amq.ssl.SslPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class AioSSLMqClient<T> extends AioClient<T> {

    private AioPipe aioPipe;
    private static SslPlugin sslPlugin = new SslPlugin();
    private final boolean isWaiting = true;

    public AioSSLMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
        config.setSsl(true);
        this.addPlugin(sslPlugin);
    }

    @Override
    public AioPipe start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();

        //连接成功则构造AIOSession对象
        aioPipe = new AioPipe(socketChannel, config,true,true);
        //发送握手消息
        boolean writed = aioPipe.write(sslPlugin.clientReqAuthInfo());
        //检查是否握手成功
        if (writed) {
            boolean isWaiting = true;
            while (isWaiting) {
                aioPipe.initSession();
                if (aioPipe.SSL_HANDSHAKE_SUCC) {
                    isWaiting = false;
                }
            }
        }
        return aioPipe;
    }

}
