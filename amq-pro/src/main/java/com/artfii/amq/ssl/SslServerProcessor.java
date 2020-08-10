package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2020/7/27.
 */
public class SslServerProcessor extends AioBaseProcessor<BaseMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SslServerProcessor.class);

    private static SslPlugin sslPlugin = SslPlugin.build();

    @Override
    public void process0(AioPipe<BaseMessage> pipe, BaseMessage msg) {
        logger.info("服务端收到信息:"+ msg.toString());
        if(!pipe.IS_HANDSHAKE){ // 客户端启动时发送了认证信息,这里对认证信息进行核对
            BaseMessage.Head head = msg.getHead();
            if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_REQ == head.getKind()) {
                if(sslPlugin.serverCheckAuth(msg)){
                    SslPlugin.Auth auth = SslPlugin.Auth.serverAuthSucc();
                    logger.warn("server aec pwd: " + auth);
                    pipe.IS_HANDSHAKE = true;
                    pipe.MSG_CHIPER = auth.getCipher();
                    pipe.write(sslPlugin.serverRspAuthResult(auth.getMsg()));
                }else {
                    pipe.close();
                }
            }
        }
        return;
    }

    @Override
    public void stateEvent0(AioPipe<BaseMessage> pipe, State state, Throwable throwable) {

    }
}
