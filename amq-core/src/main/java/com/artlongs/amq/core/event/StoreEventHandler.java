package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.disruptor.WorkHandler;
import org.osgl.util.S;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : 存储事件 Handler
 *
 * @author: leeton on 2019/2/15.
 */
public class StoreEventHandler implements WorkHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(StoreEventHandler.class);
    @Override
    public void onEvent(JobEvent event) throws Exception {
        Message message = event.getMessage();
        if (message != null && Message.Type.ACK == message.getType()) return; // 签收的消息,没必要保存

        if(event.isStoreAllMsg()){ // 开启了保存所有消息
            if (message != null) {
                logger.debug("[MQ]执行消息保存到硬盘(ALL) ......");
                IStore.ofServer().save(IStore.server_mq_all_data, message.getK().getId(), message);
            }
        }else {
            if (message != null) {
                String dbName = getDbNameByMsgType(message);
                if (S.noBlank(dbName)) {
                    logger.debug("[MQ]执行消息保存到硬盘({}).",dbName);
                    IStore.ofServer().save(dbName,message.getK().getId(), message);
                }
            }
            Subscribe subscribe = event.getSubscribe();
            if (subscribe != null) {
                logger.debug("[MQ]执行消息保存到硬盘(subscribe) ......");
                IStore.ofServer().save(IStore.server_mq_subscribe,subscribe.getId(), subscribe);
            }
        }

    }

    private String getDbNameByMsgType(Message msg) {
        String dbName = "";
        switch (msg.getType()){
            case ACK:
                break;
            case SUBSCRIBE:
                return IStore.server_mq_subscribe;
            case PUBLISH:
                return IStore.server_mq_common_publish;
            case PUBLISH_JOB:
                return IStore.server_mq_common_publish;
            case ACCEPT_JOB:
                return IStore.server_mq_subscribe;

        }
        return dbName;
    }
}
