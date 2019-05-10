package com.artlongs.amq.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * Func :订阅(消息中心内部使用), Message 的简化版本
 *
 * @author: leeton on 2019/1/23.
 */
public class Subscribe implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topic;
    private Integer pipeId;
    private Message.Life life;
    private Message.Listen listen;
    private long ctime;
    private int idx; // 在队列里的 index,记录下来,以加速remove

    public Subscribe(String id, String topic, Integer pipeId, Message.Life life,Message.Listen listen,long ctime) {
        this.id = id;
        this.topic = topic;
        this.pipeId = pipeId;
        this.life = life;
        this.listen = listen;
        this.ctime = ctime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Subscribe subscribe = (Subscribe) o;
        return Objects.equals(topic, subscribe.topic) &&
                Objects.equals(pipeId, subscribe.pipeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, pipeId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Subscribe{");
        sb.append("id='").append(id).append('\'');
        sb.append(", topic='").append(topic).append('\'');
        sb.append(", pipeId=").append(pipeId);
        sb.append(", life=").append(life);
        sb.append(", listen=").append(listen);
        sb.append(", ctime=").append(ctime);
        sb.append(", idx=").append(idx);
        sb.append('}');
        return sb.toString();
    }
    //================================ 我的貂婵在那里 ================================================


    public String getId() {
        return id;
    }

    public Subscribe setId(String id) {
        this.id = id;
        return this;
    }
    public int getIdx() {
        return idx;
    }

    public Subscribe setIdx(int idx) {
        this.idx = idx;
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public Subscribe setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public Integer getPipeId() {
        return pipeId;
    }

    public Subscribe setPipeId(Integer pipeId) {
        this.pipeId = pipeId;
        return this;
    }

    public Message.Life getLife() {
        return life;
    }

    public Subscribe setLife(Message.Life life) {
        this.life = life;
        return this;
    }

    public Message.Listen getListen() {
        return listen;
    }

    public Subscribe setListen(Message.Listen listen) {
        this.listen = listen;
        return this;
    }

    public long getCtime() {
        return ctime;
    }

    public Subscribe setCtime(long ctime) {
        this.ctime = ctime;
        return this;
    }
}
