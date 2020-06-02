package com.artfii.amq.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Func : RingBuffer Queue, 自动扩容
 *
 * @author: leeton on 2019/2/26.
 */
public class RingBufferQueue<T> implements Iterable {
    private static Logger logger = LoggerFactory.getLogger(RingBufferQueue.class);
    private static int DEFAULT_SIZE = 2000;
    private T[] items;
    private int head = 0;
    private int tail = 0;
    private int capacity = DEFAULT_SIZE;
    private int realSize = 0; //实际的 item 个数
    private final Semaphore readLock = new Semaphore(1, true);
    private final Semaphore writeLock = new Semaphore(1, true);
    private int id;

    public RingBufferQueue() {
        this.capacity = DEFAULT_SIZE;
        this.items = (T[]) Array.newInstance(Object.class, DEFAULT_SIZE);
        this.id = hashCode();
    }

    /**
     * capacity 容量一定要设定为2的倍数
     *
     * @param capacity
     */
    public RingBufferQueue(int capacity) {
        if (capacity <= 0) capacity = DEFAULT_SIZE;
        if (capacity % 2 != 0) throw new UnsupportedOperationException("RBQ(capacity),容量一定要设定为2的倍数.");
        this.capacity = capacity;
        DEFAULT_SIZE = capacity;
        this.items = (T[]) Array.newInstance(Object.class, capacity);
        init();
    }

    private void init() {
        for (T item : items) {
            item = null;
        }
    }

    public Boolean isEmpty() {
        return (tail == head) && (tail == 0);
    }

    public Boolean isNotEmpty() {
        return !isEmpty();
    }

    public Boolean full() {
        return tail == capacity;
    }

    public void clear() {
        if (readLock.tryAcquire()) {
            Arrays.fill(items, null);
            this.head = 0;
            this.tail = 0;
            readLock.release();
        }
    }

    public int size() {
        return (realSize = tail - head);
    }

    public int capacity() {
        return capacity;
    }

    public int put(T v) {
        if (writeLock.tryAcquire()) {
            if (full()) {
                grow();
            }
            final int current = tail;
            items[current] = v;
            tail++;
            size();
            writeLock.release();
            return current;
        }
        return -1;
    }

    /**
     * FIFO 并把取出的位置设为 NULL
     *
     * @return
     */
    public T pop() {
        if (readLock.tryAcquire()) {
            if (isEmpty()) {
                return null;
            }
            T item = items[head];
            remove(head);
            head++;
            size();
            readLock.release();
            return item;
        }
        return null;
    }

    public T get(int index) {
        if (index >= size() || index < 0) throw new UnsupportedOperationException("RingBufferQueue,队列上标或下标溢出.");
        T item = items[index];
        return item;
    }

    public Result putIfAbsent(T v) {
        boolean found = false;
        int oldIndex = 0;
        int _index = 0;
        final Iterator iter = iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o != null && o.equals(v)) {
                found = true;
                oldIndex = _index;
                break;
            }
            _index++;
        }
        if (!found) {
            return new Result(true, put(v));
        }
        return new Result(false, oldIndex);
    }

    /**
     * 实际是把值设为 NULL,但 BOX 还是存在的.
     *
     * @param index
     */
    public void remove(int index) {
        if (index < 0) throw new UnsupportedOperationException("RingBufferQueue,队列上标溢出.");
        items[index] = null;
    }

    /**
     * 扩容
     */
    private void grow() {
        final int oldCapacity = items.length;
        final int newCapacity = capacity << 1; //扩容一倍
        if (newCapacity >= Integer.MAX_VALUE) {
            downCap();
            return;
        }
        try {
            final T[] newElementData = (T[]) Array.newInstance(Object.class, newCapacity);
            System.arraycopy(items, 0, newElementData, 0, oldCapacity);
            this.items = newElementData;
            this.capacity = newCapacity;
        } catch (NegativeArraySizeException e) {
            logger.error("oldCapacity:({}),newCapacity:({})", oldCapacity, newCapacity);
            e.printStackTrace();
        }
    }

    /**
     * 超过 Integer.MAX_VALUE ,执行缩小容量
     */
    private void downCap() {
        final int oldCapacity = items.length;
        final int newCapacity = DEFAULT_SIZE << 1;
        final T[] newElementData = (T[]) Array.newInstance(Object.class, newCapacity);
        System.arraycopy(items, oldCapacity - DEFAULT_SIZE, newElementData, 0, DEFAULT_SIZE);
        this.items = newElementData;
        this.capacity = newCapacity;
        this.head = 0;
        this.tail = DEFAULT_SIZE;
        size();

    }

    public T[] asArray(Class<?> clazz) {
        T[] newItems = (T[]) Array.newInstance(clazz, size());
        System.arraycopy(items, 0, newItems, 0, size());
        return newItems;
    }

    public List<T> asList() {
        final List<T> list = new ArrayList<>(size());
        if (isEmpty()) return list;
        for (T item : items) {
            list.add(item);
        }
        return list;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index;

            @Override
            public boolean hasNext() { // 因为是队列,为 true 时,值也可能为 null , 这里只是判断是否存在这个 box
                if (realSize > 0) {
                    return index < size();
                }
                return false;
            }

            @Override
            public T next() {
                if (index < size()) {
                    return items[index++];
                }
                return null;
            }
        };
    }

    public void remove(T v) {
        for (int i = 0; i < items.length; i++) {
            if (v.equals(items[i])) {
                remove(i);
                break;
            }
        }
    }

    public static class Result {
        public boolean success;
        public int index; // 在队列里存放的位置

        public Result(boolean success, int index) {
            this.success = success;
            this.index = index;
        }
    }

    private boolean tryAcquire(int timeoutMS) {
        try {
            return readLock.tryAcquire(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        RingBufferQueue<ByteBuffer> rbq = new RingBufferQueue<>(2);
        for (int i = 0; i < 10; i++) {
            rbq.put(ByteBuffer.wrap(String.valueOf(i).getBytes()));
        }

        System.err.println(rbq.size());
//        rbq.pop();

        Iterator<ByteBuffer> iter = rbq.iterator();
        while (iter.hasNext()) {
            System.err.println(new String(iter.next().array()));
        }

/*        for (ByteBuffer o : rbq.asArray(ByteBuffer.class)) {
            System.err.println(new String(o.array()));
        }*/

    }


}
