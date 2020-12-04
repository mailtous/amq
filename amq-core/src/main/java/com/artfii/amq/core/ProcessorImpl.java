package com.artfii.amq.core;

import com.artfii.amq.core.aio.*;
import com.artfii.amq.core.aio.plugin.MonitorPlugin;
import com.artfii.amq.core.event.BizEventHandler;
import com.artfii.amq.core.event.JobEvent;
import com.artfii.amq.core.event.StoreEventHandler;
import com.artfii.amq.core.store.IStore;
import com.artfii.amq.disruptor.*;
import com.artfii.amq.disruptor.dsl.Disruptor;
import com.artfii.amq.disruptor.dsl.ProducerType;
import com.artfii.amq.disruptor.util.DaemonThreadFactory;
import com.artfii.amq.tools.FastList;
import com.artfii.amq.tools.IOUtils;
import com.artfii.amq.tools.RingBufferQueue;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioServer;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/22.
 */
public enum ProcessorImpl implements Processor {
    INST;
    private static Logger logger = LoggerFactory.getLogger(ProcessorImpl.class);

    /**
     * 从客户端读取到的数据缓存(普通publish消息) map(messageId,Message)
     */
    private ConcurrentSkipListMap<String, Message> cache_common_publish_message = new ConcurrentSkipListMap();

    /**
     * 发送失败的消息数据缓存 map(messageId,Message)
     */
    private ConcurrentSkipListMap<String, Message> cache_falt_message = new ConcurrentSkipListMap();

    /**
     * 发布的工作任务缓存
     **/
    private static ConcurrentSkipListMap<String, Message> cache_public_job = new ConcurrentSkipListMap();

    /**
     * 订阅的缓存 RingBufferQueue(Subscribe)
     */
    private static RingBufferQueue<Subscribe> cache_subscribe = new RingBufferQueue<>(MqConfig.inst.mq_subscribe_quene_cache_sizes);

    // ringBuffer cap setting
    private final int WORKER_BUFFER_SIZE = 1024 * 32;

    //创建消息多线程任务分发器 ringBuffer
    private final RingBuffer<JobEvent> job_worker;

    // 业务处理 worker
//    private final RingBuffer<JobEvent> biz_worker;
    // 业务处理 worker pool
//    private final WorkerPool<JobEvent> biz_worker_pool;

    // 消息持久化 worker
    private RingBuffer<JobEvent> persistent_worker = null;
    // 消息持久化 worker pool
    private WorkerPool<JobEvent> persistent_worker_pool = null;
    ExecutorService jobPool = null;
    ExecutorService bizPool = null;
    ExecutorService storeThreadPool = null;
    private Disruptor mqDisruptor;

    private static boolean shutdowNow = false; // 关闭服务
    private MonitorPlugin monitor;

    ProcessorImpl() {
        //创建分发器
        this.mqDisruptor = createDisrupter();
        // 启动：消息任务分发器
        this.job_worker = mqDisruptor.start();
        //
//        jobPool = IOUtils.createFixedThreadPool(MqConfig.inst.worker_thread_pool_size, "MQ:job-");
//        bizPool = IOUtils.createFixedThreadPool(MqConfig.inst.worker_thread_pool_size, "MQ:biz-");

        //数据持久化，采用独立的线程池来分配工作
        storeThreadPool = IOUtils.createFixedThreadPool(MqConfig.inst.worker_thread_pool_size, "MQ:store-");
        //
//        this.biz_worker_pool = createWorkerPool(new BizEventHandler());
//        this.biz_worker = biz_worker_pool.start(bizPool);

        //数据持久化的线程池交给 Ringbuffer 进行管理
        this.persistent_worker_pool = createWorkerPool(new StoreEventHandler());
        this.persistent_worker = persistent_worker_pool.start(storeThreadPool);

    }

    public ProcessorImpl addMonitor(MonitorPlugin monitorPlugin) {
        this.monitor = monitorPlugin;
        return this;
    }

    /**
     * 创建消息 disruptor
     *
     * @return
     */
    private Disruptor<JobEvent> createDisrupter() {
        Disruptor<JobEvent> disruptor =
                new Disruptor<JobEvent>(
                        JobEvent.EVENT_FACTORY,
                        WORKER_BUFFER_SIZE,
                        DaemonThreadFactory.INSTANCE,
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());
        disruptor.handleEventsWith(new BizEventHandler());
        return disruptor;
    }

