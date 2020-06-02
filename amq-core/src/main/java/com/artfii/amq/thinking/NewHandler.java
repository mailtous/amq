package com.artfii.amq.thinking;

import com.artfii.amq.tools.IOUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 * Created by leeton on 2018/12/28.
 */

public class NewHandler implements Runnable {

    private SocketChannel socketChannel;
    private SelectionKey selectionKey;
    private ByteBuffer oldBuffer;
    private static final ExecutorService workerPool = Executors.newFixedThreadPool(4);

    /**
     * 这里使用了状态码来防止多线程出现数据不一致等问题
     **/
    static final int PROCESSING = 1;
    static final int PROCESSED = 2;
    private volatile int state = PROCESSED;

    public NewHandler(SocketChannel socketChannel, Selector selector) throws IOException {

        // 初始化的oldBuffer为null
        oldBuffer = null;
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);

        // 在构造函数里就注册通道到Selector
        this.selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        // attach(this)将自身对象绑定到key上，作用是使dispatch()函数正确使用
        selectionKey.attach(this);
        // Selector.wakeup()方法会使阻塞中的Selector.select()方法立刻返回
        selector.wakeup();
    }

    // 使用线程池执行
    @Override
    public void run() {
        if (state == PROCESSED) {
            // 如果此时没有线程在处理该通道的本次读取，就提交申请到线程池进行读写操作
            workerPool.execute(new process(selectionKey));
        } else {
            // 如果此时有线程正在进行读写操作，就直接return，选择器会进行下一次选择和任务分派
            return;
        }
    }

    /**
     * 内部类实现对通道数据的读取处理和发送
     *
     * @author CringKong
     */
    private class process implements Runnable {

        private SelectionKey selectionKey;

        public process(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
            state = PROCESSING;
        }

        @Override
        public void run() {
            try {
                assignJob(selectionKey);
            } catch (IOException | InterruptedException e) {
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        /**
         * FUNC: 分配任务
         *
         * 这是一个同步方法，因为在reactor中的选择器有可能会出现一种状况：
         * 当process线程已经要对某通道进行读写的时候，有可能Selector会再次选择该通道
         * 因为此时该process线程还并没有真正的进行读写，会导致另一线程重新创建一个process
         * 并试图进行读写操作，此时就会出现cpu资源浪费的情况，或者出现异常，因为线程1在读取通道内容的时候
         * 线程2就会被阻塞，而等到线程2执行操作的时候，线程1已经对通道完成了读写操做
         * 因此可以通过设置对象状态码来防止出现这些问题
         *
         * @param selectionKey
         * @throws IOException
         * @throws InterruptedException
         */
        private synchronized void assignJob(SelectionKey selectionKey) throws IOException, InterruptedException {

            ByteBuffer newBuffer = ByteBuffer.allocate(64);

            int read;
            while ((read = socketChannel.read(newBuffer)) <= 0) {
                state = PROCESSED;
                return;
            }

            IOUtils.print(newBuffer);

            //TODO: 调用 mq 主题匹配,把内容发送给匹配上的服务器做处理.

           /* newBuffer.flip();
            String line = readLine(newBuffer);
            if (line != null) {

                // 如果这次读到了行结束符，就将原来不含有行结束符的buffer合并位一行
                String sendData = readLine(mergeBuffer(oldBuffer, newBuffer));
                if (readLineContent(sendData).equalsIgnoreCase("exit")) { // 如果这一行的内容是exit就断开连接
                    socketChannel.close();
                    state = PROCESSED;
                    return;
                }
                // 然后直接发送回到客户端
                System.err.println("[show]:"+sendData);
                ByteBuffer sendBuffer = ByteBuffer.wrap(sendData.getBytes("utf-8"));
                while (sendBuffer.hasRemaining()) {
                    socketChannel.write(sendBuffer);
                }
                oldBuffer = null;
            } else {
                // 如果这次没读到行结束付，就将这次读的内容和原来的内容合并
                oldBuffer = mergeBuffer(oldBuffer, newBuffer);
            }*/

        }



    }



    /**
     * 读取ByteBuffer直到一行的末尾 返回这一行的内容，包括换行符
     *
     * @param buffer
     * @return String 读取到行末的内容，包括换行符 ; null 如果没有换行符
     * @throws UnsupportedEncodingException
     */
    private static String readLine(ByteBuffer buffer) throws UnsupportedEncodingException {
        // windows中的换行符表示手段 "\r\n"
        // 基于windows的软件发送的换行符是会是CR和LF
        char CR = '\r';
        char LF = '\n';

        boolean crFound = false;
        int index = 0;
        int len = buffer.limit();
        buffer.rewind();
        while (index < len) {
            byte temp = buffer.get();
            if (temp == CR) {
                crFound = true;
            }
            if (crFound && temp == LF) {
                // Arrays.copyOf(srcArr,length)方法会返回一个 源数组中的长度到length位 的新数组
                return new String(Arrays.copyOf(buffer.array(), index + 1), "utf-8");
            }
            index++;
        }
        return null;
    }

    /**
     * 获取一行的内容，不包括换行符
     *
     * @param line
     * @return String 行的内容
     * @throws UnsupportedEncodingException
     */
    private String readLineContent(String line) throws UnsupportedEncodingException {
        System.out.print(line);
        System.out.print(line.length());
        return line.substring(0, line.length() - 2);
    }

    /**
     * 对传入的Buffer进行拼接
     *
     * @param oldBuffer
     * @param newBuffer
     * @return ByteBuffer 拼接后的Buffer
     */
    public static ByteBuffer mergeBuffer(ByteBuffer oldBuffer, ByteBuffer newBuffer) {
        // 如果原来的Buffer是null就直接返回
        if (oldBuffer == null) {
            return newBuffer;
        }
        // 如果原来的Buffer的剩余长度可容纳新的buffer则直接拼接
        newBuffer.rewind();
        if (oldBuffer.remaining() > (newBuffer.limit() - newBuffer.position())) {
            return oldBuffer.put(newBuffer);
        }

        // 如果不是以上两种情况就构建新的Buffer进行拼接
        int oldSize = oldBuffer != null ? oldBuffer.limit() : 0;
        int newSize = newBuffer != null ? newBuffer.limit() : 0;
        ByteBuffer result = ByteBuffer.allocate(oldSize + newSize);

        result.put(Arrays.copyOfRange(oldBuffer.array(), 0, oldSize));
        result.put(Arrays.copyOfRange(newBuffer.array(), 0, newSize));

        return result;
    }


}
