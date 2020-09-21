package com.artfii.amq.core.aio;

import com.artfii.amq.core.aio.plugin.Monitor;
import com.artfii.amq.core.aio.plugin.Plugin;

import java.util.LinkedList;

/**
 * Func : Aio 处理器
 *
 * @author: leeton on 2019/2/22.
 */
public interface AioProcessor<T> {
    void process(AioPipe<T> pipe, T msg);
    void stateEvent(AioPipe<T> pipe, State state, Throwable throwable);
    void addPlugin(Plugin plugin);
    LinkedList<Plugin> getPlugins();
    Monitor getMonitor();
}
