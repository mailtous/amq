/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HandshakeCompletion.java
 * Date: 2018-01-02
 * Author: sandao
 */

package com.artfii.amq.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/2
 */
class HandshakeCompletion implements CompletionHandler<Integer, HandshakeModel> {
    private static final Logger logger = LoggerFactory.getLogger(HandshakeCompletion.class);
    private SSLService sslService;

    public HandshakeCompletion(SSLService sslService) {
        this.sslService = sslService;
    }

    @Override
    public void completed(Integer result, HandshakeModel attachment) {
        if (result == -1) {
            attachment.setEof(true);
        }
        synchronized (attachment) {
            sslService.doHandshake(attachment);
        }
    }

    @Override
    public void failed(Throwable exc, HandshakeModel attachment) {
        try {
            attachment.getSocketChannel().close();
            attachment.getSslEngine().closeOutbound();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.warn("handshake exception", exc);
    }
}
