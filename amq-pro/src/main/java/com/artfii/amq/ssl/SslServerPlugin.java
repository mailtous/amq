package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.BaseMsgType;
import com.artfii.amq.core.aio.State;
import com.artfii.amq.core.aio.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : SSL 服务端插件,主要是处理握手信息
 * 注意:要保证本插件在插件队列里优先处理
 * @author: leeton on 2020/9/15.
 */
public class SslServerPlugin extends SslBasePlugin implements Plugin<BaseMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SslServerPlugin.class);

    private static SslBasePlugin sslBasePlugin = SslBasePlugin.build();

    @Override
    public boolean preProcess(AioPipe<BaseMessage> pipe, BaseMessage message) {
        return process(pipe, message);
    }

    @Override
    public void stateEvent(State State, AioPipe<BaseMessage> pipe, Throwable throwable) {

    }

    private boolean process(AioPipe<BaseMessage> pipe, BaseMessage msg) {
        logger.info("服务端收到信息:"+ msg.toString());
        if(!pipe.SSL_HANDSHAKE_SUCC){ // 收到客户端的握手信息,执行核对
            BaseMessage.Head head = msg.getHead();
            if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_REQ == head.getKind()) {
                if(sslBasePlugin.serverCheckAuth(msg)){//握手成功
                    SslBasePlugin.Auth auth = SslBasePlugin.Auth.buildServerAuthSucc();
                    logger.warn("client ssl handshake auth SUCC , send connet pwd to client.");
                    //握手成功,返回通讯密钥
                    pipe.SSL_HANDSHAKE_SUCC = true;
                    pipe.SSL_CHIPER = auth.getCipher();
                    pipe.write(sslBasePlugin.serverRspAuthResult(auth.getMsg()));
                    return true;
                }else {
                    logger.warn("client ssl handshake auth FAIL , CLOSE PIPE.");
                    pipe.close();
                    return false;
                }
            }
        }
        return true;
    }

}
