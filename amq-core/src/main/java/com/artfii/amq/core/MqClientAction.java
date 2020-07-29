package com.artfii.amq.core;

import com.artfii.amq.core.aio.Call;

/**
 * FUNC: MqClientAction
 * Created by leeton on 2019/1/15.
 */
public interface MqClientAction {

    /**
     * 普通的发布消息,适合大批量发送的场景
     *
     * @param topic
     * @param v     传值
     * @param <V>
     * @return
     */
    <V> boolean publish(String topic, V v);

    <V> boolean publish(String topic, V v,Message.Life life);

    /**
     * 普通的订阅消息(默认存活周期为 {@link Message.Life#FOREVER} )
     *
     * @param topic
     * @param callBack
     */
    <V> void subscribe(String topic, Call<V> callBack);

    /**
     * 普通的订阅消息
     *
     * @param topic
     * @param life     订阅的生命周期
     * @param callBack
     */
    <V> void subscribe(String topic, Message.Life life, Call<V> callBack);

    /**
     * 普通的订阅消息,带传值
     *
     * @param topic
     * @param v        通常做为附带的条件
     * @param life     订阅的生命周期
     * @param callBack
     */
    <V> void subscribe(String topic, V v, Message.Life life, Call<V> callBack);

    /**
     * 发布一个工作任务,类似 RPC 调用
     *
     * @param topic
     * @param v     传值
     * @param <V>
     * @return
     */
    <V> Message publishJob(String topic, V v);

    /**
     * 接受一个工作任务
     *
     * @param topic
     * @param acceptJobThenExecute 回调方法
     * @param <V>
     */
    <V> void acceptJob(String topic, Call<V> acceptJobThenExecute);

    /**
     * 反馈任务执行结果
     *
     * @param topic
     * @param v           执行结果
     * @return
     */
    <V> boolean finishJob(String topic, V v);

    /**
     * 确认收到消息(签收)
     *
     * @param messageId
     * @return
     */
    boolean ack(String messageId);

}
