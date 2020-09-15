package com.artfii.amq.core.aio.plugin;

import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.BaseMsgType;
import com.artfii.amq.core.aio.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : 断链重连
 *
 * @author: leeton on 2019/5/27.
 */
public class ClientReconectPlugin implements Plugin {
    private static final Logger logger = LoggerFactory.getLogger(ClientReconectPlugin.class);

    private static String firstPipeId = "";

    @Override
    public boolean preProcess(AioPipe pipe, Object message) {
        BaseMessage msg = (BaseMessage) message;
        if (BaseMsgType.RE_CONNECT_RSP == msg.getHead().getKind()) { //收到服务端响应断链重连的消息
            String pipeId = new String(msg.getHead().getSlot()).trim();
            if (firstPipeId == "") { //第一次,保存最初的pipeID
                logger.warn("服务端-->最初的pipeID:{}", pipeId);
                firstPipeId = pipeId;
            } else { // 第二次以上说明是服务器重启了,需要更换 pipeId
                sendReplacePipeId(pipe, firstPipeId, pipeId);
                firstPipeId = pipeId;
            }
        }
        return true;
    }

    /**
     * 发送更换 PIPEID 的消息
     *
     * @param aioPipe
     */
    private void sendReplacePipeId(AioPipe aioPipe, String oldPipeId, String newPipeid) {
        logger.warn("服务器重启过了,更换PIPEID: {} -> {} ", oldPipeId, newPipeid);
        byte[] includeInfo = (oldPipeId + "," + newPipeid).getBytes();
        BaseMessage baseMessage = BaseMessage.ofHead(BaseMsgType.RE_CONNECT_REQ, includeInfo);
        aioPipe.write(baseMessage);
    }


    @Override
    public void stateEvent(State State, AioPipe pipe, Throwable throwable) {

    }
}
