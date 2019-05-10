package com.artlongs.amq.thinking;

import com.artlongs.amq.core.MqConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Func :
 * Created by leeton on 2018/12/25.
 */
public class NioServer {

    //通道管理器
    private Selector selector;

    //获取一个ServerSocket通道，并初始化通道
    public NioServer init(int port) throws IOException {
        //获取一个ServerSocket通道
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));
        //获取通道管理器
        selector=Selector.open();
        //将通道管理器与通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件，
        //只有当该事件到达时，Selector.select()会返回，否则一直阻塞。
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        return this;
    }

    public void listen() throws IOException{
        System.out.println("服务器端启动成功");

        //使用轮询访问selector
        while(true){
            //当有注册的事件到达时，方法返回，否则阻塞。
            selector.select();

            //获取selector中的迭代器，选中项为注册的事件
            Iterator<SelectionKey> ite=selector.selectedKeys().iterator();

            while(ite.hasNext()){
                SelectionKey key = ite.next();
                //删除已选key，防止重复处理
                ite.remove();
                //客户端请求连接事件
                if(key.isAcceptable()){
                    ServerSocketChannel server = (ServerSocketChannel)key.channel();
                    //获得客户端连接通道
                    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    //向客户端发消息
//                    channel.write(ByteBuffer.wrap(new String("send message to client").getBytes()));
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    buffer.position(0);
                    buffer.limit(0);

                    channel.write(buffer);
                    //在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件。
                    channel.register(selector, SelectionKey.OP_READ);

                    System.out.println("客户端请求连接事件");
                }else if(key.isReadable()){//有可读数据事件
                    //获取客户端传输数据可读取消息通道。
                    SocketChannel channel = (SocketChannel)key.channel();
                    //创建读取数据缓冲器
                    ByteBuffer buffer = ByteBuffer.allocate(10);

                    int read = channel.read(buffer);
                    byte[] data = buffer.array();
                    String message = new String(data);

                    System.out.println("receive message from client, size:" + buffer.position() + " msg: " + message);
//                    ByteBuffer outbuffer = ByteBuffer.wrap(("server.".concat(msg)).getBytes());
//                    channel.write(outbuffer);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer().init(MqConfig.inst.port).listen();
    }
}
