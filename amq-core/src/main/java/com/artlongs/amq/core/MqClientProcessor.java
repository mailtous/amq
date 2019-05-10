package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor extends AioBaseProcessor<Message> implements MqClientAction {
    private static Logger logger = LoggerFactory.getLogger(MqClientProcessor.class);

    private AioPipe<Message> pipe;
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>(); //客户端返回的消息包装
    private static Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>(); //客户端返回的消息包装(仅一次)
    //

    @Override
    public void process0(AioPipe<Message> pipe, Message message) {
        prcessMsg(message);
    }

    private void prcessMsg(Message message) {
        try {
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
            removeFutureResultMap(result.getSubscribeId());
            if (MqConfig.inst.mq_auto_acked) {
                ack(result.getSubscribeId());
            }
        }

        return result;
    }

    @Override
    public <V> void acceptJob(String topic, Call<V> acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    public <V> boolean finishJob(String topic, V v, String acceptJobId) {
        try {
            Message<Message.Key, V> finishJob = Message.buildFinishJob(acceptJobId, topic, v, getNode());
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

    private boolean write(Message obj) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            return this.pipe.write(obj);
        });
        Boolean send = future.join();

        return send;
    }

    @Override
    public void stateEvent0(AioPipe<Message> pipe, State state, Throwable throwable) {
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

    private void removeCallbackMap(String key) {
        callBackMap.remove(key);
    }

    private Integer getNode() {
        return this.pipe.getId();
    }


}
