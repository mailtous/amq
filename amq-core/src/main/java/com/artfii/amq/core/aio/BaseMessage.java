package com.artfii.amq.core.aio;

import com.artfii.amq.core.Message;
import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.tools.io.Buffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * 消息基类
 * <p>
 * amq:协议标志(4字节)
 * MessageType：消息类型(4个字节)
 * bodyLength： body长度(4个字节)
 *
 * @author: leeton on 2019/5/20.
 */
public class BaseMessage implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(BaseMessage.class);
    private Head head;
    private Message body;
    //=========================================

    public static BaseMessage ofHead(int baseMsgType, byte[] headSlot){
        BaseMessage bm = new BaseMessage();
        bm.setHead(new BaseMessage.Head(baseMsgType, headSlot));
        return bm;
    }
    public static BaseMessage ofAll(int baseMsgType, byte[] headSlot,Message body){
        BaseMessage bm = new BaseMessage();
        bm.setHead(new BaseMessage.Head(baseMsgType, headSlot));
        bm.setBody(body);
        return bm;
    }

    public static BaseMessage ofBody(int baseMsgType, String body){
        BaseMessage bm = new BaseMessage();
        bm.setHead(new BaseMessage.Head(baseMsgType, null));
        bm.setBody(Message.ofDef(Message.Key.ofDef(),body));
        return bm;
    }

    public static class Head implements Serializable {
        public static final int head_length = 64; //头部总长度
        public static final int amq = 0x00616d71; //amq
        private int protocol = 0;

        /** 消息类型 {@link BaseMsgType}*/
        private int kind;
        private int bodyLength = 0;  //消息总长度
        private byte[] slot; // 消息头包含的简短信息,长度必须满足: slot < head_length - protocol - kind

        public Head() {
            this.protocol = amq;
        }

        public Head(int kind) {
            this.protocol = amq;
            this.kind = kind;
        }

        public Head(int kind, byte[] slot) {
            this.protocol = amq;
            this.kind = kind;
            this.slot = slot;
        }

        public void encode(ByteBuffer buffer) {
            buffer.putInt(Head.amq);
            buffer.putInt(this.kind);
            buffer.putInt(this.bodyLength);
            int remainLength = head_length - buffer.position();
            if(null != this.slot && this.slot.length>remainLength){
                logger.error(" head slot length is OVERLOAD!");
            }
            if(null != this.slot && this.slot.length <= remainLength){
                buffer.put(this.slot);
            }
            buffer.position(Head.head_length);
        }

        public static Head decode(ByteBuffer buffer) {
            Head head = new Head();
            head.setProtocol(buffer.getInt()); //amq
            head.setKind(buffer.getInt());
            head.setBodyLength(buffer.getInt());
            int remainLength = head_length - buffer.position();
            final byte[] include = new byte[remainLength];
            buffer.get(include);
            head.setSlot(include);
            return head;
        }

        public int getBodyLength() {
            return bodyLength;
        }

        public void setBodyLength(int bodyLength) {
            this.bodyLength = bodyLength;
        }

        public int getKind() {
            return kind;
        }

        public void setKind(int kind) {
            this.kind = kind;
        }

        public int getProtocol() {
            return protocol;
        }

        public void setProtocol(int protocol) {
            this.protocol = protocol;
        }

        public byte[] getSlot() {
            return slot;
        }

        public void setSlot(byte[] slot) {
            this.slot = slot;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Head{");
            sb.append("protocol=").append(protocol);
            sb.append(", kind=").append(kind);
//            sb.append(", slot=").append(new String(slot));
            sb.append(", bodyLength=").append(bodyLength);
            sb.append('}');
            return sb.toString();
        }
    }

    public static boolean isAmq(ByteBuffer buffer) {
        return (Head.amq == buffer.getInt(4));
    }

    public static boolean isHeart(ByteBuffer buffer) {
        return isHeart(Head.decode(buffer));
    }

    public static boolean isHeart(Head head) {
        return (BaseMsgType.HEART_MESSAGE_REQ == head.getKind() || BaseMsgType.HEART_MESSAGE_RSP == head.getKind());
    }

    public static boolean isReConnectReq(Head head) {
        return (BaseMsgType.RE_CONNECT_REQ == head.getKind());
    }

    public static ByteBuffer encode(BaseMessage baseMessage) {
        byte[] bodyByte = ISerializer.Serializer.INST.of().toByte(baseMessage.getBody());
        int bodyLength = null == bodyByte ? 0 : bodyByte.length;
        ByteBuffer buffer = ByteBuffer.allocate(Head.head_length + bodyLength);
        //head
        baseMessage.getHead().setBodyLength(bodyLength);
        baseMessage.getHead().encode(buffer);
        //body
        if(bodyLength>0){
            buffer.put(bodyByte);
        }
        buffer.flip();
        return buffer;
    }

    public static BaseMessage decode(ByteBuffer buffer) {
        buffer.rewind();
        //识别消息长度
        if (buffer.remaining() < Head.head_length) {
            return null;
        }
        BaseMessage message = new BaseMessage();
        Head head = Head.decode(buffer);
        message.setHead(head);

        //判断 BODY 是否存在半包情况
        int bodyLength = head.getBodyLength();
        if (buffer.remaining() < bodyLength) {
            logger.error("[AIO]消息不完整,buffer剩余长度:{},总长:{}", buffer.remaining(), bodyLength);
            buffer.clear();
            buffer = null;
        }

        if(bodyLength>0){
            Message body = decodeBody(buffer, bodyLength);
            message.setBody(body);
        }
        return message;
    }

    private static Message decodeBody(ByteBuffer buffer, int bodyLength) {
        if (bodyLength == 0) {
            return null;
        }
        if (buffer.position() == 0) {
            Head.decode(buffer);
        }
        buffer.position(Head.head_length);
        byte[] bodyByte = Buffers.take(buffer, bodyLength);
        Message body = ISerializer.Serializer.INST.of().getObj(bodyByte, Message.class);
        return body;
    }

    //=========================================

    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public Message getBody() {
        return body;
    }

    public void setBody(Message body) {
        this.body = body;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BaseMessage{");
        sb.append("head=").append(head);
        sb.append(", body=").append(body);
        sb.append('}');
        return sb.toString();
    }
}
