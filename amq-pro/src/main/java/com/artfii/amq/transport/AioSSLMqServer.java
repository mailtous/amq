package com.artfii.amq.transport;

import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.MqScheduler;
import com.artfii.amq.core.ProcessorImpl;
import com.artfii.amq.core.aio.AioProtocol;
import com.artfii.amq.core.aio.AioServer;
import com.artfii.amq.http.AioHttpServer;
import com.artfii.amq.http.HttpServer;
import com.artfii.amq.ssl.SslPlugin;
import com.artfii.amq.ssl.SslServerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func : SSL 服务端
 *
 * @author: leeton on 2019/2/22.
 */
public class AioSSLMqServer<T> extends AioServer {
    private static Logger logger = LoggerFactory.getLogger(AioSSLMqServer.class);

    public static final AioSSLMqServer instance = new AioSSLMqServer();

    private HttpServer httpServer = null;

    private ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.server_connect_thread_pool_size);

    private AioSSLMqServer() {
        super(MqConfig.inst.host, MqConfig.inst.port, new AioProtocol(), new SslServerProcessor());
        config.setSsl(true);
        config.setServer(true);
        config.getProcessor().addPlugin(new SslPlugin());
    }

    public void start() {

        try {
            this.startCheckAlive(MqConfig.inst.start_check_client_alive)
                .startMonitorPlugin(MqConfig.inst.start_flow_monitor)
                    .setResumeSubcribe(true);
            //
            pool.submit(this);
            //
            super.start();
            //
            ProcessorImpl.INST.addMonitor(this.getMonitor());
            //
            startAdmin();
            //
            scheduler();
            //
            startCommond();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void scheduler() {
        MqScheduler.inst.start();
    }

    public void startAdmin() {
        if (MqConfig.inst.start_mq_admin) {
            httpServer = AioHttpServer.instance;
            httpServer.start();
        }
    }


    private void startCommond() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            sc.useDelimiter("/n");
            System.out.println();
            System.out.println("=======================================");
            System.out.println("AMQ(SSL)已启动,(消息端口:" + config.host+ "),(管理端口:" + config.port + ")");
            System.out.println("如果想安全退出,请输入命令: quit");
            System.out.println("=======================================");
            System.out.println();
            String quit = sc.nextLine();
            if (quit.equalsIgnoreCase("quit")) {
                shutdown();
                sc.close();
                break;
            }

        }
    }


    public static void main(String[] args) throws IOException {
        AioSSLMqServer.instance.start();
    }

}
