package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2020/7/28.
 */
public class SslClientProcessor extends AioBaseProcessor<BaseMessage> {
    private static Logger logger = LoggerFactory.getLogger(SslClientProcessor.class);

    private static SslPlugin sslPlugin = SslPlugin.build();

    @Override
    public void process0(AioPipe pipe, BaseMessage message) {
        logger.info("客户端收到信息:"+ message.toString());
        if(!pipe.SSL_HANDSHAKE_SUCC){ // 客户端启动时发送了认证信息,这里对对认证结果进行判断
            BaseMessage.Head head = message.getHead();
            if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_RSP == head.getKind()) {
                SslPlugin.Auth auth = sslPlugin.clientReadAuthResult(message);//服务端的认证结果
                if(SslPlugin.Auth.isAuthSucc(auth.getFlag())){//认证成功
                    pipe.SSL_HANDSHAKE_SUCC = true;
                    pipe.SSL_CHIPER = auth.getCipher();
                    logger.warn("[AMQ]: client auth SUCC.");
                }else {
                    logger.warn("[AMQ]: client auth FAIL.");
                    pipe.close();
                }
            }
        }
    }


    @Override
    public void stateEvent0(AioPipe pipe, State state, Throwable throwable) {

    }
}
