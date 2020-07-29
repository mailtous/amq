package com.artfii.amq.core.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Func : Aio 协议
 *
 * @author: leeton on 2019/2/22.
 */
public class AioProtocol implements Protocol<BaseMessage> {
    private static final Logger logger = LoggerFactory.getLogger(AioProtocol.class);

    @Override
    public ByteBuffer encode(BaseMessage baseMessage) {
        return BaseMessage.encode(baseMessage);
    }

    @Override
    public BaseMessage decode(ByteBuffer readBuffer) {
        try {
           return BaseMessage.decode(readBuffer);
        } catch (Exception e) {
            logger.error("[AIO]处理消息出错: " + e.getMessage(), e);
            return null;
        }
    }


}
