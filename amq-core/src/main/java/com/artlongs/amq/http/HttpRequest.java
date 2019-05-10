package com.artlongs.amq.http;

import java.util.Map;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpRequest {
	
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_DELETE = "DELETE";
	
	String uri();
	String method();
	String query();
	String bodyString();
	byte[] bodyBytes();
	String header(String name);
	Map<String,String> headers();
	Map<String,Object> params();
}
