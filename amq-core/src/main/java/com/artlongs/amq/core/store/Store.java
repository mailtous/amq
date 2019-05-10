package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.FastList;
import com.artlongs.amq.tools.ID;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/18.
 */
public enum Store implements IStore {
    INST;
    ISerializer serializer = ISerializer.Serializer.INST.of();

    private HashMap<String, DB> db = new HashMap<>();
    // MQ 收到的所有数据
    private BTreeMap<String, byte[]> all_data = markMap(IStore.mq_all_data, Serializer.BYTE_ARRAY);
    // 需要重发的 MQ 数据
    private BTreeMap<String, byte[]> need_retry = markMap(IStore.mq_need_retry, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_subscribe = markMap(IStore.mq_subscribe, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_common_publish = markMap(IStore.mq_common_publish, Serializer.BYTE_ARRAY);

    private final static String DEF_TREEMAP_NAME = "amqdata";
    private static Map<Integer, List> filterListCache = new ConcurrentHashMap<>();

    public DB markDb(String dbName) {
        DB _db = DBMaker.fileDB(MqConfig.inst.mq_db_store_file_path + dbName)
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .allocateIncrement(1024)
                .cleanerHackEnable()
                .closeOnJvmShutdown()
                .transactionEnable()
                .concurrencyScale(128)
                .make();

        db.put(dbName, _db);
        return _db;
    }

    public BTreeMap markMap(String dbName, GroupSerializer seriaType) {
        BTreeMap<String, byte[]> myMap = markDb(dbName).treeMap(DEF_TREEMAP_NAME)
                .keySerializer(Serializer.STRING)
                .valueSerializer(seriaType)
                .valuesOutsideNodesEnable()
                .createOrOpen();

        return myMap;
    }

    public BTreeMap getMapBy(String dbName) {
        BTreeMap map = getDB(dbName).treeMap(DEF_TREEMAP_NAME).open();
        return map;
    }

    private DB getDB(String dbName) {
        return db.get(dbName);
    }


    @Override
    public <T> boolean save(String dbName, String key, T obj) {
        if (S.empty(dbName)) return false;
        if (S.empty(key)) return false;
        getMapBy(dbName).putIfAbsent(key, serializer.toByte(obj));
        getDB(dbName).commit();
        removeFilterListCache(dbName, getTopic(obj));
        return true;
    }

    @Override
    public <T> T get(String dbName, String key, Class<T> tClass) {
        byte[] bytes = (byte[]) getMapBy(dbName).get(key);
        if (bytes != null) {
            T obj = serializer.getObj(bytes, tClass);
            return obj;
        }
        return null;
    }

    @Override
    public <T> FastList<T> getAll(String dbName, Class<T> tClass) {
        FastList<T> list = new FastList<>(tClass, 200_000);
        for (Object o : getMapBy(dbName).values()) {
            list.add(serializer.getObj((byte[]) o, tClass));
        }
        return list;
    }

    public <T> List<T> list(String dbName, int pageNumber, int pageSize, Class<T> tClass) {
        FastList<T> result = new FastList<>(tClass, 200_000);
        FastList<T> allList = getAll(dbName, tClass);
        // filter of page
        int total = allList.size();
        int first = Page.first(pageNumber, pageSize);
        int limit = Page.limit(total, pageNumber, pageSize);
        Iterator<T> iter = allList.iterator();
        for (int i = first; i < limit; i++) {
            result.add(iter.next());
        }
        clearList(allList);
        return result;
    }

    public <T> Page<T> getPage(String dbName, Condition<T> topicFilter, Condition<T> timeFilter, Page page, Class<T> tClass) {
        List<T> result = new ArrayList<>(1000);
        // filter of page
        filterByPage(page, filterByCondition(dbName, topicFilter, timeFilter, tClass));

        return page;
    }

    private <T> void filterByPage(Page page, List<T> oldList) {
        int total = oldList.size();
        page.setTotal(total);
        int first = page.first();
        int limit = first + page.limit();
        List<T> items = oldList.subList(first, limit);
        page.setItems(items);
    }

    /**
     * 按条件过滤
     *
     * @param dbName      数据库名
     * @param topicFilter 主题过滤条件
     * @param timeFilter  时间过滤条件
     * @param tClass
     * @param <T>
     * @return
     */
    private <T> List<T> filterByCondition(String dbName, Condition<T> topicFilter, Condition<T> timeFilter, Class<T> tClass) {
        List<T> filteredList = new ArrayList<>(10000);
        int filterKey = Condition.hashCondition(dbName, topicFilter, timeFilter);
        filteredList = filterListCache.get(filterKey); // 从缓存里找一下,看相同的条件之前是不是已经查询过.
        if (C.isEmpty(filteredList)) { //
            FastList<T> allList = getAll(dbName, tClass);
            // filter of condition ...
            if (timeFilter != null) {
                filteredList = allList.stream().filter(topicFilter.getPredicate()).filter(timeFilter.getPredicate()).collect(Collectors.toList());
            } else {
                filteredList = allList.stream().filter(topicFilter.getPredicate()).collect(Collectors.toList());
            }
            if (C.notEmpty(filteredList)) {
                filterListCache.putIfAbsent(filterKey, filteredList);
            }
        }
        return filteredList;
    }

    /**
     * 删除缓存的结果集
     *
     * @param dbName
     * @param topic
     */
    private void removeFilterListCache(String dbName, String topic) {
        List<Integer> keyList = Condition.hashMapOfTopic.get(dbName + topic);
        if (C.notEmpty(keyList)) {
            for (Integer key : keyList) {
                filterListCache.remove(key);
            }
        }
    }

    @Override
    public void remove(String dbName, String key) {
        getMapBy(dbName).remove(key);
        getDB(dbName).commit();
    }


    private String getTopic(Object obj) {
        if (obj instanceof Message) {
            return ((Message) obj).getK().getTopic();
        }
        if (obj instanceof Subscribe) {
            return ((Subscribe) obj).getTopic();
        }
        return "";
    }

    private void clearList(List list) {
        list.clear();
        list = null;
    }


    public static void main(String[] args) {
        Message msg = Message.ofDef(new Message.Key(ID.ONLY.id(), "hello"), "hello,world!");
        Store.INST.save(IStore.mq_all_data, msg.getK().getId(), msg);
        System.err.println(Store.INST.<Message>get(IStore.mq_all_data, msg.getK().getId(), Message.class));
        System.err.println(Store.INST.<Message>getAll(IStore.mq_all_data, Message.class));
    }


}
