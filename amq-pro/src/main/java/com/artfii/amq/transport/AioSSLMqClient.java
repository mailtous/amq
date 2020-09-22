package com.artfii.amq.transport;

import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioClient;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioProcessor;
import com.artfii.amq.core.aio.Protocol;
import com.artfii.amq.core.aio.plugin.ClientReconectTask;
import com.artfii.amq.ssl.SslClientPlugin;

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
    private static SslClientPlugin sslClientPlugin = new SslClientPlugin();

    public AioSSLMqClient(Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(MqConfig.inst.host, MqConfig.inst.port, protocol, messageProcessor);
        config.setSsl(true);
        //客户端创建时,加载 SSL-Plugin
        this.addPlugin(sslClientPlugin);
    }

    @Override
    public AioPipe start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        //创建 socketChannel,并且连接服务器
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();

        //构造 AioPipe 对象,并且初始化
        aioPipe = new AioPipe(socketChannel, config,true,true);
        aioPipe.setAioClient(this);
        //发送握手消息
        boolean writed = aioPipe.write(sslClientPlugin.clientReqAuthInfo());
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
        // 启动断链重连TASK
        ClientReconectTask.start(pipe, config.getBreakReconnectMs());
        return aioPipe;
    }

}
