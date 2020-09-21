package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.BaseMsgType;
import com.artfii.amq.core.aio.State;
import com.artfii.amq.core.aio.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : 客户端SSL插件,处理握手信息
 * 注意:要保证本插件在插件队列里优先处理
 *
 * @author: leeton on 2020/9/15.
 */
public class SslClientPlugin extends SslBasePlugin implements Plugin<BaseMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SslClientPlugin.class);

    private static SslBasePlugin sslBasePlugin = SslBasePlugin.build();

    @Override
    public boolean preProcess(AioPipe<BaseMessage> pipe, BaseMessage message) {
        return process(pipe, message);
    }

    @Override
    public void stateEvent(State State, AioPipe<BaseMessage> pipe, Throwable throwable) {

    }

    private boolean process(AioPipe pipe, BaseMessage message) {
        logger.info("客户端收到信息:" + message.toString());
        if (!pipe.SSL_HANDSHAKE_SUCC) { // 客户端启动时发送了认证信息,这里对对认证结果进行判断
            BaseMessage.Head head = message.getHead();
            if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_RSP == head.getKind()) {
                SslBasePlugin.Auth auth = sslBasePlugin.clientReadAuthResult(message);//服务端的认证结果
                if (SslBasePlugin.Auth.isAuthSucc(auth.getFlag())) {//认证成功
                    pipe.SSL_HANDSHAKE_SUCC = true;
                    pipe.SSL_CHIPER = auth.getCipher();
                    logger.warn("[AMQ]: client ssl handshake auth SUCC.");
                    return true;
                } else {
                    logger.warn("[AMQ]: client ssl handshake auth FAIL.");
                    pipe.close();
                    return false;
                }
            }
        }
        return true;
    }
}
