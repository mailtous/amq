package com.artfii.amq.thinking;

import com.artfii.amq.tools.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Func :
 * Created by leeton on 2018/12/28.
 */
public class HandlerWithThreadPool implements Runnable,Destroyable {
    private static Logger logger = LoggerFactory.getLogger(HandlerWithThreadPool.class);

    final Selector selector;
    final SocketChannel socket;
    final SelectionKey key;
    final ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
    final ByteBuffer outputBuffer = ByteBuffer.allocate(1024);

    // 状态码，分别对应读状态，写状态和处理状态
    static final int READING = 1;
    static final int SENDING = 2;
    static final int PROCESSING = 3;
    // 初始的状态码是READING状态，因为Reactor分发任务时新建的Handler肯定是读就绪状态
    private int state = READING;

    public HandlerWithThreadPool(Selector selector,SocketChannel socket) throws IOException {
        inputBuffer.clear();
        outputBuffer.clear();
        this.selector = selector;
        this.socket = socket;

        socket.configureBlocking(false);
        key = socket.register(selector, SelectionKey.OP_READ);
        // attach(this)是为了dispatch()调用
        key.attach(this);
        // Selector.wakeup()方法会使阻塞中的Selector.select()方法立刻返回
        selector.wakeup();
    }

    @Override
    public void run() {
        if (state == READING) {
            read();
        } else if (state == SENDING) {
            outputBuffer.put("this message form server .".getBytes());
            write(outputBuffer);
            key.interestOps(SelectionKey.OP_READ);
            outputBuffer.clear();
            //
//            close();
        }
    }

    /**
     * 清除选择器及 key 的状态监听
     */
    private synchronized void  close() {
        key.cancel();
        selector.selectedKeys().clear();
        // 回收线程
        Reactor.workerPool.remove(this);
    }


    /** 判断读写数据时候完成的方法 **/
    private boolean inputIsCompelete(int read)  {
        return (-1 == read);
    }
    private boolean outputIsCompelete(int writed) {
        return true;
    }

    /**
     * 读入数据，确定通道内数据读完以后
     * 状态码要变为 PROCESSING
     * 需要特别注意的是，本方法是在Reactor线程中执行的
     *
     * @throws IOException
     */
    void read(){
        try {
            if (key.isValid() && key.isReadable()) {
                int read = socket.read(inputBuffer);
                if (inputIsCompelete(read)) {
                    IOUtils.print(inputBuffer);
                    state = PROCESSING;
                    // 在独立的线程沲里去执行业务逻辑
                    Reactor.workerPool.execute(new Processer());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(ByteBuffer buf){
        try {
            buf.flip();
            while (buf.hasRemaining()) {
                socket.write(buf);
            }
            buf.compact();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 这个内部类完全是为了使用线程池
     * 这样就可以实现数据的读写在主线程内
     * 而对数据的处理在其他线程中完成
     */
    class Processer implements Runnable {
        public void run() {
            processAndHandOff();
        }

        /**
         * 这个方法调用了process()方法
         * 而后修改了状态码和兴趣操作集
         * 注意本方法是同步的，因为多线程实际执行的是这个方法
         * 如果不是同步方法，有可能出现异常
         */
        synchronized void processAndHandOff() {
            process();
            state = SENDING;
            key.interestOps(SelectionKey.OP_WRITE);
        }


        /** 对数据的处理类，比如HTTP服务器就会返回HTTP报文 **/
        private void process() {
            // 自己实现的服务器功能
            System.err.println(" process logic .....");
        }
    }


}
