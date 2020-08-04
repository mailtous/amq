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
    private HeadMessage head;
    private Message body;
    //=========================================

    public static class HeadMessage implements Serializable {
        public static final int head_length = 128; //头部总长度
        public static final int amq = 0x00616d71; //amq
        private int protocol = 0;

        /** 消息类型 {@link BaseMsgType}*/
        private int baseMsgType;
        private int bodyLength = 0;  //消息总长度
        private byte[] include; // 消息头包含的简短信息 , 必须满足: include < head_length - protocol-baseMsgType

        public HeadMessage() {
            this.protocol = amq;
        }

        public HeadMessage(int baseMsgType) {
            this.protocol = amq;
            this.baseMsgType = baseMsgType;
        }

        public HeadMessage(int baseMsgType, byte[] include) {
            this.protocol = amq;
            this.baseMsgType = baseMsgType;
            this.include = include;
        }

        public void encode(ByteBuffer buffer) {
            buffer.putInt(HeadMessage.amq);
            buffer.putInt(this.baseMsgType);
            buffer.putInt(this.bodyLength);
            int remainLength = head_length - buffer.position();
            if(null != this.include && this.include.length>remainLength){
                logger.warn(" head mini message length is OVERLOAD!");
            }
            if(null != this.include && this.include.length <= remainLength){
                buffer.put(this.include);
            }
            buffer.position(HeadMessage.head_length);
        }

        public static HeadMessage decode(ByteBuffer buffer) {
            HeadMessage head = new HeadMessage();
            head.setProtocol(buffer.getInt()); //amq
            head.setBaseMsgType(buffer.getInt());
            head.setBodyLength(buffer.getInt());
            int remainLength = head_length - buffer.position();
            final byte[] include = new byte[remainLength];
            buffer.get(include);
            head.setInclude(include);
            return head;
        }

        public int getBodyLength() {
            return bodyLength;
        }

        public void setBodyLength(int bodyLength) {
            this.bodyLength = bodyLength;
        }

        public int getBaseMsgType() {
            return baseMsgType;
        }

        public void setBaseMsgType(int baseMsgType) {
            this.baseMsgType = baseMsgType;
        }

        public int getProtocol() {
            return protocol;
        }

        public void setProtocol(int protocol) {
            this.protocol = protocol;
        }

        public byte[] getInclude() {
            return include;
        }

        public void setInclude(byte[] include) {
            this.include = include;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("HeadMessage{");
            sb.append("protocol=").append(protocol);
            sb.append(", baseMsgType=").append(baseMsgType);
            sb.append(", bodyLength=").append(bodyLength);
            sb.append('}');
            return sb.toString();
        }
    }

    public static boolean isAmq(ByteBuffer buffer) {
        return (HeadMessage.amq == buffer.getInt(4));
    }

    public static boolean isHeart(ByteBuffer buffer) {
        return isHeart(HeadMessage.decode(buffer));
    }

    public static boolean isHeart(HeadMessage head) {
        return (BaseMsgType.HEART_MESSAGE_REQ == head.getBaseMsgType() || BaseMsgType.HEART_MESSAGE_RSP == head.getBaseMsgType());
    }

    public static boolean isReConnectReq(HeadMessage head) {
        return (BaseMsgType.RE_CONNECT_REQ == head.getBaseMsgType());
    }

    public static ByteBuffer encode(BaseMessage baseMessage) {
        byte[] bodyByte = ISerializer.Serializer.INST.of().toByte(baseMessage.getBody());
        int bodyLength = null == bodyByte ? 0 : bodyByte.length;
        ByteBuffer buffer = ByteBuffer.allocate(HeadMessage.head_length + bodyLength);
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
        if (buffer.remaining() < BaseMessage.HeadMessage.head_length) {
            return null;
        }
        BaseMessage message = new BaseMessage();
        HeadMessage head = HeadMessage.decode(buffer);
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
            HeadMessage.decode(buffer);
        }
        buffer.position(HeadMessage.head_length);
        byte[] bodyByte = Buffers.take(buffer, bodyLength);
        Message body = ISerializer.Serializer.INST.of().getObj(bodyByte, Message.class);
        return body;
    }

    //=========================================

    public HeadMessage getHead() {
        return head;
    }

    public void setHead(HeadMessage head) {
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
