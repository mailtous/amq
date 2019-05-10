package com.artlongs.amq.core.aio;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

public enum DirectBufferUtil {
    INST;
    private static final int TEMP_BUF_POOL_SIZE = 1024;
    private static final long MAX_CACHED_BUFFER_SIZE = getMaxCachedBufferSize();
    private ThreadLocal<BufferCache> bufferCache;

    DirectBufferUtil() {
        bufferCache = new BufferCacheThreadLocal();
    }


    /**
     * 分配外部 buffer
     *
     * @param size
     * @return
     */
    public static ByteBuffer allocateDirectBuffer(int size) {
        if (isBufferTooLarge(size)) {
            return ByteBuffer.allocateDirect(size);
        } else {
            DirectBufferUtil.BufferCache currentThreadCache = DirectBufferUtil.INST.bufferCache.get();
            ByteBuffer buffer = currentThreadCache.get(size);
            if (buffer != null) {
                return buffer;
            } else {
                if (!currentThreadCache.isEmpty()) {
                    buffer = currentThreadCache.removeFirst();
                    free(buffer);
                }

                return ByteBuffer.allocateDirect(size);
            }
        }
    }

    public static void freeFirstBuffer(ByteBuffer buffer) {
        if (isBufferTooLarge(buffer)) {
            free(buffer);
        } else {
            assert buffer != null;

            DirectBufferUtil.BufferCache currentThreadCache = DirectBufferUtil.INST.bufferCache.get();
            if (!currentThreadCache.offerFirst(buffer)) {
                free(buffer);
            }

        }
    }

    private static long getMaxCachedBufferSize() {
/*        String var0 = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("jdk.nio.maxCachedBufferSize");
            }
        });*/
        String var0 = System.getProperty("jdk.nio.maxCachedBufferSize");
        if (var0 != null) {
            try {
                long var1 = Long.parseLong(var0);
                if (var1 >= 0L) {
                    return var1;
                }
            } catch (NumberFormatException var3) {
                ;
            }
        }

        return 9223372036854775807L;
    }

    private static boolean isBufferTooLarge(int size) {
        return (long) size > MAX_CACHED_BUFFER_SIZE;
    }

    private static boolean isBufferTooLarge(ByteBuffer buffer) {
        if (buffer != null) {
            return isBufferTooLarge(buffer.capacity());
        }
        return false;
    }


    private static void free(ByteBuffer buffer) {
        ((DirectBuffer) buffer).cleaner().clean();
    }


    public static class BufferCache {
        private ByteBuffer[] buffers;
        private int count;
        private int start;

        public BufferCache() {
            this.buffers = new ByteBuffer[DirectBufferUtil.TEMP_BUF_POOL_SIZE];
        }

        private int next(int size) {
            return (size + 1) % DirectBufferUtil.TEMP_BUF_POOL_SIZE;
        }

        public ByteBuffer get(int size) {
            assert !DirectBufferUtil.isBufferTooLarge(size);

            if (this.count == 0) {
                return null;
            } else {
                ByteBuffer[] var2 = this.buffers;
                ByteBuffer var3 = var2[this.start];
                if (var3.capacity() < size) {
                    var3 = null;
                    int var4 = this.start;

                    while ((var4 = this.next(var4)) != this.start) {
                        ByteBuffer var5 = var2[var4];
                        if (null == var5) {
                            break;
                        }

                        if (var5.capacity() >= size) {
                            var3 = var5;
                            break;
                        }
                    }

                    if (null == var3) {
                        return null;
                    }

                    var2[var4] = var2[this.start];
                }

                var2[this.start] = null;
                this.start = this.next(this.start);
                --this.count;
                var3.rewind();
                var3.limit(size);
                return var3;
            }
        }

        public boolean offerFirst(ByteBuffer buffer) {
            assert !DirectBufferUtil.isBufferTooLarge(buffer);

            if (this.count >= DirectBufferUtil.TEMP_BUF_POOL_SIZE) {
                return false;
            } else {
                this.start = (this.start + DirectBufferUtil.TEMP_BUF_POOL_SIZE - 1) % DirectBufferUtil.TEMP_BUF_POOL_SIZE;
                this.buffers[this.start] = buffer;
                ++this.count;
                return true;
            }
        }

        public boolean isEmpty() {
            return this.count == 0;
        }

        ByteBuffer removeFirst() {
            assert this.count > 0;

            ByteBuffer firstBuffer = this.buffers[this.start];
            this.buffers[this.start] = null;
            this.start = this.next(this.start);
            --this.count;
            return firstBuffer;
        }
    }

    public static class BufferCacheThreadLocal extends ThreadLocal<BufferCache> {
        protected BufferCache initialValue() {
            return new BufferCache();
        }
    }


}
