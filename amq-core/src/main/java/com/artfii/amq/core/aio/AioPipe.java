package com.artfii.amq.core.aio;

import com.artfii.amq.tools.RingBufferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Func : 对 Aio channel 的包装
 * 不喜欢 session 这个单词,按 nProcess 项目把 channel 叫 pipe(通道),感觉更好理解
 *
 * @author: leeton on 2019/2/22.
 */
public class AioPipe<T> implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AioPipe.class);

    private Integer id;
    private AioServerConfig<T> ioServerConfig;
    private boolean flowControl; //客户端写流控，服务端读流控

    protected static final byte ENABLED = 1;
    protected static final byte CLOSED = 0;
    protected static final byte CLOSING = -1;
    protected byte status = ENABLED;

    protected ByteBuffer readBuffer; //读缓冲
    protected ByteBuffer writeBuffer; //写缓冲
    private RingBufferQueue<ByteBuffer> writeCacheQueue; //缓存队列

    private Semaphore writeSemaphore = new Semaphore(1, true);
    private Semaphore readSemaphore = new Semaphore(1, true);

    private Writer writer = new Writer();
    private Reader reader = new Reader();

    protected AsynchronousSocketChannel channel;
    private Object attachment; //附件对象(通常传输文件才用得到)

    private AioClient aioClient;


    public AioPipe() {
    }

    public AioPipe(AsynchronousSocketChannel channel, AioServerConfig config) {
        this.channel = channel;
        this.ioServerConfig = config;
        this.writeCacheQueue = new RingBufferQueue<ByteBuffer>(config.getQueueSize());
        //初始化状态机
        config.getProcessor().stateEvent(this, State.NEW_PIPE, null);
        this.readBuffer = DirectBufferUtil.allocateDirectBuffer(config.getDirctBufferSize());
        this.id = hashCode();
    }

    /**
     * 初始化AioSession
     */
    public void initSession() {
        continueRead();
    }

    /**
     * 输出数据。
     * <p>必须实现{@link Protocol#encode(Object)}</p>方法
     *
     * @param t 待输出数据必须为当前服务指定的泛型
     * @throws IOException
     */
    public final boolean write(T t) {
        return writeBuffer(ioServerConfig.getProtocol().encode(t));
    }

    public final boolean writeBuffer(ByteBuffer buffer) {
        boolean succ = writeToCacheQueue(buffer);
        if (succ) {
            writeToChannel();
        }
        return succ;
    }

    /**
     * 将数据写入缓冲队列。
     *
     * @param buffer
     */
    private final boolean writeToCacheQueue(final ByteBuffer buffer) {
        if (isClose()) {
            logger.error("pipe({}) is " + (status == CLOSED ? "closed" : "invalid"), getId());
            return false;
        }
        if (!buffer.hasRemaining()) {
            logger.error("buffer has no remaining");
            return false;
        }

        // buffer 写入到队列缓存
        int size = writeCacheQueue.put(buffer);
//        logger.debug("[AIO]write queue index of:({})", size);
        // 流控
/*        if (size >= ioServerConfig.getFlowLimitLine() && ioServerConfig.isServer()) {
            logger.warn("[AIO]写入的数据大多,触发了流控.SIZE:({})", size);
            flowControl = true;
            ioServerConfig.getProcessor().stateEvent(this, State.FLOW_LIMIT, null);
        }*/
        return size >= 0;
    }


    /**
     * 触发AIO的写操作,将数据 buffer 输出至网络对端。
     *
     * <p>需要调用控制同步</p>
     * <p>若当前 writeBuffer 存在数据，则立即输出buffer.</p>
     * <p>若缓冲队列(writeCacheQueue)为空,说明数据已传输完毕,则清空 writeBuffer 并解锁</p>
     * <p>如果存在流控并符合释放条件，则触发读操作</p>
     */
    public void writeToChannel() {
        if (isAcquireToWrite(10)) {
            // 从队列读取 buffer
            if (writeCacheQueue.size() > 0) {
                writeBuffer = writeCacheQueue.pop();
                continueWrite();
                return;
            }

            //如果存在流控,则触发读操作,一定要放在continueWrite之前
/*        if (flowControl && writeCacheQueue.size() > ioServerConfig.getReleaseLine()) {
            logger.warn("[AIO]写入的数据太多,触发了流控.");
            ioServerConfig.getProcessor().stateEvent(this, State.RELEASE_FLOW_LIMIT, null);
            flowControl = false;
            readFromChannel(false);
            tooFastNeedSleep();
        }*/

        }

    }

    private Set<Integer> reWrite = new HashSet<>(2000);

    /**
     * 内部方法：触发通道的写操作
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        if (this.isOpen()) {
            try {
                channel.write(buffer, 0L, TimeUnit.MILLISECONDS, this, writer);
            } catch (WritePendingException e) { // 发生了写并发冲突,放回队列重来
                Integer id = buffer.hashCode();
                if (isNotFoundInRewriteList(id) && id > 0) { // 控制重写次数
                    logger.warn("[AIO]WritePendingException try to write again");
                    tooFastNeedSleep();
                    writeBuffer(buffer);
                    reWrite.add(id);
                }
            }
        } else {
            writeSemaphore.release();
        }
    }

    private boolean isNotFoundInRewriteList(Integer bufferId) {
        return !reWrite.contains(bufferId);
    }

    private void clear(ByteBuffer buffer) {
        buffer.clear();
    }


    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    public void readFromChannel(boolean eof) {
        readBuffer.flip();
        while (readBuffer.hasRemaining()) {
            T dataEntry = null;
            try {
                dataEntry = ioServerConfig.getProtocol().decode(readBuffer);
            } catch (Exception e) {
                logger.error("[AIO]解码出错: " + e.getMessage(), e);
                ioServerConfig.getProcessor().stateEvent(this, State.DECODE_EXCEPTION, e);
            }
            if (null == dataEntry) {
                break;
            }

            //处理消息
            try {
                ioServerConfig.getProcessor().process(this, dataEntry);
            } catch (Exception e) {
                logger.error("[AIO]处理消息出错: " + e.getMessage(), e);
                ioServerConfig.getProcessor().stateEvent(this, State.PROCESS_EXCEPTION, e);
            }
        }

        if (eof || status == CLOSING) {
            close(false);
            ioServerConfig.getProcessor().stateEvent(this, State.INPUT_SHUTDOWN, null);
            return;
        }
        if (status == CLOSED) {
            return;
        }

        //流控达到释放条件
/*        if (writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            flowControl = false;
            writeToChannel();
        }*/

        //数据读取完毕
        readedAndUnLock();
        // 下一次读取
        continueRead();
    }

    private void readedAndUnLock() {
        readBuffer.clear();
        readSemaphore.release();
    }

    protected void continueRead() {
        if (isAcquireToRead(5)) {
            try {
                readFromChannel0(readBuffer);
            } catch (ReadPendingException e) {
                logger.warn("[AIO]ReadPendingException try to read again");
                continueRead();
            }
        }
    }

    protected void continueWrite() {
        writeToChannel0(writeBuffer);
    }

    /**
     * 内部方法：触发通道的读操作
     *
     * @param buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, reader);
    }

    public boolean isAcquireToWrite(int timeoutMS) {
        try {
            return writeSemaphore.tryAcquire(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return false;
    }

    public void releaseWriteLock() {
        writeSemaphore.release();
    }

    private boolean isAcquireToRead(int timeoutMS) {
        try {
            return readSemaphore.tryAcquire(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 强制关闭当前AioPipe。
     * <p>若此时还存留待输出的数据，则会导致该部分数据丢失</p>
     */
    public final void close() {
        close(true);
    }

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public synchronized void close(boolean immediate) {
        //status == SESSION_STATUS_CLOSED说明close方法被重复调用
        if (status == CLOSED) {
            logger.warn("pipe:{} is closed:", getId());
            writeSemaphore.release();
            return;
        }
        status = immediate ? CLOSED : CLOSING;
        if (immediate) {
            try {
                channel.shutdownInput();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                channel.shutdownOutput();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("close pipe exception", e);
            }
            try {
                ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSED, null);
            } finally {
                writeSemaphore.release();
            }
            DirectBufferUtil.freeFirstBuffer(readBuffer);
            if (writeBuffer != null && writeBuffer.isDirect()) {
                DirectBufferUtil.freeFirstBuffer(writeBuffer);
            }
        } else if ((null == writeBuffer || !writeBuffer.hasRemaining()) && (null == writeCacheQueue || writeCacheQueue.size() == 0) && writeSemaphore.tryAcquire()) {
            close(true);
        } else {
            ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSING, null);
        }
    }

    /**
     * 获取当前 pipe 的唯一标识
     */
    public final Integer getId() {
        return id;
    }

    /**
     * 当前会话是否已失效
     */
    public final boolean isClose() {
        return status != ENABLED;
    }

    public final boolean isOpen() {
        return status == ENABLED;
    }

    /**
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public final InetSocketAddress getLocalAddress() {
        try {
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public final InetSocketAddress getRemoteAddress() {
        try {
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    private void assertChannel() throws IOException {
        if (status == CLOSED || null == channel) {
            throw new IOException("pipe is closed");
        }
    }

    AioServerConfig<T> getServerConfig() {
        return this.ioServerConfig;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public Object getAttachment() {
        return attachment;
    }

    public AioPipe<T> setAttachment(Object attachment) {
        this.attachment = attachment;
        return this;
    }

    public void setAioClient(AioClient aioClient) {
        this.aioClient = aioClient;
    }

    public AioClient getAioClient() {
        return aioClient;
    }

    public AioPipe reConnect(){
        readedAndUnLock();
       return this.aioClient.reConnect();
    }

    /**
     * 清空 WriteBuffer 并解锁
     */
    public void clearWriteBufferAndUnLock() {
        if (this.writeBuffer != null && this.writeBuffer.isDirect()) {
            DirectBufferUtil.freeFirstBuffer(writeBuffer);
        }
        this.writeBuffer = null;
        this.writeSemaphore.release();
    }

    private void tooFastNeedSleep() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AioPipe{");
        sb.append("id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
