package com.artfii.amq.core.aio;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface Protocol<T> {
    ByteBuffer encode(T obj);
    T decode(final ByteBuffer buffer);


}
