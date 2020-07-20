/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioSSLQuickServer.java
 * Date: 2018-02-04
 * Author: sandao
 */

package com.artfii.amq.transport;

import com.artfii.amq.core.aio.*;
import com.artfii.amq.core.aio.plugin.Plugin;
import com.artfii.amq.ssl.ClientAuth;
import com.artfii.amq.ssl.SSLConfig;
import com.artfii.amq.ssl.SSLService;
import com.artfii.amq.ssl.SslPlugin;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Set;
import java.util.function.Function;

/**
 * AIO服务端
 * Created by 三刀 on 2017/6/28.
 */
public class AioSSLQuickServer<T> extends AioServer<T> {
    private SSLConfig sslConfig = new SSLConfig();

    private SSLService sslService;


    /**
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioSSLQuickServer(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(host,port, protocol, messageProcessor);
    }

    /**
     * 打印banner
     *
     * @param out
     */
    private static void printBanner(PrintStream out) {
        out.println(AioServerConfig.BANNER);
        out.println(" :: AMQ (tls/ssl) ::\t(" + AioServerConfig.VERSION + ")");
    }

    @Override
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            printBanner(System.out);
        }
        Set<Plugin> pluginSet = config.getProcessor().getPlugins();
        for (Plugin plugin : pluginSet) {
            if (plugin instanceof SslPlugin) {
                this.sslService = ((SslPlugin) plugin).getSslService();
            }
        }
        //启动SSL服务
        start0(new Function<AsynchronousSocketChannel, AioPipe<T>>() {
            @Override
            public AioPipe<T> apply(AsynchronousSocketChannel channel) {
                return new SSLAioSession<T>(channel, config,sslService);
            }
        });
    }

    public AioSSLQuickServer<T> setKeyStore(String keyStoreFile, String keystorePassword) {
        sslConfig.setKeystoreFile(keyStoreFile);
        sslConfig.setKeystorePassword(keystorePassword);
        return this;
    }

    public AioSSLQuickServer<T> setTrust(String trustFile, String trustPassword) {
        sslConfig.setTrustFile(trustFile);
        sslConfig.setTrustPassword(trustPassword);
        return this;
    }

    public AioSSLQuickServer<T> setClientAuth(ClientAuth clientAuth) {
        sslConfig.setClientAuth(clientAuth);
        return this;
    }

    public void setSslService(SSLService sslService) {
        this.sslService = sslService;
    }
}
