
package com.artlongs.amq.tools.io;

import java.nio.ByteBuffer;

/**
 * A generic pooled resource manager.
 *
 * @param <T> the resource type
 *
 * @apiviz.landmark
 *
 */
public interface Pool<T> {

    /**
     * Allocate a resource from the pool.
     *
     * @return the resource
     */
    Pooled<T> allocate();

    /**
     * A compatibility pool which maps to {@link ByteBufferPool#MEDIUM_HEAP}.
     */
    Pool<ByteBuffer> HEAP = new Pool<ByteBuffer>() {
        public Pooled<ByteBuffer> allocate() {
            return Buffers.globalPooledWrapper(ByteBufferPool.MEDIUM_HEAP.allocate());
        }
    };

    /**
     * A compatibility pool which maps to {@link ByteBufferPool#MEDIUM_DIRECT}.
     */
    Pool<ByteBuffer> MEDIUM_DIRECT = new Pool<ByteBuffer>() {
        public Pooled<ByteBuffer> allocate() {
            return Buffers.globalPooledWrapper(ByteBufferPool.MEDIUM_DIRECT.allocate());
        }
    };

    Pool<ByteBuffer> SMALL_DIRECT = new Pool<ByteBuffer>() {
        public Pooled<ByteBuffer> allocate() {
            return Buffers.globalPooledWrapper(ByteBufferPool.SMALL_DIRECT.allocate());
        }
    };

    default Pool<ByteBuffer> create(int size,boolean isDirect) {
       return new Pool<ByteBuffer>() {
            public Pooled<ByteBuffer> allocate() {
                return Buffers.globalPooledWrapper(ByteBufferPool.create(size,isDirect).allocate());
            }
        };
    }
}
