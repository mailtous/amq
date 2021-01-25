package com.artfii.amq.core.event;

import com.artfii.amq.disruptor.EventFactory;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.Subscribe;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.disruptor.EventFactory;

import java.nio.ByteBuffer;

/**
 * Func : 服务端收到的数据等同于一个 JOB 事件
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvent {
    private Message message;  // 普通消息
    private Subscribe subscribe;  //订阅
    private boolean storeAllMsg; // 保存所有的消息
    private ByteBuffer byteBuffer ;
    private AioPipe pipe;

    public static final EventFactory<JobEvent> EVENT_FACTORY = new EventFactory<JobEvent>()
    {
        public JobEvent newInstance()
        {
            return new JobEvent();
        }
    };

    /**
     * 把消息及 PIPE 封装到事件中
     * @param jobEvent
     * @param pipe
     * @param message
     */
    public static void translate(JobEvent jobEvent,long sequence,AioPipe pipe,Message message) {
        jobEvent.setPipe(pipe);
        jobEvent.setMessage(message);
    }

    public static void translate(JobEvent jobEvent, long sequence, Message msg) {
        jobEvent.setMessage(msg);
    }
    public static void translate(JobEvent jobEvent, long sequence, Subscribe msg) {
        jobEvent.setSubscribe(msg);
    }

    public static void translate(JobEvent jobEvent, long sequence, Message msg,boolean storeAllMsg) {
        jobEvent.setMessage(msg);
        jobEvent.setStoreAllMsg(storeAllMsg);
    }

    //===================================================================================================================

    public Message getMessage() {
        return message;
    }

    public JobEvent setMessage(Message message) {
        this.message = message;
        return this;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public JobEvent setSubscribe(Subscribe subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    public boolean isStoreAllMsg() {
        return storeAllMsg;
    }

    public JobEvent setStoreAllMsg(boolean storeAllMsg) {
        this.storeAllMsg = storeAllMsg;
        return this;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public JobEvent setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        return this;
    }

    public AioPipe getPipe() {
        return pipe;
    }

    public JobEvent setPipe(AioPipe pipe) {
        this.pipe = pipe;
        return this;
    }
}
