package com.artlongs.amq.http;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpResponse {
	String getHeader(String name);
	void setHeader(String name, String value);
	HttpStatus getState();
	void setState(HttpStatus state);
	void append(String body);
	void append(byte[] body) ;

	void end();
}
