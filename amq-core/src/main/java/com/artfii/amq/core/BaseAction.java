package com.artfii.amq.core;

import com.artfii.amq.core.aio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func :
 *
 * @author: leeton on 2020/9/9.
 */
public class BaseAction implements MqAction {
    private static Logger logger = LoggerFactory.getLogger(BaseAction.class);
    private AioPipe<BaseMessage> pipe;
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>(); //客户端返回的消息包装
    private static Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>(); //客户端返回的消息包装(仅一次)

    public BaseAction(AioPipe<BaseMessage> pipe) {
        this.pipe = pipe;
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

    @Override
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
        subscribe(topic, null, Message.Life.FOREVER, callBack);
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

    @Override
    public <V> void acceptJob(String topic, Call<V> acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    @Override
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
        if (this.pipe.isClose()) {
            reConnetion();
        }
        BaseMessage baseMessage = BaseMessage.ofAll(BaseMsgType.BYTE_ARRAY_MESSAGE_REQ, null, message);
        return this.pipe.write(baseMessage);
    }
    private void reConnetion(){
        this.pipe = this.pipe.reConnect();
    }

    private void removeFutureResultMap(String key) {
        futureResultMap.remove(key);
    }

    private void removeCallbackMap(String key) {
        callBackMap.remove(key);
    }

    private Integer getNode() {
        if(null != this.pipe){
            return this.pipe.getId();
        }
        return -1;
    }

}
