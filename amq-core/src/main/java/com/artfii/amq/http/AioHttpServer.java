package com.artfii.amq.http;


import com.artfii.amq.http.routes.Controller;
import com.artfii.amq.http.routes.Url;
import com.artfii.amq.core.aio.AioServer;
import com.artfii.amq.http.routes.Controller;
import com.artfii.amq.http.routes.Url;
import com.artfii.amq.scanner.AnnotationDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author leeton
 * 2018年2月6日
 */
public class AioHttpServer implements HttpServer {
    private static Logger logger = LoggerFactory.getLogger(AioHttpServer.class);

    private AioHttpServer() {
        this.httpProcessor = new HttpProcessor();
    }

    public static final AioHttpServer instance = new AioHttpServer();

    private HttpProcessor httpProcessor;
    final ExecutorService pool = Executors.newFixedThreadPool(HttpServerConfig.maxConnection);

    public void start() {
        try {
            //
            autoScanController();
            //
            AioServer<Http> aioServer = new AioServer<Http>(HttpServerConfig.host, HttpServerConfig.port, new HttpProtocol(), this.httpProcessor);
            aioServer.setBannerEnabled(false);
            //
            Thread t = new Thread(aioServer);
            t.setDaemon(true);
            pool.submit(t);
            aioServer.start();
            //
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        pool.shutdownNow();
    }

    @Override
    public HttpProcessor getHttpProcessor() {
        return this.httpProcessor;
    }

    /**
     * 自动扫描 controller
     */
    public void autoScanController() {
        for (Class clz : Url.Scan.inst.classList) {
            try {
                Controller controller = (Controller) clz.newInstance();
                this.httpProcessor.addController(controller);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        AioHttpServer.instance.start();
    }


}
