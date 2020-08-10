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
    public void process0(AioPipe pipe, BaseMessage msg) {
        logger.info("客户端收到信息:"+ msg.toString());
        if(!pipe.IS_HANDSHAKE){ // 客户端启动时发送了认证信息,这里对对认证结果进行判断
            BaseMessage.Head head = msg.getHead();
            if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_RSP == head.getKind()) {
                String rece = sslPlugin.clientReceMsg(msg);//服务端的认证结果
                SslPlugin.Auth auth = SslPlugin.Auth.decodeAuthResult(rece);
                if(SslPlugin.Auth.isAuthSucc(auth.getFlag())){//认证成功
                    pipe.IS_HANDSHAKE = true;
                    pipe.MSG_CHIPER = auth.getCipher();
                    logger.warn("[AMQ]: client auth SUCC.");
                }else {
                    pipe.close();
                    logger.warn("[AMQ]: client auth FAIL.");
                }
            }
        }
        return;
    }


    @Override
    public void stateEvent0(AioPipe pipe, State state, Throwable throwable) {

    }
}
