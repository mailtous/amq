package com.artlongs.amq.http;

import java.util.concurrent.atomic.AtomicInteger;

/**
*@author leeton
*2018年2月6日
*
*/
public class HttpServerState {

	public static final String STATE_RUNNING = "RUNNING";
	public static final String STATE_STOPED = "STOPED";
	
	public static AtomicInteger CONNECTION_NUMS ; //连接计数器
	public static AtomicInteger CONCURRENT_NUMS ; //并发计数器

	private String state = STATE_STOPED;
	
	public HttpServerState(HttpServer httpServer){
		CONNECTION_NUMS = new AtomicInteger(0);
		CONCURRENT_NUMS = new AtomicInteger(0);
	}


}
