/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSSLQuickClient.java
 * Date: 2018-02-04
 * Author: sandao
 */


package com.artfii.amq.transport;

import com.artfii.amq.core.aio.AioClient;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioProcessor;
import com.artfii.amq.core.aio.Protocol;
import com.artfii.amq.core.aio.plugin.Plugin;
import com.artfii.amq.ssl.SSLConfig;
import com.artfii.amq.ssl.SSLService;
import com.artfii.amq.ssl.SslPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * AIO实现的客户端服务
 * Created by 三刀 on 2017/6/28.
 */
public class AioSSLQuickClient<T> extends AioClient<T> {
    private SSLService sslService;

    private SSLConfig sslConfig = new SSLConfig();

    /**
     * @param host             远程服务器地址
     * @param port             远程服务器端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioSSLQuickClient(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(host, port, protocol, messageProcessor);
    }

    /**
     * @param asynchronousChannelGroup
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        //配置SSL证书
        sslConfig.setClientMode(true);
        //setKeyStore(MqConfig.inst.amq_client_jks_file,MqConfig.inst.amq_client_jks_pwd);
       // setTrust(MqConfig.inst.amq_client_trust_file, MqConfig.inst.amq_client_trust_pwd);
     //   sslService = new SSLService(true,ClientAuth.NONE);
        //启动SSL服务
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();

        // 从插件里取得配置好的 sslService
        Set<Plugin> pluginSet = config.getProcessor().getPlugins();
        for (Plugin plugin : pluginSet) {
            if (plugin instanceof SslPlugin) {
                this.sslService = ((SslPlugin) plugin).getSslService();
            }
        }
        //连接成功则构造AIOSession对象
        pipe = new SSLAioSession<T>(socketChannel, config, sslService);
        pipe.initSession();
        return pipe;
    }


    public AioSSLQuickClient<T> setKeyStore(String keyStoreFile, String keystorePassword) {
        sslConfig.setKeystoreFile(keyStoreFile);
        sslConfig.setKeystorePassword(keystorePassword);
        return this;
    }

    public AioSSLQuickClient<T> setTrust(String trustFile, String trustPassword) {
        sslConfig.setTrustFile(trustFile);
        sslConfig.setTrustPassword(trustPassword);
        return this;
    }


}
