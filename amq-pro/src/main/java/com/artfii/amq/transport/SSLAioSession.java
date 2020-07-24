/*
 * Copyright (c) 2017, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLAioSession.java
 * Date: 2017-12-19
 * Author: sandao
 */

package com.artfii.amq.transport;

import com.artfii.amq.buffer.BufferFactory;
import com.artfii.amq.buffer.BufferPage;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioServerConfig;
import com.artfii.amq.ssl.HandshakeCallback;
import com.artfii.amq.ssl.HandshakeModel;
import com.artfii.amq.ssl.SSLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;

/**
 * @author 三刀
 * @version V1.0 , 2017/12/19
 */
public class SSLAioSession<T> extends AioPipe<T> {
    private static final Logger logger = LoggerFactory.getLogger(SSLAioSession.class);
    private ByteBuffer netWriteBuffer;

    private ByteBuffer netReadBuffer;
    private SSLEngine sslEngine = null;

    /**
     * 完成握手置null
     */
    private HandshakeModel handshakeModel;
    /**
     * 完成握手置null
     */
    private SSLService sslService;

    Semaphore readSemaphore = new Semaphore(1);

    /**
     * 自适应的输出长度
     */
    private int adaptiveWriteSize = -1;

    /**
     * @param channel
     * @param config
     * @param sslService                是否服务端Session
     */
    SSLAioSession(AsynchronousSocketChannel channel, AioServerConfig<T> config, SSLService sslService) {
        super(channel, config);
        BufferPage bp = BufferFactory.DISABLED_BUFFER_FACTORY.create().allocateBufferPage();
        this.sslService = sslService;

    }

    @Override
    public void writeToChannel() {
        checkInitialized();
        if (netWriteBuffer != null && netWriteBuffer.hasRemaining()) {
            writeToChannel0(netWriteBuffer);
            return;
        }
        super.writeToChannel();
    }


    @Override
    public void initSession() {
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        this.netWriteBuffer.flip();
        this.netReadBuffer = ByteBuffer.allocate(readBuffer.capacity());
//        this.serverFlowLimit = sslEngine.getUseClientMode() ? null : false;//服务端设置流控标志
        this.handshakeModel.setHandshakeCallback(new HandshakeCallback() {
            @Override
            public void callback() {
                synchronized (SSLAioSession.this) {
                    handshakeModel = null;//释放内存
                    SSLAioSession.this.notifyAll();
                }
                sslService = null;//释放内存
                readSemaphore.tryAcquire();
                continueRead();
            }
        });
    }

    /**
     * 校验是否已完成初始化,如果还处于Handshake阶段则阻塞当前线程
     */
    private void checkInitialized() {
        if (handshakeModel == null) {
            return;
        }
        synchronized (this) {
            if (handshakeModel == null) {
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
                logger.debug(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void continueRead() {
        readFromChannel0(readBuffer);
    }

    @Override
    public void readFromChannel(boolean eof) {
        checkInitialized();
        readBuffer = netReadBuffer;
        super.readFromChannel(eof);
    }

    @Override
    protected void continueWrite() {
        writeToChannel0(netWriteBuffer);
    }

    @Override
    public void close(boolean immediate) {
        super.close(immediate);
        if (status == CLOSED) {
            sslEngine.closeOutbound();
        }
    }
}
