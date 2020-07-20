/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsAsynchronousSocketChannel.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.artfii.amq.ssl;

import com.artfii.amq.buffer.BufferPage;
import com.artfii.amq.buffer.VirtualBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2020/4/16
 */
public class SslAsynchronousSocketChannel extends AsynchronousSocketChannel {
    private static final Logger logger = LoggerFactory.getLogger(SslAsynchronousSocketChannel.class);
    private final VirtualBuffer netWriteBuffer;
    private final VirtualBuffer netReadBuffer;
    private final VirtualBuffer appReadBuffer;
    private final AsynchronousSocketChannel asynchronousSocketChannel;
    private SSLEngine sslEngine = null;
    /**
     * 完成握手置null
     */
    private HandshakeModel handshakeModel;
    /**
     * 完成握手置null
     */
    private SSLService sslService;

    private boolean handshake = true;
    /**
     * 自适应的输出长度
     */
    private int adaptiveWriteSize = -1;

    public SslAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel, SSLService sslService, BufferPage bufferPage) {
        super(null);
        this.handshakeModel = sslService.createSSLEngine(asynchronousSocketChannel, bufferPage);
        this.sslService = sslService;
        this.asynchronousSocketChannel = asynchronousSocketChannel;
        this.sslEngine = handshakeModel.getSslEngine();
        this.netWriteBuffer = handshakeModel.getNetWriteBuffer();
        this.netReadBuffer = handshakeModel.getNetReadBuffer();
        this.appReadBuffer = handshakeModel.getAppReadBuffer();
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        return asynchronousSocketChannel.bind(local);
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        return asynchronousSocketChannel.setOption(name, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return asynchronousSocketChannel.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return asynchronousSocketChannel.supportedOptions();
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        return asynchronousSocketChannel.shutdownInput();
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        return asynchronousSocketChannel.shutdownOutput();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return asynchronousSocketChannel.getRemoteAddress();
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        asynchronousSocketChannel.connect(remote, attachment, handler);
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        return asynchronousSocketChannel.connect(remote);
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (handshake) {
            handshakeModel.setHandshakeCallback(new HandshakeCallback() {
                @Override
                public void callback() {
                    handshake = false;
                    synchronized (SslAsynchronousSocketChannel.this) {
                        //释放内存
                        handshakeModel.getAppWriteBuffer().clean();
                        netReadBuffer.buffer().clear();
                        netWriteBuffer.buffer().clear();
                        appReadBuffer.buffer().clear().flip();
                        SslAsynchronousSocketChannel.this.notifyAll();
                    }
                    if (handshakeModel.isEof()) {
                        handler.completed(-1, attachment);
                    } else {
                        SslAsynchronousSocketChannel.this.read(dst, timeout, unit, attachment, handler);
                    }
                    handshakeModel = null;
                }
            });
            //触发握手
            sslService.doHandshake(handshakeModel);
            return;
        }
        ByteBuffer appBuffer = appReadBuffer.buffer();
        if (appBuffer.hasRemaining()) {
            int pos = dst.position();
            if (appBuffer.remaining() > dst.remaining()) {
                int limit = appBuffer.limit();
                appBuffer.limit(appBuffer.position() + dst.remaining());
                dst.put(appBuffer);
                appBuffer.limit(limit);
            } else {
                dst.put(appBuffer);
            }
            handler.completed(dst.position() - pos, attachment);
            return;
        }

        asynchronousSocketChannel.read(netReadBuffer.buffer(), timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                int pos = dst.position();
                ByteBuffer appBuffer = appReadBuffer.buffer();
                appBuffer.clear();
                doUnWrap();
                appBuffer.flip();
                if (appBuffer.remaining() > dst.remaining()) {
                    int limit = appBuffer.limit();
                    appBuffer.limit(appBuffer.position() + dst.remaining());
                    dst.put(appBuffer);
                    appBuffer.limit(limit);
                } else if (appBuffer.hasRemaining()) {
                    dst.put(appBuffer);
                } else if (result > 0) {
                    appBuffer.compact();
                    asynchronousSocketChannel.read(netReadBuffer.buffer(), timeout, unit, attachment, this);
                    return;
                }

                handler.completed(result != -1 ? dst.position() - pos : result, attachment);
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });
    }

    private void doUnWrap() {
        try {
            ByteBuffer netBuffer = netReadBuffer.buffer();
            ByteBuffer appBuffer = appReadBuffer.buffer();
            netBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(netBuffer, appBuffer);
            boolean closed = false;
            while (!closed && result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        logger.warn("BUFFER_OVERFLOW error");
                        break;
                    case BUFFER_UNDERFLOW:
                        if (netBuffer.limit() == netBuffer.capacity()) {
                            logger.warn("BUFFER_UNDERFLOW error");
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("BUFFER_UNDERFLOW,continue read:" + netBuffer);
                            }
                            if (netBuffer.position() > 0) {
                                netBuffer.compact();
                            } else {
                                netBuffer.position(netBuffer.limit());
                                netBuffer.limit(netBuffer.capacity());
                            }
                        }
                        return;
                    case CLOSED:
                        logger.warn("doUnWrap Result:" + result.getStatus());
                        closed = true;
                        break;
                    default:
                        logger.warn("doUnWrap Result:" + result.getStatus());
                }
                result = sslEngine.unwrap(netBuffer, appBuffer);
            }
            netBuffer.compact();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (handshake) {
            checkInitialized();
        }
        int pos = src.position();
        doWrap(src);
        asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, new CompletionHandler<Integer, A>() {
            @Override
            public void completed(Integer result, A attachment) {
                if (result == -1) {
                    System.err.println("aaaaaaaaaaa");
                }
                if (netWriteBuffer.buffer().hasRemaining()) {
                    asynchronousSocketChannel.write(netWriteBuffer.buffer(), timeout, unit, attachment, this);
                } else {
                    handler.completed(src.position() - pos, attachment);
                }
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                handler.failed(exc, attachment);
            }
        });
    }

    /**
     * 校验是否已完成初始化,如果还处于Handshake阶段则阻塞当前线程
     */
    private void checkInitialized() {
        if (!handshake) {
            return;
        }
        synchronized (this) {
            if (!handshake) {
                return;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void doWrap(ByteBuffer writeBuffer) {
        try {
            ByteBuffer netBuffer = netWriteBuffer.buffer();
            netBuffer.compact();
            int limit = writeBuffer.limit();
            if (adaptiveWriteSize > 0 && writeBuffer.remaining() > adaptiveWriteSize) {
                writeBuffer.limit(writeBuffer.position() + adaptiveWriteSize);
            }
            SSLEngineResult result = sslEngine.wrap(writeBuffer, netBuffer);
            while (result.getStatus() != SSLEngineResult.Status.OK) {
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW:
                        netBuffer.clear();
                        writeBuffer.limit(writeBuffer.position() + ((writeBuffer.limit() - writeBuffer.position() >> 1)));
                        adaptiveWriteSize = writeBuffer.remaining();
//                        logger.info("doWrap BUFFER_OVERFLOW maybeSize:{}", maybeWriteSize);
                        break;
                    case BUFFER_UNDERFLOW:
                        logger.info("doWrap BUFFER_UNDERFLOW");
                        break;
                    default:
                        logger.warn("doWrap Result:" + result.getStatus());
                }
                result = sslEngine.wrap(writeBuffer, netBuffer);
            }
            writeBuffer.limit(limit);
            netBuffer.flip();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return asynchronousSocketChannel.getLocalAddress();
    }

    @Override
    public boolean isOpen() {
        return asynchronousSocketChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        netWriteBuffer.clean();
        netReadBuffer.clean();
        appReadBuffer.clean();
        try {
            sslEngine.closeInbound();
        } catch (SSLException e) {
            logger.warn("ignore closeInbound exception: {}", e.getMessage());
        }
        sslEngine.closeOutbound();
        asynchronousSocketChannel.close();

    }
}