    /**
     * 创建工作线程池
     *
     * @param workHandler 事件处理器
     * @return
     */
    private WorkerPool<JobEvent> createWorkerPool(WorkHandler workHandler) {
        final RingBuffer<JobEvent> ringBuffer = RingBuffer.createSingleProducer(JobEvent.EVENT_FACTORY, WORKER_BUFFER_SIZE, new BlockingWaitStrategy());
        WorkerPool<JobEvent> workerPool = new WorkerPool<JobEvent>(ringBuffer, ringBuffer.newBarrier(), new FatalExceptionHandler(), workHandler);
        //init sequences
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;

    }


    /**
     * 收到消息后进一步处理（订阅、发布、持久化)
     * @param pipe
     * @param message
     */
    @Override
    public void onMessage(AioPipe pipe, Message message) {
        if (!shutdowNow && null != message) {
            if (MqConfig.inst.start_store_all_message_to_db) { // 配置了持久化所有消息
                if (!message.subscribeTF()) {
                    tiggerStoreAllMsgToDb(persistent_worker, message);
                }
            }
            String msgId = message.getK().getId();
            if (message.ackMsgTF()) { // ACK 消息
                incrAck();
                if (Message.Life.SPARK == message.getLife()) {
                    removeSubscribeCacheOnAck(msgId);
                    removeDbDataOfDone(msgId);
                } else {
                    Integer clientNode = getNode(pipe);
                    upStatOfACK(clientNode, message);
                }
            } else {
                if (message.subscribeTF()) { // subscribe msg
                    addSubscribeIF(pipe, message);
                    if (isAcceptJob(message)) { // 如果工作任务已经先一步发布了,则触发-->直接把任务发给订阅者
                        incrAccpetJob();
                        triggerDirectSendJobToAcceptor(pipe, message);
                    }else {
                        incrCommonSubscribe();
                    }
                    //
                    return;

                } else {
                    if (isPublishJob(message)) { // 发布的消息为工作任务(pingpong)
                        incrPublishJob();
                        cachePubliceJobMessage(msgId, message);
                        buildSubscribeWaitingJobResult(pipe, message);
                        Subscribe acceptor = getSubscribe(message.getK().getTopic());
                        if (null != acceptor) { // 本任务已经有订阅者
                            sendMessageToSubcribe(message,acceptor);
                            return;
                        }
                    }else {
                        incrCommonPublish();
                        if (Message.Life.SPARK != message.getLife()) {
                            cacheCommonPublishMessage(msgId, message);
                        }
                    }
                    // 发布消息
                    publishJobToWorker(message);
                }
            }
        }

    }


    /**
     * 分派消息
     *
     * @param message
     */
    public void pulishJobEvent(Message message) {
        message.getStat().setOn(Message.ON.SENDING);
        job_worker.publishEvent(JobEvent::translate, message);
    }

    /**
     * 分派消息,并把消息持久化放到线程池里去执行
     *
     * @param message
     */
    @Override
    public void publishJobToWorker(Message message) {
        message.getStat().setOn(Message.ON.SENDING);
//        publishBizToWorkerPool(biz_worker, message);
        pulishJobEvent(message);
        tiggerStoreCommonMessageToDb(persistent_worker, message);
    }

    private void publishBizToWorkerPool(RingBuffer<JobEvent> ringBuffer, Message message) {
        ringBuffer.publishEvent(JobEvent::translate, message);
    }

    private void tiggerStoreAllMsgToDb(RingBuffer<JobEvent> ringBuffer, Message message) {
        ringBuffer.publishEvent(JobEvent::translate, message, true);
    }

    private void tiggerStoreSubscribeToDb(RingBuffer<JobEvent> ringBuffer, Subscribe subscribe) {
        if (Message.Life.SPARK == subscribe.getLife()) return;
        ringBuffer.publishEvent(JobEvent::translate, subscribe);
    }

    private void tiggerStoreCommonMessageToDb(RingBuffer<JobEvent> ringBuffer, Message message) {
        if (Message.Life.SPARK == message.getLife()) return;
        ringBuffer.publishEvent(JobEvent::translate, message);
    }

