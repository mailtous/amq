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
import com.artfii.amq.tools.cipher.Rsa;

import java.math.BigInteger;
import java.util.List;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public final class SslPlugin<T> implements Plugin<T> {
    private static final String hi_message = "hi-amq!";
    public static BigInteger[] PUB_KEY=null;
    public static BigInteger[] SELFT_KEY=null;
    private static Rsa rsa = null;
    private BufferPagePool bufferPagePool;
    private SSLService sslService;
    private boolean init = false;


    public SslPlugin() {
        this.bufferPagePool = BufferFactory.DISABLED_BUFFER_FACTORY.create();
        initRSA();
    }

    public SslPlugin(BufferPagePool bufferPagePool) {
        this.bufferPagePool = bufferPagePool;
    }

    private void initRSA(){
        PUB_KEY = Rsa.unFormatKey(MqConfig.inst.amq_pubkey_file);
        SELFT_KEY = Rsa.unFormatKey(MqConfig.inst.amq_selftkey_file);
        rsa = Rsa.builder().setKey(PUB_KEY, SELFT_KEY).fast().build();
    }

    /**
     * 发送握手信息
     * @return
     */
    public List<BigInteger> ping() {
        return rsa.encrypt(hi_message);
    }

    /**
     * 接收握手信息
     * @param receive
     * @return
     */
    public String pong(List<BigInteger> receive) {
        return rsa.decrypt(receive);
    }

    /**
     * 通讯认证是否成功
     * @param receive 收到的握手信息
     * @return
     */
    public boolean isAuthSucc(String receive) {
        return hi_message.equals(receive.trim());
    }


    public SSLService getSslService() {
        return sslService;
    }

    public BufferPagePool getBufferPagePool() {
        return bufferPagePool;
    }

    @Override
    public boolean preProcess(AioPipe<T> pipe, T message) {
        return isAuthSucc((String)message);
    }

    @Override
    public void stateEvent(State State, AioPipe<T> pipe, Throwable throwable) {

    }
}
