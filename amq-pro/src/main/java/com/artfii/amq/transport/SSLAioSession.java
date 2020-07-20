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
        this.handshakeModel = sslService.createSSLEngine(channel,bp);
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
        sslService.doHandshake(handshakeModel);
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
        doUnWrap();
        readBuffer = netReadBuffer;
        super.readFromChannel(eof);
    }

    @Override
    protected void continueWrite() {
        doWrap(writeBuffer);
        writeToChannel0(netWriteBuffer);
    }
/*
    private ByteBuffer doWrap(ByteBuffer writeBuffer) {
        int maxPacketSize = sslEngine.getSession().getPacketBufferSize();
        netWriteBuffer = ByteBuffer.allocate(maxPacketSize);
        try {
            SSLEngineResult r = sslEngine.wrap(writeBuffer, netWriteBuffer);
            netWriteBuffer.flip();
            int length = netWriteBuffer.remaining();
            System.out.println(writeBuffer + " wrapped " + length + " bytes.");
            System.out.println(writeBuffer + " handshake status is " + sslEngine.getHandshakeStatus());
            if (maxPacketSize < length && maxPacketSize != 0) {
                throw new AssertionError("Handshake wrapped net buffer length "
                        + length + " exceeds maximum packet size "
                        + maxPacketSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return netWriteBuffer;
    }*/

    private void doWrap(ByteBuffer writeBuffer) {
        try {
            netWriteBuffer.compact();
            int limit = writeBuffer.limit();
            if (adaptiveWriteSize > 0 && writeBuffer.remaining() > adaptiveWriteSize) {
                writeBuffer.limit(writeBuffer.position() + adaptiveWriteSize);
            }
            SSLEngineResult result = sslEngine.wrap(writeBuffer, netWriteBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        netWriteBuffer.clear();
                        writeBuffer.limit(writeBuffer.position() + ((writeBuffer.limit() - writeBuffer.position() >> 1)));
                        adaptiveWriteSize = writeBuffer.remaining();
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.warn("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer, netWriteBuffer);
            }
            writeBuffer.limit(limit);
            netWriteBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }


    private void doUnWrap() {
        try {
            readBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(readBuffer,netReadBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Could attempt to drain the dst buffer of any already obtained
                        // data, but we'll just increase it to the size needed.
                        int appSize = readBuffer.capacity() * 2 < sslEngine.getSession().getApplicationBufferSize() ? readBuffer.capacity() * 2 : sslEngine.getSession().getApplicationBufferSize();
                        logger.info("doUnWrap BUFFER_OVERFLOW:" + appSize);
                        ByteBuffer b = ByteBuffer.allocate(appSize + readBuffer.position());
                        readBuffer.flip();
                        b.put(readBuffer);
                        readBuffer = b;
                        // retry the operation.
                        break;
                    case BUFFER_UNDERFLOW:

//                        int netSize = readBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? readBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
//                        int netSize = sslEngine.getSession().getPacketBufferSize();

                        // Resize buffer if needed.
                        if (readBuffer.limit() == readBuffer.capacity()) {
                            int netSize = readBuffer.capacity() * 2 < sslEngine.getSession().getPacketBufferSize() ? readBuffer.capacity() * 2 : sslEngine.getSession().getPacketBufferSize();
                            logger.debug("BUFFER_UNDERFLOW:" + netSize);
                            ByteBuffer b1 = ByteBuffer.allocate(netSize);
                            b1.put(readBuffer);
                            readBuffer = b1;
                        } else {
                            if (readBuffer.position() > 0) {
                                readBuffer.compact();
                            } else {
                                readBuffer.position(readBuffer.limit());
                                readBuffer.limit(readBuffer.capacity());
                            }
                            logger.debug("BUFFER_UNDERFLOW,continue read:" + readBuffer);
                        }
                        // Obtain more inbound network data for src,
                        // then retry the operation.
//                        netReadBuffer.compact();
                        return;
                    default:
                        logger.error("doUnWrap Result:" + result.getStatus());
                        // other cases: CLOSED, OK.
                        return;
                }
                result = sslEngine.unwrap(readBuffer,netReadBuffer);
            }
            readBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(boolean immediate) {
        super.close(immediate);
        if (status == CLOSED) {
            sslEngine.closeOutbound();
        }
    }
}
