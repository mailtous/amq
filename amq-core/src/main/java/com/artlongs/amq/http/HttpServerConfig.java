package com.artlongs.amq.http;

import com.artlongs.amq.core.MqConfig;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
*@author leeton
*2018年2月6日
*
*/
public class HttpServerConfig {
	public static Charset charsets = StandardCharsets.UTF_8;
	public static int port = MqConfig.inst.admin_http_port;
	public static String host = MqConfig.inst.host;
	public static int maxConnection = 100;


}