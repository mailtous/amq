package com.artlongs.amq.core.store;

import java.util.List;

/**
 * Func :持久化
 *
 * @author: leeton on 2019/3/13.
 */
public interface IStore {
    static IStore instOf(){//预留有其它持久化实现的可能
        return Store.INST;
    }

    /**
     * 所有的数据备份(感觉没有必要)
     */
    String mq_all_data = "mq_all_data.db";

    /**
     * 需要重发的数据备份
     */
    String mq_need_retry = "mq_need_retry.db";
    /**
     * 订阅者数据备份
     */
    String mq_subscribe = "mq_subscribe.db";

    /**
     * 生产者消息备份
     */
    String mq_common_publish = "mq_common_publish_message.db";

    <T> boolean save(String dbName,String key, T obj);

    <T> T get(String dbName,String key,Class<T> tClass);

    <T> List<T> getAll(String dbName,Class<T> tClass);

    <T> List<T> list(String dbName, int pageNumber, int pageSize, Class<T> tClass);

    <T> Page<T> getPage(String dbName, Condition<T> topicFilter, Condition<T> timeFilter, Page page, Class<T> tClass);

    void remove(String dbName,String key);

}
