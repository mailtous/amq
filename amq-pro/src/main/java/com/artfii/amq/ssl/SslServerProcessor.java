package com.artfii.amq.ssl;

import com.artfii.amq.core.aio.AioBaseProcessor;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.State;

/**
 * Func :
 *
 * @author: leeton on 2020/7/27.
 */
public class SslServerProcessor extends AioBaseProcessor<BaseMessage> {

    private static SslPlugin sslPlugin = SslPlugin.build();

    @Override
    public void process0(AioPipe<BaseMessage> pipe, BaseMessage msg) {
        if(!pipe.IS_HANDSHAKE){ // 客户端启动时发送了认证信息,这里对认证信息进行核对
            if(sslPlugin.serverCheckAuth(msg)){
                pipe.write(sslPlugin.serverRspAuthResult());
            }else {
                pipe.close();
                throw new RuntimeException("client auth fail.");
            }
        }
        return;
    }

    @Override
    public void stateEvent0(AioPipe<BaseMessage> pipe, State state, Throwable throwable) {

    }
}
