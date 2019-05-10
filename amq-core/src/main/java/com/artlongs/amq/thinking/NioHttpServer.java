package com.artlongs.amq.thinking;

import com.artlongs.amq.http.HttpHandler;
import com.artlongs.amq.http.HttpServer;
import com.artlongs.amq.http.HttpServerConfig;
import com.artlongs.amq.http.HttpServerState;
import com.artlongs.amq.http.routes.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by ${leeton} on 2018/10/26.
 */
public class NioHttpServer  {
    private static Logger logger = LoggerFactory.getLogger(NioHttpServer.class);
    ServerSocketChannel socket;
    Selector selector = null;
    HttpServerConfig config;

    public NioHttpServer(HttpServerConfig config) {
        this.config = config;
    }

    public void start() {
        Thread t = new Thread(start0());
        t.setDaemon(true);
        t.run();
    }

    private Runnable start0(){
       return new Runnable(){
            @Override
            public void run() {
                try {
                    selector = Selector.open();
                    socket = ServerSocketChannel.open();
                    socket.bind(new InetSocketAddress(config.host, config.port));
                    socket.configureBlocking(false);
                    socket.register(selector, SelectionKey.OP_ACCEPT);
                    loop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void loop() {
        try {
            while (true) {
                if (selector.select(3) == 0) {
                    continue;
                }
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isWritable() && key.isValid()) {
                        handleWrite(key);
                    }
                    if (key.isConnectable()) {
                        System.out.println("isConnectable = true");
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void shutdown() {

    }

    public void stop() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socket != null && socket.isOpen()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handler(HttpHandler httpHandler) {

    }

    public HttpHandler getHandler() {
        return null;
    }

    public HttpServerState getState() {
        return null;
    }

    public HttpServerConfig getConfig() {
        return null;
    }

    public HttpServer addController(Controller... controller) {
        return null;
    }


    public static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssChannel.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(1024));
    }

    public static void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer in = (ByteBuffer) key.attachment();
        long readCount = sc.read(in);
        while (readCount > 0) {
            in.flip();
            while (in.hasRemaining()) {
                in.get();
            }
            System.out.println();
            in.clear();
            readCount = sc.read(in);
        }
        if (readCount == -1) {
            sc.close();
        }
    }

    public static void handleWrite(SelectionKey key) throws IOException {
        ByteBuffer buf = (ByteBuffer) key.attachment();
        buf.flip();
        SocketChannel sc = (SocketChannel) key.channel();
        while (buf.hasRemaining()) {
            sc.write(buf);
        }
        buf.compact();
    }

    public static void main(String[] args) {
        new NioHttpServer(new HttpServerConfig()).start();

    }


}