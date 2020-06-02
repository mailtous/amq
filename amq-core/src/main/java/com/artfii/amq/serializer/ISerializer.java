package com.artfii.amq.serializer;


import java.nio.ByteBuffer;

public interface ISerializer {

    public <T> byte[] toByte(T obj);

    public <T> T getObj(byte[] bytes);

    public <T> T getObj(byte[] bytes, Class<T> clzz);

    public <T> T getObj(ByteBuffer buffer);

    public <T> T getObj(ByteBuffer buffer, Class<T> clzz);


    public static enum Serializer {
        INST;

        public ISerializer of() {
            return new Fst();
        }
    }
}
