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

    public AioSSLMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
        this.addPlugin(sslPlugin);
    }

    @Override
    public AioPipe start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();

        //连接成功则构造AIOSession对象
        aioPipe = new AioPipe(socketChannel, config);
        aioPipe.initSession();
        //
        if (!aioPipe.IS_HANDSHAKE) {//发送握手信号
            aioPipe.write(sslPlugin.clientReqAuth());
        }

        return aioPipe;
    }

}
