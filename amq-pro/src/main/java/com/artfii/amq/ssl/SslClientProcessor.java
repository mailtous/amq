package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.AioBaseProcessor;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.State;

/**
 * Func :
 *
 * @author: leeton on 2020/7/28.
 */
public class SslClientProcessor extends AioBaseProcessor<BaseMessage> {

    private static SslPlugin sslPlugin = SslPlugin.build();

    @Override
    public void process0(AioPipe pipe, BaseMessage msg) {
        if(!pipe.IS_HANDSHAKE){ // 客户端启动时发送了认证信息,这里对对认证结果进行判断
            String rece = sslPlugin.clientReceMsg(msg);//服务端的认证结果
            if(!sslPlugin.isAuthSucc(rece)){//认证失败
                pipe.close();
                throw new RuntimeException("client auth fail.");
            }
        }
        return;
    }


    @Override
    public void stateEvent0(AioPipe pipe, State state, Throwable throwable) {

    }
}
