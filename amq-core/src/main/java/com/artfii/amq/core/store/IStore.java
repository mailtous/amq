package com.artfii.amq.core.store;

import java.util.List;

/**
 * Func :持久化
 *
 * @author: leeton on 2019/3/13.
 */
public interface IStore {
    static IStore ofServer(){//预留有其它持久化实现的可能
        return MapDbStore.INST;
    }

    /**
     * 服务端所有的 MQ 数据备份(感觉没有必要)
     */
    String server_mq_all_data = "server_mq_all_data.db";

    /**
     * 服务端需要重发的数据备份
     */
    String server_mq_need_retry = "server_mq_need_retry.db";
    /**
     * 服务端订阅者数据备份
     */
    String server_mq_subscribe = "server_mq_subscribe.db";

    /**
     * 服务端生产者消息备份
     */
    String server_mq_common_publish = "server_mq_common_publish_message.db";

    /**
     * 客户端订阅者数据备份
     */
    String client_mq_subscribe = "client_mq_subscribe.db";



    <T> boolean save(String dbName,String key, T obj);

    <T> T get(String dbName,String key,Class<T> tClass);

    <T> List<T> getAll(String dbName,Class<T> tClass);

    <T> List<T> list(String dbName, int pageNumber, int pageSize, Class<T> tClass);

    <T> Page<T> getPage(String dbName, Condition<T> topicFilter, Condition<T> timeFilter, Page page, Class<T> tClass);

    void remove(String dbName,String key);

}
