package com.artlongs.amq.tools;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 这个队列实际上用了读写互斥锁,以避免发生并发读写的IO错误
 */
public final class FastBlockingQueue {

    private final ByteBuffer[] items;
    private final ReentrantLock lock = new ReentrantLock(false);
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    int takeIndex;
    int putIndex;
    int count;
    int remaining;

    public FastBlockingQueue(int capacity) {
        this.items = new ByteBuffer[capacity];
    }

    private void enqueue(ByteBuffer x) {
        items[putIndex] = x;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
        remaining += x.remaining();
        notEmpty.signal();
    }


    private ByteBuffer dequeue() {
        ByteBuffer x = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        remaining -= x.remaining();
        notFull.signal();
        return x;
    }

    public int expectRemaining(int maxSize) {
        lock.lock();
        try {
            if (remaining <= maxSize || count == 1) {
                return remaining;
            }

            int takeIndex = this.takeIndex;
            int preCount = 0;
            int remain = items[takeIndex].remaining();
            while (remain <= maxSize) {
                remain += (preCount = items[++takeIndex % items.length].remaining());
            }
            return remain - preCount;
        } finally {
            lock.unlock();
        }
    }


    public int put(ByteBuffer buffer) {
        try {
            lock.lockInterruptibly();
            while (count == items.length) {
                notFull.await();
            }
            enqueue(buffer);
//            System.err.println("putindex="+putIndex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return count;
    }

    public ByteBuffer pop() {
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public void popInto(ByteBuffer destBuffer) {
        lock.lock();
        try {
            while (destBuffer.hasRemaining()) {
                destBuffer.put(dequeue());
            }
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty(){
        return size()==0;
    }
    public boolean isNotEmpty(){
        return size()>0;
    }
}