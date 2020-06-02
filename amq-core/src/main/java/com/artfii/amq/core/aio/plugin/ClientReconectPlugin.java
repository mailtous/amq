package com.artfii.amq.core.aio.plugin;

import com.artfii.amq.core.aio.AioPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Func : 断链重连
 *
 * @author: leeton on 2019/5/27.
 */
public class ClientReconectPlugin extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ClientReconectPlugin.class);
    public static Timer timer = null;
    private static AioPipe aioPipe;

    public ClientReconectPlugin(AioPipe aioPipe) {
        this.aioPipe = aioPipe;
        createTask();
    }

    private synchronized void createTask() {
        if (null == timer) {
            timer = new Timer("Reconnect Timer", true);
            timer.scheduleAtFixedRate(this,50,5000);
        }
    }

    @Override
    public void run() {
        if(aioPipe.isClose()){
            logger.warn("Client:{} try reconect to server.", aioPipe.getId());
            aioPipe = aioPipe.reConnect();
        }
    }
}
