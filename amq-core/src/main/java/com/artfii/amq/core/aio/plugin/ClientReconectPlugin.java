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
    private static ClientReconectPlugin clientReconectPlugin = null;

    private ClientReconectPlugin() {
    }

    public static ClientReconectPlugin start(AioPipe pipe) {
        if (null == clientReconectPlugin) {
            clientReconectPlugin = new ClientReconectPlugin();
            clientReconectPlugin.aioPipe = pipe;
            runTask();
        }
        return clientReconectPlugin;
    }

    private static synchronized void runTask() {
        if (null == timer) {
            timer = new Timer("Reconnect Timer", true);
            timer.scheduleAtFixedRate(clientReconectPlugin, 50, 5000);
        }
    }

    @Override
    public void run() {
        if (aioPipe.isClose()) {//如果通道已经断开,发起重连
            logger.warn("Client:{} try reconect to server.", aioPipe.getId());
            System.err.println("Client:{} try reconect to server."+ aioPipe.getId());
            aioPipe = aioPipe.reConnect();
        }
    }
}
