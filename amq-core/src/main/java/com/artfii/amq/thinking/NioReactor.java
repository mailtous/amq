package com.artfii.amq.thinking;

import com.artfii.amq.core.MqConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Func :
 * Created by leeton on 2018/12/28.
 */
public class NioReactor {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public NioReactor(String address,int port) throws IOException {
        // 初始化Selector和Channel，并完成注册
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(address,port));
        SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new Acceptor());
    }

    /**
     * 轮询分发任务
     * @throws IOException
     */
    private void dispatchLoop() throws IOException {
        while(true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while(iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                dispatchTask(selectionKey);
            }
            selectionKeys.clear();
        }
    }

    /**
     * 任务分派器的进阶版，耦合性降低，拓展性增强
     * 子类只需要实现Runnable接口，并重写run()方法，就可以实现多种任务的无差别分派
     *
     * @param taskSelectionKey
     */
    private void dispatchTask(SelectionKey taskSelectionKey) {
        Runnable runnable = (Runnable)taskSelectionKey.attachment();
        if (runnable != null) {
            runnable.run();
        }
    }

    /**
     * Accept类，实际TCP连接的建立和SocketChannel的获取在这个类中实现
     * 根据类的实现，可以发现一个Accept类对应一个 serverSocketChannel.accept()
     *
     * @author CringKong
     *
     */
    private class Acceptor implements Runnable{
        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    // 创建一个新的处理类
                    new NewHandler(socketChannel, selector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    public static void main(String[] args) {
        NioReactor reactor;
        try {
            reactor = new NioReactor(MqConfig.inst.host, MqConfig.inst.port);
            reactor.dispatchLoop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
