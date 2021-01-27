package com.artfii.amq.core;

import com.artfii.amq.core.aio.*;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func : 客户端信息处理中心
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor extends AioBaseProcessor<BaseMessage> implements MqAction,Serializable {
    private static Logger logger = LoggerFactory.getLogger(MqClientProcessor.class);

    private AioPipe<BaseMessage> pipe;
    private AioClient aioClient;
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>(); //客户端返回的消息包装
    private static Map<String, Method> callBackMethodMap = new ConcurrentHashMap<>(); //客户端返回的消息包装
    private static Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>(); //客户端返回的消息包装(仅一次)
    //
    private static String firstPipeId = "";

    @Override
    public void process0(AioPipe<BaseMessage> pipe, BaseMessage message) {
        this.aioClient = pipe.getAioClient();
        if(BaseMsgType.RE_CONNECT_RSP == message.getHead().getKind()){ //服务端-->断线重连
            String pipeId = new String(message.getHead().getSlot()).trim();
            if (firstPipeId == "") { //第一次,保存最初的pipeID
                logger.warn("服务端-->最初的pipeID:{}",pipeId);
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
                    autoAckOfSubribe(message);
                    Call call = callBackMap.get(subscribeId);
                    if (null != call) {
                        call.back(message);
                    }
                }
                if (C.notEmpty(futureResultMap) && null != futureResultMap.get(subscribeId)) {
                    futureResultMap.get(subscribeId).complete(message);
                }
            }
        } catch (Exception e) {
            logger.error("[C] Client prcess decode exception: ", e);
        }
    }
    /**
     * 发送更换 PIPEID 的消息
     * @param aioPipe
     */
    private void sendReplacePipeId(AioPipe aioPipe, String oldPipeId,String newPipeid) {
        logger.warn("服务器重启过了,更换PIPEID: {} -> {} ",oldPipeId,newPipeid);
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
        Message subscribe = Message.buildSubscribe(topic, v, getNode(), life, Message.Listen.CALLBACK);
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), callBack);
    }

    @Override
    public <V> void acceptJob(String topic, Call<V> acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    @Override
    public <V> Message publishJob(String topic, V v) {
        Message job = Message.buildPublishJob(topic, v, getNode());
        String jobId = job.getK().getId();
        // 发布一个 future ,等任务完成后读取结果.
        futureResultMap.put(jobId, new CompletableFuture<Message>());
        write(job);
        Message result = futureResultMap.get(jobId).join();
        if (null != result) {
            if (MqConfig.inst.mq_auto_acked) {
                ack(result.getSubscribeId());
            }
            removeFutureResultMap(result.getSubscribeId());
        }

        return result;
    }

    public <V> boolean finishJob(String topic, V v) {
        try {
            Message<Message.Key, V> finishJob = Message.buildFinishJob(topic, v, getNode());
            return write(finishJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean ack(String messageId) {
        Message message = Message.buildAck(messageId, Message.Life.SPARK);
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
            ack(message.getSubscribeId());
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
            logger.warn("[C]消息处理,状态:{}, EX:{}", state.toString(), throwable);
        }
    }

    private void removeFutureResultMap(String key) {
        futureResultMap.remove(key);
    }

    private Integer getNode() {
        if(null != this.pipe){
            return this.pipe.getId();
        }
        return -1;
    }

}
