package com.artlongs.amq.http;


import com.artlongs.amq.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
*@author leeton
*2018年2月6日
*
*/
public class Request extends Http implements HttpRequest {
	private static Logger logger = LoggerFactory.getLogger(Request.class);

	public String method = METHOD_GET;
	public String uri;
	public String query;
	public byte[] bodyBytes;
	public StringBuilder sourceTxt = new StringBuilder();
	public final Map<String,String> headers = new LinkedHashMap<>();
	public final Map<String,Object> params = new LinkedHashMap<>();
	private ReadState state = ReadState.BEGIN;

	private static ISerializer serializer = ISerializer.Serializer.INST.of();

	public enum ReadState {
		BEGIN, REQ_FIRST_LINE,HEADERS, BODY, END
		}

	/**
	 * 把数据按 Http protocol 解析为 Request对象
	 * @param data
	 * @returnH
	 */
	public Request build(ByteBuffer data){
		try {
			data.rewind();
			CharBuffer buffer = HttpServerConfig.charsets.decode(data);
			sourceTxt.append(buffer);
			if(state == ReadState.BEGIN || state == ReadState.REQ_FIRST_LINE) {
				resolveFirstLine();
			}
			if(state == ReadState.HEADERS) {
				resolveHeader();
			}
			if(state == ReadState.BODY) {
				resolveBody(sourceTxt);
			}
			return this;
		} catch (Exception e) {
			logger.error("解码出错导致创建 REQ 失败.",e);
		}
		return new Request();
	}
	private void resolveFirstLine() {
		state = ReadState.REQ_FIRST_LINE;
		int x = sourceTxt.indexOf("\r\n");
		if(x >= 0) {
			String firstLine = sourceTxt.substring(0,x);
			sourceTxt.delete(0, x+2);
			x = firstLine.indexOf(' ');
			//如果x<0代表请求违法，会抛出异常
			this.method = firstLine.substring(0, x).toUpperCase();
			x++;
			int y = firstLine.indexOf(' ',x);
			String uri = firstLine.substring(x, y);
			x = uri.indexOf('?');
			if(x>0) {
				this.uri = uri.substring(0,x);
				this.query = uri.substring(++x);
				parserReqParams(this.query);
			}else {
				this.uri = uri;
			}
		}
		state = ReadState.HEADERS;
	}
	private void resolveHeader() {
		int x = sourceTxt.indexOf("\r\n");
		while(x>0) {
			int y = sourceTxt.indexOf(":");
			String key = sourceTxt.substring(0,y).trim();
			String value = sourceTxt.substring(y+1, x);
			this.headers.put(key, value);
			sourceTxt.delete(0, x+2);
			x = sourceTxt.indexOf("\r\n");
		}
		if(x==0) {
			sourceTxt.delete(0, 2);
			if(Request.METHOD_POST.equals(this.method)) {
				state = ReadState.BODY;
			}else {
				state = ReadState.END;
			}
			return;
		}
		state = ReadState.END;
	}
	private void resolveBody(StringBuilder data) {
		if (state != ReadState.BODY) return;
		this.bodyBytes = data.toString().getBytes();
		state = ReadState.END;
	}

	private void parserReqParams(String paramStr) {
		if (paramStr.length() <= 0) return;
		String[] paramArr = paramStr.split("&");
		for (String kvStr : paramArr) {
			String[] kv = kvStr.split("=");
			this.params.put(kv[0], kv[1]);
		}
	}

	@Override
	public String uri() {
		return uri;
	}
	@Override
	public String method() {
		return method;
	}
	@Override
	public String query() {
		return query;
	}
	@Override
	public byte[] bodyBytes() {
		return bodyBytes;
	}
	@Override
	public String header(String name) {
		return headers.get(name);
	}
	@Override
	public Map<String, String> headers() {
		return headers;
	}

	@Override
	public Map<String, Object> params() {
		return params;
	}

	@Override
	public String bodyString() {
		return String.valueOf(bodyBytes);
	}

	@Override
	public String toString() {
		return "Request [method=" + method + ", uri=" + uri + ", query=" + query + ", bodyBytes="
				+ Arrays.toString(bodyBytes) + ", headers=" + headers + "]";
	}
	
}