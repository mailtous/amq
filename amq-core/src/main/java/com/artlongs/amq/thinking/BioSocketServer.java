package com.artlongs.amq.thinking;

/**
 * Func :
 *
 * @author: leeton on 2019/3/11.
 */

import com.artlongs.amq.core.MqConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于BIO的Socket服务端进程
 *
 * @author shirdrn
 */
public class BioSocketServer extends Thread {

    /** 服务端口号 */
    private int port = 8888;
    /** 为客户端分配编号  */
    private static int sequence = 0;
    /** 处理客户端请求的线程池 */
    private ExecutorService pool;

    public BioSocketServer(int port, int poolSize) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void run() {
        Socket socket = null;
        int counter = 0;
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            boolean flag = false;
            Date start = null;
            while(true) {
                socket = serverSocket.accept(); // 监听
                // 有请求到来才开始计时
                if(!flag) {
                    start = new Date();
                    flag = true;
                }
                // 将客户端请求放入线程池处理
                pool.execute(new RequestHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 客户端请求处理线程类
     *
     * @author shirdrn
     */
    class RequestHandler implements Runnable {

        private Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream(); // 流：客户端->服务端（读）
                OutputStream out = socket.getOutputStream(); // 流：服务端->客户端（写）
                byte[] receiveBuffer = new byte[128];
                String clientMessage = "";
                int receiveBytes = in.read(receiveBuffer);
                if(receiveBytes != -1) {
                    clientMessage = new String(receiveBuffer, 0, receiveBytes);
                    if(clientMessage.startsWith("I am the client")) {
                        String serverResponseWords =
                                "I am the server, and you are the " + (++sequence) + "th client.";
                        out.write(serverResponseWords.getBytes());
                    }
                }
                out.flush();
                System.out.println("Server: receives clientMessage->" + clientMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException{
        BioSocketServer serverSocket = new BioSocketServer(MqConfig.inst.port, 20);
        serverSocket.run();
    }
}