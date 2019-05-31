package com.artlongs.amq.core;

import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.tools.RingBufferQueue;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Func : MQ 中心的内部定时任务
 *
 * @author: leeton on 2019/3/15.
 */
public enum MqScheduler {
    inst;
    private static Logger logger = LoggerFactory.getLogger(MqScheduler.class);
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public void start() {
        //服务器重启了,恢复之前的订阅
        resumeJobOnServerStart();
        // 计时任务
        final ScheduledFuture<?> delaySend = scheduler.scheduleWithFixedDelay(
                delaySendOnScheduled(), MqConfig.inst.msg_not_acked_resend_period, MqConfig.inst.msg_not_acked_resend_period, SECONDS);
        final ScheduledFuture<?> retrySend = scheduler.scheduleWithFixedDelay(
                retrySendOnScheduled(), MqConfig.inst.msg_falt_message_resend_period, MqConfig.inst.msg_falt_message_resend_period, SECONDS);
        final ScheduledFuture<?> checkAlive = scheduler.scheduleWithFixedDelay(
                removeExpireMsgScheduled(), MqConfig.inst.msg_default_alive_time_second, MqConfig.inst.msg_default_alive_time_second, SECONDS);

/*        TimeWheelService.instance.schedule(delaySendOnScheduled(), 5, MqConfig.msg_not_acked_resend_period, SECONDS);
        TimeWheelService.instance.schedule(retrySendOnScheduled(), 5, MqConfig.msg_falt_message_resend_period, SECONDS);
        TimeWheelService.instance.schedule(removeExpireMsgScheduled(), 1, MqConfig.msg_default_alive_time_second, SECONDS);*/
    }

    /**
     * 服务器重启了,恢复之前的订阅
     */
    private void resumeJobOnServerStart(){
        RingBufferQueue<Subscribe> cache_subscribe = ProcessorImpl.INST.getCache_subscribe();
        List<Subscribe> retryList = IStore.ofServer().getAll(IStore.server_mq_subscribe, Subscribe.class);
        for (Subscribe o : retryList) {
            cache_subscribe.putIfAbsent(o);
        }
    }


    /**
     * 客户端未确认的消息-->重发
     *
     * @return
     */
    private Runnable delaySendOnScheduled() {
        final Runnable delay = new Runnable() {
            @Override
            public void run() {
                if (MqConfig.inst.start_msg_not_acked_resend) {

                    RingBufferQueue<Subscribe> cache_subscribe = ProcessorImpl.INST.getCache_subscribe();
                    if (cache_subscribe.isEmpty()) { // 从 DB 恢复所有订阅
                        List<Subscribe> retryList = IStore.ofServer().getAll(IStore.server_mq_subscribe, Subscribe.class);
                        for (Subscribe o : retryList) {
                            cache_subscribe.putIfAbsent(o);
                        }
                    }

                    ConcurrentSkipListMap<String, Message> cache_common_publish_message = ProcessorImpl.INST.getCache_common_publish_message();
                    if (C.isEmpty(cache_common_publish_message)) { //  从 DB 恢复所有未确认的消息
                        List<Message> retryList = IStore.ofServer().getAll(IStore.server_mq_all_data, Message.class);
                        for (Message o : retryList) {
                            cache_common_publish_message.putIfAbsent(o.getK().getId(), o);
                        }
                    }

                    for (Message message : cache_common_publish_message.values()) {
                        if (MqConfig.inst.msg_not_acked_resend_max_times > message.getStat().getDelay()) {
                            logger.warn("The scheduler task is running delay-send message({})! " , message.getK().getId());
                            message.incrDelay();
                            ProcessorImpl.INST.pulishJobEvent(message);
                        }
                    }
                }
            }

        };
        return delay;
    }

    /**
     * 发送失败的消息-->重发
     *
     * @return
     */
    private Runnable retrySendOnScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                if (MqConfig.inst.start_msg_falt_message_resend) {
                    ConcurrentSkipListMap<String, Message> cache_falt_message = ProcessorImpl.INST.getCache_falt_message();
                    if (C.isEmpty(cache_falt_message)) {
                        List<Message> retryList = IStore.ofServer().getAll(IStore.server_mq_need_retry, Message.class);
                        for (Message o : retryList) {
                            cache_falt_message.putIfAbsent(o.getK().getId(), o);
                        }
                    }
                    for (Message message : cache_falt_message.values()) {
                        if (MqConfig.inst.msg_falt_message_resend_max_times > message.getStat().getRetry()) {
                            logger.warn("The scheduler task is running retry-send message ({})!",message.getK().getId());
                            message.incrRetry();
                            ProcessorImpl.INST.pulishJobEvent(message);
                        }
                    }
                }
            }
        };
        return retry;
    }

    /**
     * 删除过期的消息
     *
     * @return
     */
    private Runnable removeExpireMsgScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                ConcurrentSkipListMap<String, Message> cache_common_publish_message = ProcessorImpl.INST.getCache_common_publish_message();

                final List<Message> retryList = IStore.ofServer().getAll(IStore.server_mq_need_retry, Message.class);
                for (Message o : retryList) {
                    cache_common_publish_message.putIfAbsent(o.getK().getId(), o);
                }
                //
                final List<Message> allDataList = IStore.ofServer().getAll(IStore.server_mq_all_data, Message.class);
                for (Message o : allDataList) {
                    cache_common_publish_message.putIfAbsent(o.getK().getId(), o);
                }
                //
                final List<Message> commonPlulishList = IStore.ofServer().getAll(IStore.server_mq_common_publish, Message.class);
                for (Message o : commonPlulishList) {
                    cache_common_publish_message.putIfAbsent(o.getK().getId(), o);
                }

                for (Message message : cache_common_publish_message.values()) {
                    long cTime = message.getStat().getCtime();
                    long now = System.currentTimeMillis();
                    long ttl = message.getStat().getTtl() * 1000;
                    if ((now - cTime >= ttl) && Message.Life.FOREVER != message.getLife()) {
                        String id = message.getK().getId();
                        logger.warn("The scheduler task is running remove message({}) on TTL.", id);
                        ProcessorImpl.INST.removeDbDataOfDone(id);
                        ProcessorImpl.INST.removeCacheOfDone(id);
                    }
                }

                // 订阅方的消息--过期删除
                RingBufferQueue<Subscribe> cache_subscribe = ProcessorImpl.INST.getCache_subscribe();
                final List<Subscribe> subscribeList = IStore.ofServer().getAll(IStore.server_mq_subscribe, Subscribe.class);
                for (Subscribe o : subscribeList) {
                    cache_subscribe.putIfAbsent(o);
                }

                Iterator<Subscribe> subscribeIterator = cache_subscribe.iterator();
                while (subscribeIterator.hasNext()) {
                    Subscribe subscribe = subscribeIterator.next();
                    if (subscribe != null) {
                        long cTime = subscribe.getCtime();
                        long now = System.currentTimeMillis();
                        long ttl = MqConfig.inst.msg_default_alive_time_second * 1000;
                        if ((now - cTime >= ttl) && Message.Life.FOREVER != subscribe.getLife()) {
                            String id = subscribe.getId();
                            logger.warn("The scheduler task is running remove subscribe({}) on TTL.", id);
                            ProcessorImpl.INST.removeSubscribeOfCache(subscribe);
                            ProcessorImpl.INST.removeSubscribeOfDB(id);
                        }
                    }
                }

            }
        };
        return retry;
    }
}
