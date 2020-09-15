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
 * Func : SSL 客户端
 *
 * @author: leeton on 2019/2/25.
 */
public class AioSSLMqClient<T> extends AioClient<T> {

    private AioPipe aioPipe;
    private static SslPlugin sslPlugin = new SslPlugin();

    public AioSSLMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
        config.setSsl(true);
        //客户端创建时,加载 SSL-Plugin
        this.addPlugin(sslPlugin);
    }

    @Override
    public AioPipe start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        //创建 socketChannel,并且连接服务器
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();

        //构造 AioPipe 对象,并且初始化
        aioPipe = new AioPipe(socketChannel, config,true,true);
        //发送握手消息
        boolean writed = aioPipe.write(sslPlugin.clientReqAuthInfo());
        //轮询检查是否握手成功
        if (writed) {
            boolean isWaiting = true;
            while (isWaiting) {
                aioPipe.startRead();//从服务端读取数据
                if (aioPipe.SSL_HANDSHAKE_SUCC) {
                    isWaiting = false;
                }
            }
        }
        return aioPipe;
    }

}
