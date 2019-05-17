package com.artlongs.amq.core;

import com.alibaba.fastjson.JSON;
import com.artlongs.amq.tools.ID;
import org.osgl.util.C;

import java.io.Serializable;
import java.util.Set;

/**
 * FUNC: Mq message
 * Created by leeton on 2018/12/13.
 */
@SuppressWarnings("unchecked")
public class Message<K extends Message.Key, V> implements KV<K, V> {
    private static final long serialVersionUID = 1L;
    public static final String BACK = "back"; // 任务的回复 TOPIC 标示

    ////=============================================
    private Key k;
    private V v; // centent body
    private Stat stat;
    private String subscribeId;  //订阅者 ID
    private Life life;
    private Listen listen;
    private Type type;

    ////=============================================
    public static Message ofDef(Key k, Object v) {
        long now = System.currentTimeMillis();
        Message m = new Message();
        m.k = k;
        m.v = v;
        m.type = Type.PUBLISH;
        Stat stat = new Stat()
                .setCtime(now)
                .setMtime(now)
                .setDelay(0)
                .setRetry(0);
        m.stat = stat;
        return m;
    }

    public static Message empty() {
        return new Message();
    }

    /**
     * 创建 ACK 消息
     *
     * @return
     */
    public static Message ofAcked(String msgId, Life life) {
        Message m = new Message();
        m.setK(new Key());
        m.k.id = msgId;
        m.life = life;
        m.setType(Type.ACK);
        m.k.sendNode = null;
        m.k.topic = null;
        m.stat = null;
        m.v = null;
        return m;
    }

    /**
     * 创建 MESSAGE ID 格式: xx(2位客户机编号)_yyyyMMddHHmmssSSS"(17) + (2位)原子顺序数累加
     *
     * @param clientId
     * @return
     */
    public static String createId(String clientId) {
        String id = clientId + ID.ONLY.id();
        return id;
    }

    // ======================================================= MESSAGE BUILD BEGIN ======================================
    public static <V> Message buildCommonMessage(String topic, V data, Integer sendNode) {
        Message.Key mKey = key(topic, sendNode);
        return Message.ofDef(mKey, data);
    }

    public static <V> Message buildSubscribe(String topic, V v, Integer sendNode, Message.Life life, Message.Listen listen) {
        Message.Key mKey = key(topic, sendNode);
        Message message = Message.ofSubscribe(mKey, v, life, listen);
        return message;
    }

    public static <V> Message buildPublishJob(String topic, V v, Integer sendNode) {
        Message.Key mKey = key(topic, sendNode);
        Message message = Message.ofPublicJob(mKey, v);
        return message;
    }

    public static Message buildAcceptJob(String topic, Integer sendNode) {
        Message.Key mKey = key(topic, sendNode);
        Message message = Message.ofAcceptJob(mKey);
        return message;
    }

    public static <V> Message buildFinishJob(String topic, V v, Integer sendNode) {
        String jobTopic = buildFinishJobTopic(topic);
        Message.Key mKey = key(jobTopic, sendNode);
        Message message = Message.ofFinishJob(mKey, v);
        return message;
    }

    public static Message buildAck(String msgId, Message.Life life) {
        Message message = Message.ofAcked(msgId, life);
        return message;
    }

    public static Message.Key key(String topic, Integer sendNode) {
        Message.Key mKey = new Message.Key(createId(""), topic);
        mKey.setSendNode(sendNode);
        return mKey;
    }

    /**
     * 任务结果的TOPIC
     * @param oldTopic  原来的 TOPIC
     * @return
     */
    public static String buildFinishJobTopic(String oldTopic) {
        return BACK + "_" + oldTopic;
    }

    private static <V> Message ofSubscribe(Key k, V v, Life life, Listen listen) {
        return ofDef(k, v).setSubscribeId(k.id).setLife(life).setListen(listen).setType(Type.SUBSCRIBE);
    }

    private static <V> Message ofPublicJob(Key k, V v) {
        //注意这里不生成普通的订阅. MQ 中心里会手动生成一个特殊的订阅
        return ofDef(k, v).setLife(Life.ALL_ACKED).setListen(Listen.FUTURE_AND_ONCE).setType(Type.PUBLISH_JOB);
    }

    private static Message ofAcceptJob(Key k) {//实际上是一个订阅类别的消息
        return ofSubscribe(k, null, Life.FOREVER, Listen.CALLBACK).setType(Type.ACCEPT_JOB);
    }

    private static <V> Message ofFinishJob(Key k, V v) {
        return ofDef(k, v).setLife(Life.ALL_ACKED).setType(Type.FINISH_JOB);
    }

    // ======================================================= MESSAGE BUILD END =========================================

    public void upStatOfSended(Integer node) {
        Stat stat = getStat();
        if (null == stat.nodesDelivered) {
            stat.nodesDelivered = C.newSet();
        }
        stat.nodesDelivered.add(node);
        stat.setMtime(System.currentTimeMillis());
        stat.setOn(Message.ON.SENED);
    }

    public void upStatOfACK(Integer node) {
        Stat stat = getStat();
        if (null == stat.nodesConfirmed) {
            stat.nodesConfirmed = C.newSet();
        }
        stat.nodesConfirmed.add(node);
        stat.setMtime(System.currentTimeMillis());
        stat.setOn(ON.ACKED);
    }

    /**
     * 累加重发次数
     */
    public void incrRetry() {
        Stat stat = getStat();
        if (null != stat) {
            stat.setRetry(stat.getRetry() + 1);
        }
    }

