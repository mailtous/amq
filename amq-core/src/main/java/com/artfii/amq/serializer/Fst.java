package com.artfii.amq.serializer;


import com.artfii.amq.core.Message;
import com.artfii.amq.core.Subscribe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.http.Render;
import com.artfii.amq.tester.TestUser;
import com.artfii.amq.tools.io.Buffers;
import org.nustaq.serialization.FSTConfiguration;

import java.nio.ByteBuffer;

public class Fst implements ISerializer {
    FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();
    FSTConfiguration fst_json = FSTConfiguration.createJsonConfiguration();
    public Fst() {
        fst.registerClass(BaseMessage.class);
        fst.registerClass(BaseMessage.Head.class);
        fst.registerClass(Message.class);
        fst.registerClass(Message.Stat.class);
        fst.registerClass(Message.Type.class);
        fst.registerClass(Message.Listen.class);
        fst.registerClass(Message.Life.class);
        fst.registerClass(Message.ON.class);
        fst.registerClass(Subscribe.class);
        fst.registerClass(Render.class);
        fst.registerClass(Render.Fmt.class);
        fst.registerClass(TestUser.class);
    }

    public <T> byte[] toByte(T obj) {
        if (null == obj) return null;
        return fst.asByteArray(obj);
    }

    public <T> T getObj(byte[] bytes) {
        if (null == bytes || bytes.length == 0) return null;
        T obj = (T) fst.asObject(bytes);
        return obj;
    }

    @Override
    public <T> T getObj(byte[] bytes, Class<T> clzz) {
        return getObj(bytes);
    }

    @Override
    public <T> T getObj(ByteBuffer byteBuffer) {
        byte[] bytes = Buffers.take(byteBuffer);
        return getObj(bytes);
    }

    @Override
    public <T> T getObj(ByteBuffer byteBuffer, Class<T> clzz) {
        return getObj(byteBuffer);
    }

    public String toJson(Object object) {
       return fst_json.asJsonString(object);
    }

    public Object fromJson(String json) {
        return fst_json.asObject(json.getBytes());
    }

    public static void main(String[] args) {
        Fst fst = new Fst();
        Message msg = Message.buildCommonMessage("hello", new TestUser(1, "alice"), 127);
        byte[] jsonBytes = fst.toByte(msg);
        System.err.println(new String(jsonBytes));
        String jsonStr = fst.fst_json.asJsonString(msg);
        System.err.println(jsonStr);
        //
        long s = System.currentTimeMillis();
        int nums = 1_000;
        for (int i = 0; i < nums; i++) {
            fst.getObj(jsonBytes);
        }
        System.err.println("byte de used times(ms):" + (System.currentTimeMillis() - s));
        //
        byte[] jsonStrBtyes = jsonStr.getBytes();
        long s2 = System.currentTimeMillis();
        for (int i = 0; i < nums; i++) {
            fst.fst_json.asObject(jsonStrBtyes);
        }
        System.err.println("json de used times(ms):" + (System.currentTimeMillis() - s2));
    }

}
