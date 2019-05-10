package com.artlongs.amq.core.aio.plugin;

import com.artlongs.amq.core.aio.AioPipe;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface Monitor<T> extends Plugin {

    void read(AioPipe<T> pipe, int readSize);

    void write(AioPipe<T> pipe, int writeSize);
}
