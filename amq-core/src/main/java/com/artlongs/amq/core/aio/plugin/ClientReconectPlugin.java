package com.artlongs.amq.core.aio.plugin;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Func : 断链重连
 *
 * @author: leeton on 2019/5/27.
 */
public class ClientReconectPlugin extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ClientReconectPlugin.class);
    private static Timer timer = new Timer("Quick Timer", true);
    private AioPipe aioPipe;
    private AsynchronousChannelGroup asynchronousChannelGroup;

    public ClientReconectPlugin(AioPipe aioPipe, AioServerConfig config, AsynchronousChannelGroup asynchronousChannelGroup) {
        this.aioPipe = aioPipe;
        this.asynchronousChannelGroup = asynchronousChannelGroup;
        timer.schedule(this,0,config.getBreakReconnect());
    }

    @Override
    public void run() {
        if(aioPipe.isClose()){
            logger.warn("Client:{} try reconect to server.", aioPipe.getId());
            this.aioPipe = aioPipe.reConnect();
        }
    }
}
