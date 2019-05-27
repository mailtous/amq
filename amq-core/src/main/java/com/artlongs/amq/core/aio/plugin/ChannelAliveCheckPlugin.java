package com.artlongs.amq.core.aio.plugin;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.tools.QuickTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func : 客户端存活检测
 *
 * @author: leeton on 2019/3/11.
 */
public class ChannelAliveCheckPlugin extends QuickTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ChannelAliveCheckPlugin.class);

    private ConcurrentHashMap<Integer,AioPipe> channelAliveMap = null;

    public ChannelAliveCheckPlugin(ConcurrentHashMap<Integer,AioPipe>channelAliveMap ) {
        this.channelAliveMap = channelAliveMap;
    }

    @Override
    protected long getDelay() {
        return 5;
    }

    @Override
    protected long getPeriod() {
        return 30; // 30秒,检查一次
    }

    @Override
    public void run() {
        remove();
    }

    /**
     * 心跳检测
     */
    private synchronized void remove() {
        if(!channelAliveMap.isEmpty()){
            final Set<Integer> removeSet = new HashSet<>();
            for (Integer key : channelAliveMap.keySet()) {
                AioPipe pipe = channelAliveMap.get(key);
                if(null != pipe && pipe.getChannel().isOpen()){
                    removeSet.add(key);
                    pipe.close(false);
                    logger.warn("Client pipe ({}) is closed, remove now!",pipe.getId());
                }
            }
            for (Integer key : removeSet) {
                channelAliveMap.remove(key);
            }
        }
    }
}
