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
public class ClientReconectTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ClientReconectTask.class);
    public static Timer timer = null;
    private static AioPipe aioPipe;

    public ClientReconectTask() {
    }

    /**
     * @param aioPipe
     * @param periodMs 重连的间隔毫秒数
     */
    public ClientReconectTask(AioPipe aioPipe, long periodMs) {
        this.aioPipe = aioPipe;
        reconnectServerOfTask(periodMs);
    }

    public static void start(AioPipe aioPipe, long periodMs) {
        new ClientReconectTask(aioPipe, periodMs);
    }

    /**
     * 定时自动重新连接服务器
     */
    private synchronized void reconnectServerOfTask(long periodMs) {
        if (null == timer) {
            timer = new Timer("Reconnect Timer", true);
            timer.scheduleAtFixedRate(this, 50, periodMs);
        }
    }

    @Override
    public void run() {
        if (null != aioPipe && aioPipe.isClose()) {
            logger.warn("Client:{} try reconnect to server.", aioPipe.getId());
            aioPipe = aioPipe.reConnect();
        }
    }
}
