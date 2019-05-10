package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.Protocol;
import com.artlongs.amq.serializer.ISerializer;

import java.nio.ByteBuffer;

/**
 * Func : Mq 协议
 *
 * @author: leeton on 2019/2/22.
 */
public class MqProtocol implements Protocol<Message> {
   private static ISerializer serializer = ISerializer.Serializer.INST.of();
    private static int INT_LENGTH = 4;

    @Override
    public ByteBuffer encode(Message message) {
        byte[] bytes = serializer.toByte(message);
        ByteBuffer buffer = ByteBuffer.allocate(INT_LENGTH + bytes.length);
        buffer.putInt(INT_LENGTH + bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public Message decode(ByteBuffer readBuffer) {
        //识别消息长度
        if (readBuffer.remaining() < INT_LENGTH) {
            return null;
        }
        //判断是否存在半包情况
        int len = readBuffer.getInt(0);
        if (readBuffer.remaining() < len) {
            return null;
        }
        readBuffer.getInt();//跳过length字段
        byte[] bytes = new byte[len - INT_LENGTH];
        readBuffer.get(bytes);
        return serializer.getObj(bytes);
    }


}
