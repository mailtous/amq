package com.artlongs.amq.http;

/**
 * @author leeton
 * 2018年2月6日
 */
public interface HttpServer {
    void start();

    void shutdown();

    HttpProcessor getHttpProcessor();


}
