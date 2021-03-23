package com.artfii.amq.core;

import com.artfii.amq.core.aio.*;
import com.artfii.amq.tools.MqLogger;
import org.osgl.util.C;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func : 客户端信息处理中心
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor extends AioBaseProcessor<BaseMessage> implements MqAction,Serializable {
    private static MqLogger mqLogger = MqLogger.build(MqClientProcessor.class);

    private AioPipe<BaseMessage> pipe;
    private AioClient aioClient;
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>(); //客户端返回的消息包装（可多次）
    private static Map<String, CompletableFuture<Message>> onceFutureResultMap = new ConcurrentHashMap<>(); //客户端返回的消息包装(仅一次)
    //
    private static String firstPipeId = "";

    @Override
    public void process0(AioPipe<BaseMessage> pipe, BaseMessage message) {
        this.aioClient = pipe.getAioClient();
        if(BaseMsgType.RE_CONNECT_RSP == message.getHead().getKind()){ //服务端-->断线重连
            String pipeId = new String(message.getHead().getSlot()).trim();
            if (firstPipeId == "") { //第一次,保存最初的pipeID
                mqLogger.debug("服务端-->最初的pipeID:{}",pipeId);
                firstPipeId = pipeId;
            }else { // 第二次以上说明是服务器重启了,需要更换 pipeId
                sendReplacePipeId(pipe, firstPipeId, pipeId);
                firstPipeId = pipeId;
            }
        }else {
            prcessMsg(message);
        }
    }

    private void prcessMsg(BaseMessage baseMessage) {
        try {
            Message message = baseMessage.getBody();
            if (null != message) {
                String subscribeId = message.getSubscribeId();
                if (C.notEmpty(callBackMap) && null != callBackMap.get(subscribeId)) {
                    //自动签收
                    autoAckOfSubribe(message);
                    Call call = callBackMap.get(subscribeId);
                    if (null != call) {
                        call.back(message);
                    }
                }
                if (C.notEmpty(onceFutureResultMap) && null != onceFutureResultMap.get(subscribeId)) {
                    onceFutureResultMap.get(subscribeId).complete(message);
                }
            }
        } catch (Exception e) {
            mqLogger.error("[C] Client prcess decode exception: ", e);
        }
    }
    /**
     * 发送更换 PIPEID 的消息
     * @param aioPipe
     */
    private void sendReplacePipeId(AioPipe aioPipe, String oldPipeId,String newPipeid) {
        mqLogger.warn("服务器重启过了,更换PIPEID: {} -> {} ",oldPipeId,newPipeid);
        byte[] includeInfo = (oldPipeId+","+newPipeid).getBytes();
        BaseMessage baseMessage = BaseMessage.ofHead(BaseMsgType.RE_CONNECT_REQ, includeInfo);
        aioPipe.write(baseMessage);
    }

    @Override
    public <M> boolean publish(String topic, M data) {
        try {
            Message message = Message.buildCommonMessage(topic, data, getNode());
            return write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public <M> boolean publish(String topic, M data,Message.Life life) {
        try {
            Message message = Message.buildCommonMessage(topic, data, getNode());
            message.setLife(life);
            return write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <V> void subscribe(String topic, Call<V> callBack) {
        subscribe(topic, Message.Life.FOREVER, callBack);
    }

    @Override
    public <V> void subscribe(String topic, Message.Life life, Call<V> callBack) {
        subscribe(topic, null, life, callBack);
    }

    @Override
    public <V> void subscribe(String topic, V v, Message.Life life, Call<V> callBack) {
        if (MqConfig.inst.isMatchLocalTopic(topic)) {
            Message subscribe = Message.buildSubscribe(topic, v, getNode(), life, Message.Listen.CALLBACK);
            write(subscribe);
            callBackMap.put(subscribe.getSubscribeId(), callBack);
        }
    }

    @Override
    public <V> void acceptJob(String topic, Call<V> acceptJobThenExecute) {
        if (MqConfig.inst.isMatchLocalTopic(topic)) {
            Message subscribe = Message.buildAcceptJob(topic, getNode());
            write(subscribe);
            callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
        }
    }

    @Override
    public <V> Message pingJob(String topic, V v) {
        Message job = Message.buildPingJob(topic, v, getNode());
        String jobId = job.getK().getId();
        // 发布一个 future ,等任务完成后读取结果.
        onceFutureResultMap.put(jobId, new CompletableFuture<Message>());
        write(job);
        Message result = onceFutureResultMap.get(jobId).join();
        if (null != result) {
            ofEndJob(jobId);
            removeFutureResultMap(result.getSubscribeId());
        }

        return result;
    }

    public <V> boolean pongJob(String topic, V v) {
        try {
            Message<Message.Key, V> finishJob = Message.buildPongJob(topic, v, getNode());
            return write(finishJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean ack(String messageId) {
        Message message = Message.buildAck(messageId);
        return write(message);
    }

    /**
     * 工作任务完成，执行清理
     * @param msgIdOfEnd
     * @return
     */
    private boolean ofEndJob(String msgIdOfEnd) {
        Message message = Message.buildOfEndJob(msgIdOfEnd);
        return write(message);
    }

    /**
     * 非永久型订阅,则自动签收.
     * 所有订阅都已签收的消息会被注销,以避免不停的接收重复的消息.
     *
     * @param message
     */
    private void autoAckOfSubribe(Message message) {
        if (Message.Life.FOREVER != message.getLife()) {
            ack(message.getK().getId());
        }
    }

    private boolean write(Message message) {
        if (null == pipe || this.pipe.isClose()) {
            this.pipe = this.aioClient.reConnect();
        }
        BaseMessage baseMessage = BaseMessage.ofAll(BaseMsgType.BYTE_ARRAY_MESSAGE_REQ, null, message);
        return this.pipe.write(baseMessage);
    }

    @Override
    public void stateEvent0(AioPipe<BaseMessage> pipe, State state, Throwable throwable) {
        switch (state) {
            case NEW_PIPE:
                this.pipe = pipe;
                break;
        }
        if (null != throwable) {
            throwable.printStackTrace();
        }
        if (State.NEW_PIPE != state) {
            mqLogger.warn("[C]消息处理,状态:{}, EX:{}", state.toString(), throwable);
        }
    }

    private void removeFutureResultMap(String key) {
        onceFutureResultMap.remove(key);
    }

    private Integer getNode() {
        if(null != this.pipe){
            return this.pipe.getId();
        }
        return -1;
    }

}
