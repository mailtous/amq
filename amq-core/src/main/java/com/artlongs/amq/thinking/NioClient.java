package com.artlongs.amq.thinking;

import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.tools.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Func :
 *
 * @author: leeton on 2019/1/24.
 */
public class NioClient {

    //管道管理器
    private Selector selector;
    private SocketChannel channel;

    public NioClient(String serverIp, int port) throws IOException {
        //获取socket通道
        channel = SocketChannel.open();

        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(serverIp, port));
        //获得通道管理器
        selector = Selector.open();
        //为该通道注册SelectionKey.OP_CONNECT事件
        channel.register(selector, SelectionKey.OP_CONNECT);
    }


    public void listen() throws IOException {
        System.out.println("客户端启动");
        //轮询访问selector
        while (true) {
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;
            Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    //客户端连接服务器，需要调用channel.finishConnect(),才能实际完成连接。
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    }
/*                    Message.Key mKey = new Message.Key(ID.ONLY.id(), "quenu");
                    Message<Message.Key, String> message = Message.ofDef(mKey, " This message from clinet .");
                    IOUtils.write(key, json.toByte(message));*/
                    ByteBuffer buffer = ByteBuffer.allocate(0);
//                    buffer.limit(1);
//                    buffer.position(0);
                    IOUtils.write(key,buffer.array());
                } else if (key.isValid() && key.isReadable()) { //有可读数据事件。
                    IOUtils.print(IOUtils.read(key));
                }
                // 删除已选的key，防止重复处理
                ite.remove();
            }
        }
    }

    public void send(Object content) {

    }


    public static void main(String[] args) throws IOException {
        NioClient client = new NioClient(MqConfig.inst.host, MqConfig.inst.port);

        client.listen();


    }
}


