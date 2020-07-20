/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.artfii.amq.ssl;

import com.artfii.amq.buffer.BufferFactory;
import com.artfii.amq.buffer.BufferPagePool;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.State;
import com.artfii.amq.core.aio.plugin.Plugin;

import java.io.InputStream;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public final class SslPlugin<T> implements Plugin<T> {
    private SSLService sslService;
    private BufferPagePool bufferPagePool;
    private boolean init = false;

    public SslPlugin() {
        this.bufferPagePool = BufferFactory.DISABLED_BUFFER_FACTORY.create();
    }

    public SslPlugin(BufferPagePool bufferPagePool) {
        this.bufferPagePool = bufferPagePool;
    }


    public SslPlugin initForServer() {
        InputStream serverJksFile = this.getClass().getResourceAsStream(MqConfig.inst.amq_server_jks_file);
        initForServer(serverJksFile, MqConfig.inst.amq_server_jks_pwd, MqConfig.inst.amq_server_trust_pwd, ClientAuth.OPTIONAL);
        return this;
    }

    public SslPlugin initForClinet() {
        InputStream clientTrustFile = this.getClass().getResourceAsStream(MqConfig.inst.amq_server_jks_file);
        initForClient(clientTrustFile, MqConfig.inst.amq_client_trust_pwd);
        return this;
    }

    public void initForServer(InputStream keyStoreInputStream, String keyStorePassword, String keyPassword, ClientAuth clientAuth) {
        initCheck();
        sslService = new SSLService(false, clientAuth);
        sslService.initKeyStore(keyStoreInputStream, keyStorePassword, keyPassword);

    }

    public void initForClient(InputStream trustInputStream, String trustPassword) {
        initCheck();
        sslService = new SSLService(true, ClientAuth.NONE);
        sslService.initTrust(trustInputStream, trustPassword);
    }

    private void initCheck() {
        if (init) {
            throw new RuntimeException("plugin is already init");
        }
        init = true;
    }

    public final AsynchronousSocketChannel shouldAccept(AsynchronousSocketChannel channel) {
        return new SslAsynchronousSocketChannel(channel, sslService, bufferPagePool.allocateBufferPage());
    }

    public SSLService getSslService() {
        return sslService;
    }

    public BufferPagePool getBufferPagePool() {
        return bufferPagePool;
    }

    @Override
    public boolean preProcess(AioPipe<T> pipe, T t) {
        return false;
    }

    @Override
    public void stateEvent(State State, AioPipe<T> pipe, Throwable throwable) {

    }
}
