package com.artfii.amq.tools;

import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.tools.io.Pool;
import com.artfii.amq.core.aio.DirectBufferUtil;
import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.tools.io.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Func : NIO Untils
 * Created by leeton on 2018/12/26.
 */
public class IOUtils {
    private static Logger logger = LoggerFactory.getLogger(IOUtils.class);
    private static ISerializer serializer = ISerializer.Serializer.INST.of();

    public static void print(ByteBuffer buf){
        if (null != buf && buf.remaining() >0) {
            buf.flip();
            buf.get();
            logger.info("[REC]:"+new StringBuilder(StandardCharsets.UTF_8.decode(buf)).toString());
        }
    }

    public static void print(byte[] bytes) {
        if (null != bytes) {
            logger.info("[REC]:" + new String(bytes));
        }
    }

    public static ByteBuffer wrap(byte[] bytes) {
        ByteBuffer buffer = DirectBufferUtil.allocateDirectBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer wrap(Object obj) {
        byte[] bytes = serializer.toByte(obj);
        return wrap(bytes);
    }

    /**
     * NIO 读取数据
     * @param key
     * @return
     */
     public static byte[] read(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer in = (ByteBuffer) key.attachment();
        if (in != null) {
            try {
                int size = sc.read(in);
                if (size > 0) {
                    byte[] bytes = new byte[size];
                    in.flip();
                    for (int i = 0; i < size; i++) {
                        byte b = in.get();
                        bytes[i] = b;
                    }
                    in.clear();
                    return bytes;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * NIO 写数据
     * @param key
     * @param attachment
     */
    public static boolean write(SelectionKey key,byte[] attachment) {
        SocketChannel sc = (SocketChannel)key.channel();
        try {
            if(sc.isOpen()){
//                ByteBuffer buffer = ByteBuffer.allocate(8192);
                ByteBuffer buffer = Pool.MEDIUM_DIRECT.allocate().getResource();// 分配外部 direct buffer
                buffer.put(attachment);
                buffer.position(0);
                buffer.limit(attachment.length);
                sc.write(buffer);
                // sc.register 有 BUG , 一定要把buffer传入,并且加上 buffer.clear(),客户端才能收到消息
                sc.register(key.selector(), SelectionKey.OP_READ,buffer);
                buffer.clear();
                return true;

            }
        } catch (IOException e) {
            logger.error("[x] connection to {} is broken.",getRemoteAddress(sc));
            closeChannel(sc);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Aio write data
     * @param client
     * @param buffer
     */
    public static boolean write(AsynchronousSocketChannel client, ByteBuffer buffer) {
        try {
            buffer.rewind();
            client.write(buffer, null, newWriteHandler());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static CompletionHandler newWriteHandler(){
        CompletionHandler writeHandler = new CompletionHandler() {
            @Override
            public void completed(Object result, Object attachment) {

            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        };
        return writeHandler;
    }



    public static void closeChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeChannel(AsynchronousSocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getRemoteAddress(SocketChannel channel){
        try {
           return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getLocalAddress(SocketChannel channel){
        try {
            return channel.getLocalAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static InetSocketAddress getRemoteAddress(NetworkChannel channel) {
        try {
            return (InetSocketAddress)((AsynchronousSocketChannel)channel).getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRemoteAddressStr(NetworkChannel channel) {

        return getRemoteAddress(channel).toString();
    }

    public static ExecutorService createFixedThreadPool(int threadSize, String threadName) {
        return Executors.newFixedThreadPool(threadSize, new ThreadFactory() {
            byte index = 0;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName + (++index));
            }
        });
    }



}
