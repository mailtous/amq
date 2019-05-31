package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.tools.ID;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.util.HashMap;

/**
 * Func :
 *
 * @author: leeton on 2019/2/18.
 */
public class ServerStore extends BaseStore {

    public static ServerStore INST = create();

    private HashMap<String, DB> db = new HashMap<>();
    // MQ 收到的所有数据
    private BTreeMap<String, byte[]> all_data = markMap(IStore.server_mq_all_data, Serializer.BYTE_ARRAY);
    // 需要重发的 MQ 数据
    private BTreeMap<String, byte[]> need_retry = markMap(IStore.server_mq_need_retry, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_subscribe = markMap(IStore.server_mq_subscribe, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_common_publish = markMap(IStore.server_mq_common_publish, Serializer.BYTE_ARRAY);

    private ServerStore() {
    }
    private static synchronized ServerStore create(){
        if (INST == null) {
            INST = new ServerStore();
        }
        return INST;
    }

    @Override
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

    public DB getDB(String dbName) {
        return db.get(dbName);
    }


    public static void main(String[] args) {
        Message msg = Message.ofDef(new Message.Key(ID.ONLY.id(), "hello"), "hello,world!");
        IStore.ofServer().save(IStore.server_mq_all_data, msg.getK().getId(), msg);
        System.err.println(IStore.ofServer().<Message>get(IStore.server_mq_all_data, msg.getK().getId(), Message.class));
        System.err.println(IStore.ofServer().<Message>getAll(IStore.server_mq_all_data, Message.class));
    }


}