    /**
     * 如果是订阅消息,加入到订阅队列
     *
     * @param pipe
     * @param message
     * @return
     */
    private boolean addSubscribeIF(AioPipe pipe, Message message) {
        if (message.subscribeTF()) {
            String clientKey = message.getK().getId();
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(), pipe.getId(), message.getLife(), message.getListen(), System.currentTimeMillis());
            RingBufferQueue.Result result = cache_subscribe.putIfAbsent(listen);
            if (result.success) {
                listen.setIdx(result.index);
                tiggerStoreSubscribeToDb(persistent_worker, listen);
                return true;
            }
        }
        return false;
    }

    /**
     * 标记消息已经收到
     *
     * @param clientNode 消息的通道 ID
     * @param ack
     */
    private void upStatOfACK(Integer clientNode, Message ack) {
        String ackId = ack.getK().getId();
        Message message = getMessageOfCache(ackId);
        if (null != message) {
            message.upStatOfACK(clientNode);
        }
    }

    /**
     * 消息类型为 {@link Message.Type#PUBLISH_JOB} 时,自动为它创建一个订阅,以收取任务结果
     * NOTE: 这里是实时的收取任务结果,所以不需要保存到硬盘
     *
     * @param pipe
     * @param message
     * @return
     */
    private boolean buildSubscribeWaitingJobResult(AioPipe pipe, Message message) {
        String jobId = message.getK().getId();
        String jobTopc = Message.buildFinishJobTopic(message.getK().getTopic());
        Subscribe subscribe = new Subscribe(jobId, jobTopc, pipe.getId(), message.getLife(), message.getListen(), System.currentTimeMillis());
        RingBufferQueue.Result result = cache_subscribe.putIfAbsent(subscribe);
        if (result.success) {
            subscribe.setIdx(result.index);
        }
        return true;
    }

    private Subscribe getSubscribe(String topic) {
        Iterator<Subscribe> subscribes = cache_subscribe.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(topic)) {
                AioPipe pipe = getPipeBy(listen.getPipeId());
                if(null != pipe && pipe.isOpen()) return listen;
            }
        }
        return null;
    }

    /**
     * 按 TOPIC 前缀式匹配
     *
     * @param topic
     * @return
     */
    public FastList<Subscribe> subscribeMatchOfTopic(String topic) {
        FastList<Subscribe> list = new FastList<>(Subscribe.class);
        Iterator<Subscribe> subscribes = cache_subscribe.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(topic)) {
                list.add(listen);
            }
        }
        return list;
    }


    /**
     * 发送消息给订阅方
     *
     * @param message
     * @param subscribeList
     */
    public void sendMessageToSubcribe(Message message, List<Subscribe> subscribeList) {
        for (Subscribe subscribe : subscribeList) {
            if (isPipeClosed(subscribe)) {
                removeSubscribeOfCache(subscribe);
                removeSubscribeOfDB(subscribe.getId());
                continue;
            }
            // 当客户端全部 ACK,则 remove 掉缓存
            if (removeMessageWhenAllAcked(subscribeList.size(), message)) return;
            sendMessageToSubcribe(message, subscribe);
        }
    }

    /**
     * 发送消息给订阅者
     *
     * @param subscribe
     * @param message
     */
    private void sendMessageToSubcribe(Message message, Subscribe subscribe) {
        // 当前的消息,客户端已签收过
        if (isAcked(message, subscribe)) return;
        //追加订阅者的消息ID及状态
        changeMessageOnReply(subscribe, message);
        // 发送消息给订阅方
        AioPipe pipe = getPipeBy(subscribe.getPipeId());
        boolean writed = write(pipe, message);
        if (writed) {
            onSendSuccToPrcess(subscribe, message);
        } else {
            onSendFailToBackup(message);
        }

    }

    private boolean write(AioPipe pipe, Message message) {
        BaseMessage baseMessage = BaseMessage.ofAll(BaseMsgType.BYTE_ARRAY_MESSAGE_REQ, null, message);
        return pipe.write(baseMessage);
    }

    /**
     * 删除所有客户端已经签收的消息
     *
     * @param subscribeSize 订阅数
     * @param message
     * @return
     */
    private boolean removeMessageWhenAllAcked(int subscribeSize, Message message) {
        int ackedSize = message.ackedSize();
        if (ackedSize >= subscribeSize) {
            String msgId = message.getK().getId();
            removeCacheOfDone(msgId);
            removeDbDataOfDone(msgId);
            return true;
        }
        return false;
    }

    /**
     * 追加订阅者的消息ID及状态 , 以便订阅端读取到对应的消息
     *
     * @param subscribe
     * @param message
     */
    private void changeMessageOnReply(Subscribe subscribe, Message message) {
        message.setSubscribeId(subscribe.getId());
        message.setLife(subscribe.getLife());
        message.setListen(subscribe.getListen());
    }

    /**
     * 通道失效
     */
    private boolean isPipeClosed(Subscribe subscribe) {
        try {
            if (null == subscribe.getPipeId()) {
                return true;
            }
            AioPipe pipe = getPipeBy(subscribe.getPipeId());
            if (null == pipe) {
                return true;
            }
            if (pipe.isClose()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 当前的消息,客户端已签收过
     *
     * @param message
     * @param subscribe
     * @return
     */
    private boolean isAcked(Message message, Subscribe subscribe) {
        Set<Integer> comfirmList = message.getStat().getNodesConfirmed();
        if (C.notEmpty(comfirmList)) {
            for (Integer confirm : comfirmList) {
                if (confirm.equals(subscribe.getPipeId())) return true;
            }
        }
        return false;
    }

    private void onSendFailToBackup(Message message) {
        incrSendFail();
        message.getStat().setOn(Message.ON.SENDONFAIL);
        String id = message.getK().getId();
        IStore.ofServer().save(IStore.server_mq_need_retry, id, message);
        cache_common_publish_message.remove(id);
        cache_falt_message.putIfAbsent(id, message);
    }

    private void onSendSuccToPrcess(Subscribe listen, Message message) {
        message.upStatOfSended(listen.getPipeId());
        incrSendSucc();
        if (Message.Life.SPARK == listen.getLife()) {
            removeSubscribeOfCache(listen);
        }
    }

    /**
     * 客户端的通信通道 ID
     *
     * @param pipe
     * @return
     */
    private Integer getNode(AioPipe pipe) {
        return pipe.getId();
    }

    public Message getMessageOfCache(String id) {
        return cache_common_publish_message.get(id);
    }

    private void cacheCommonPublishMessage(String key, Message message) {
        cache_common_publish_message.putIfAbsent(key, message);
    }

    private void cachePubliceJobMessage(String key, Message message) {
        cache_public_job.putIfAbsent(key, message);
    }

    public void removeCacheOfDone(String key) {
        cache_common_publish_message.remove(key);
        cache_falt_message.remove(key);
    }
    public void removePublishJobOfAcked(String key) {
        cache_public_job.remove(key);
        cache_falt_message.remove(key);
    }

    public void removeDbDataOfDone(String key) {
        IStore.ofServer().remove(IStore.server_mq_all_data, key);
        IStore.ofServer().remove(IStore.server_mq_need_retry, key);
        IStore.ofServer().remove(IStore.server_mq_common_publish, key);
    }

    private void removeSubscribeCacheOnAck(String ackId) {
        Iterator<Subscribe> iter = cache_subscribe.iterator();
        while (iter.hasNext()) {
            Subscribe subscribe = iter.next();
            if (subscribe != null && ackId.equals(subscribe.getId())) {
                cache_subscribe.remove(subscribe.getIdx());
                removeSubscribeOfDB(subscribe.getId());
                break;
            }
        }
    }

    public void removeSubscribeOfCache(Subscribe subscribe) {
        try {
            cache_subscribe.remove(subscribe.getIdx());
        } catch (Exception e) {
            logger.error(" remove subscribe-cache element exception.");
        }
    }

    public void removeSubscribeOfDB(String subscribeId) {
        IStore.ofServer().remove(IStore.server_mq_subscribe, subscribeId);
    }

    private boolean isPublishJob(Message message) {
        return Message.Type.PUBLISH_JOB == message.getType();
    }

    private boolean isAcceptJob(Message message) {
        return Message.Type.ACCEPT_JOB == message.getType();
    }

    /**
     * 直接把任务发给接收者
     *
     * @param pipe
     * @param acceptor
     */
    private void triggerDirectSendJobToAcceptor(AioPipe pipe, Message acceptor) {
        String topic = acceptor.getK().getTopic();
        Message job = matchPublishJob(topic);
        if (null != job) {
            Subscribe subscribe = new Subscribe(acceptor.getK().getId(), topic, pipe.getId(), acceptor.getLife(), acceptor.getListen(), System.currentTimeMillis());
            sendMessageToSubcribe(job, subscribe);
        }
    }

    /**
     * 匹配已经发布的任务 JOB
     *
     * @param topic
     * @return
     */
    private Message matchPublishJob(String topic) {
        if (cache_public_job.size() == 0) return null;
        final Collection<Message> messageList = cache_public_job.values();
        for (Message message : messageList) {
            if (message.getK().getTopic().startsWith(topic)) return message;
        }
        return null;
    }

    private AioPipe getPipeBy(Integer pipeId) {
        return AioServer.getChannelAliveMap().get(pipeId);
    }


    @Override
    public void shutdown() {
        this.shutdowNow = true;
        shutdownService();
        clearAllCache();
    }

    private void clearAllCache() {
        cache_public_job.clear();
        cache_falt_message.clear();
        cache_common_publish_message.clear();
        cache_subscribe.clear();
    }

    private void shutdownService() {
        try {
            this.mqDisruptor.shutdown();
//            this.biz_worker_pool.drainAndHalt();
            this.persistent_worker_pool.drainAndHalt();
//            this.bizPool.shutdown();
            this.storeThreadPool.shutdown();
        } catch (Exception e) {
            logger.error("Shutdow MQ service Error:", e);
        }
    }

    public ConcurrentSkipListMap<String, Message> getCache_common_publish_message() {
        return cache_common_publish_message;
    }

    public ConcurrentSkipListMap<String, Message> getCache_falt_message() {
        return cache_falt_message;
    }

    public ConcurrentSkipListMap<String, Message> getCache_public_job() {
        return cache_public_job;
    }

    public RingBufferQueue<Subscribe> getCache_subscribe() {
        return cache_subscribe;
    }

    public void incrCommonSubscribe(){
        if (null != monitor) {
            monitor.incrCommonSubscribe();
        }
    }
    public void incrCommonPublish(){
        if (null != monitor) {
            monitor.incrCommonPublish();
        }
    }

    public void incrAccpetJob(){
        if (null != monitor) {
            monitor.incrAccpetJob();
        }
    }

    public void incrPublishJob(){
        if (null != monitor) {
            monitor.incrPublishJob();
        }
    }
    public void incrSendFail(){
        if (null != monitor) {
            monitor.incrSendFail();
        }
    }
    public void incrSendSucc(){
        if (null != monitor) {
            monitor.incrSendSucc();
        }
    }
    public void incrAck(){
        if (null != monitor) {
            monitor.incrAck();
        }
    }

    /**
     * 更换已经失效的 PIPEID
     * @param oldPipeId
     * @param pipeId
     */
    public void replacePipeIdOnReconnect(Integer oldPipeId, Integer pipeId) {
//        logger.debug("更换已经失效的PIPE:{} -> {}", oldPipeId, pipeId);
        List<Subscribe> retryList = IStore.ofServer().getAll(IStore.server_mq_subscribe, Subscribe.class);
        if (C.notEmpty(retryList)) {
            for (Subscribe subscribe : retryList) {
                cache_subscribe.putIfAbsent(subscribe);
                if(oldPipeId.equals(subscribe.getPipeId())){
                    subscribe.setPipeId(pipeId);
                    IStore.ofServer().remove(IStore.server_mq_subscribe,subscribe.getId());
                    IStore.ofServer().save(IStore.server_mq_subscribe,subscribe.getId(), subscribe);
                }
            }
        }
        //
        Iterator<Subscribe> iterator = cache_subscribe.iterator();
        while (iterator.hasNext()) {
            Subscribe subscribe = iterator.next();
            if(oldPipeId.equals(subscribe.getPipeId())){
                subscribe.setPipeId(pipeId);
            }
        }

        //
        AioServer.removeChannelOfAliveMap(oldPipeId);

    }
}