    /**
     * 累加延迟次数
     */
    public void incrDelay() {
        Stat stat = getStat();
        if (null != stat) {
            stat.setDelay(stat.getDelay() + 1);
        }
    }

    public int ackedSize() { //@see upStatOfACK
        if (null == this.getStat()) return 0;
        if (null == this.getStat().getNodesConfirmed()) return 0;
        return this.getStat().getNodesConfirmed().size();
    }

    public boolean ackMsgTF() {
        return Type.ACK.equals(this.type);
    }

    public Boolean subscribeTF() {
        return (null != subscribeId) || (Message.Type.SUBSCRIBE == this.getType());
    }

    ////=============================================

    @Override
    public V get(K k) {
        if (this.k.equals(k)) return this.v;
        return null;
    }

    @Override
    public KV put(K k, V v) {
        this.k = k;
        this.v = v;
        return this;
    }

    public Key getK() {
        return k;
    }

    public Message<K, V> setK(Key k) {
        this.k = k;
        return this;
    }

    public V getV() {
        return v;
    }

    public Message<K, V> setV(V v) {
        this.v = v;
        return this;
    }

    public Stat getStat() {
        return stat;
    }

    public Message<K, V> setStat(Stat stat) {
        this.stat = stat;
        return this;
    }

    public Message<K, V> setSubscribeId(String subscribeId) {
        this.subscribeId = subscribeId;
        return this;
    }

    public String getSubscribeId() {
        return subscribeId;
    }

    public Life getLife() {
        return life;
    }

    public Message<K, V> setLife(Life life) {
        this.life = life;
        return this;
    }

    public Listen getListen() {
        return listen;
    }

    public Message<K, V> setListen(Listen listen) {
        this.listen = listen;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Message setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

//====================================    Message Key   ====================================

    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id; // message Id
        private String topic;
        private Integer sendNode;  //发布者节点 (pipeId)

        public Key() {
        }

        public Key(String id, String topic) {
            this.id = id;
            this.topic = topic;
        }

        public Key(String id, String topic, Integer sendNode) {
            this.id = id;
            this.topic = topic;
            this.sendNode = sendNode;
        }

        //========================= 羁绊是什么意思呢？===============================

        public String getId() {
            return id;
        }

        public String getTopic() {
            return topic;
        }

        public Integer getSendNode() {
            return sendNode;
        }

        public Key setId(String id) {
            this.id = id;
            return this;
        }

        public Key setTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public Key setSendNode(Integer sendNode) {
            this.sendNode = sendNode;
            return this;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    //====================================    Message Stat   ====================================
    public static class Stat implements Serializable {
        private static final long serialVersionUID = 1L;
        private ON on;
        private Long ttl = MqConfig.inst.msg_default_alive_time_second;   // Time To Live, 消息的存活时间,如果未成功发送,则最多存活一天
        private Long ctime; // create time
        private Long mtime; // modify time
        private int delay;  // 多次发送数(消息未ACKED,则delay多少秒后重发)
        private int retry;  // 重试次数(发送失败之后再重发)
        private Set<Integer> nodesDelivered; // 已送达
        private Set<Integer> nodesConfirmed; // 已确认

        public ON getOn() {
            return on;
        }

        public Stat setOn(ON on) {
            this.on = on;
            return this;
        }

        public Long getTtl() {
            return ttl;
        }

        public Stat setTtl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Long getCtime() {
            return ctime;
        }

        public Stat setCtime(long ctime) {
            this.ctime = ctime;
            return this;
        }

        public Long getMtime() {
            return mtime;
        }

        public Stat setMtime(long mtime) {
            this.mtime = mtime;
            return this;
        }

        public int getDelay() {
            return delay;
        }

        public Stat setDelay(int delay) {
            this.delay = delay;
            return this;
        }

        public int getRetry() {
            return retry;
        }

        public Stat setRetry(int retry) {
            this.retry = retry;
            return this;
        }

        public Set<Integer> getNodesDelivered() {
            return nodesDelivered;
        }

        public Stat setNodesDelivered(Set<Integer> nodesDelivered) {
            this.nodesDelivered = nodesDelivered;
            return this;
        }

        public Set<Integer> getNodesConfirmed() {
            return nodesConfirmed;
        }

        public Stat setNodesConfirmed(Set<Integer> nodesConfirmed) {
            this.nodesConfirmed = nodesConfirmed;
            return this;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    /**
     * Message status
     */
    public enum ON {
        QUENED, SENDING, SENDONFAIL, SENED, ACKED;
    }

    /**
     * 消息类型
     */
    public enum Type {
        SUBSCRIBE, // 普通订阅
        PUBLISH,   // 普通发布消息
        ACK,       // 签收消息
        PUBLISH_JOB, //发布工作任务(PING)
        ACCEPT_JOB,  //接受工作任务
        FINISH_JOB;  //完成工作任务(PONG)
    }

    /**
     * 消息的生命周期
     */
    public enum Life {
        FOREVER,ALL_ACKED, SPARK;
    }

    /**
     * 监听消息的模式
     */
    public enum Listen {
        FUTURE_AND_ONCE, // 使用 FUTURE 机制,但只运行一次
        CALLBACK; // 回调的模式,可多次运行
    }


    public static void main(String[] args) {
        Message msg = Message.ofDef(new Key(createId(""), "quene"), "hello");
        System.err.println("msg=" + msg);
    }


}
