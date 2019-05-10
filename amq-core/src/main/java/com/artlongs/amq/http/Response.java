package com.artlongs.amq.http;

import com.artlongs.amq.core.aio.AioPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author leeton 2018年2月6日
 */
public class Response extends Http implements HttpResponse {
    private static final Logger logger = LoggerFactory.getLogger(Response.class);
    //
    private static final String rn = "\r\n";
    private static final String end = "0\r\n\r\n";
    //
    private static final HashMap<String, String> baseHeaders = new HashMap<>();

    static {
        baseHeaders.put("Server", "amq-http");
        baseHeaders.put("Transfer-Encoding", "chunked");
    }

    //
    private HttpStatus status = HttpStatus.OK;
    private Map<String, String> headers = null;
    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
    private AioPipe aioPipe;

    public Response(AioPipe pipe) {
        this.aioPipe = pipe;
        this.headers = baseHeaders;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public HttpStatus getState() {
        return status;
    }

    @Override
    public void setState(HttpStatus code) {
        this.status = code;
    }

    @Override
    public void append(String body) {
        buildHttpBody(body.getBytes());
    }

    @Override
    public void append(byte[] body) {
        buildHttpBody(body);
    }

    //====================== write ====================================

    public void end() {
        this.aioPipe.write(new Http().setResponse(this));
    }

    public synchronized ByteBuffer build() {
        buildHttpFirstLine(this.status);
        buildHttpHeader(this.headers);
        appendBodyToOutStream();
        buildHttpEnd();
        int length = outStream.size();
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.put(outStream.toByteArray(), 0, length);
        buffer.flip();
        clear();
        return buffer;
    }

    private void buildHttpFirstLine(HttpStatus status) {
        String respLine = "HTTP/1.1 " + status.code + " " + status.msg + rn;
        write2OutStream(outStream, respLine.getBytes());
    }

    private void buildHttpHeader(Map<String, String> headers) {
        if (headers.size() == 0) return;
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            String values = headers.get(key);
            write2OutStream(outStream, (key + ":" + values + rn).getBytes());
        }
        write2OutStream(outStream, rn.getBytes());
    }

    private void buildHttpBody(byte[] data) {
        byte[] hex = Integer.toHexString(data.length).getBytes();
        write2OutStream(bodyStream, hex);
        write2OutStream(bodyStream, rn.getBytes());
        write2OutStream(bodyStream, data);
        write2OutStream(bodyStream, rn.getBytes());
    }

    private void appendBodyToOutStream() {
        try {
            getBodyStream().writeTo(outStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildHttpEnd() {
        write2OutStream(outStream, end.getBytes());
    }

    private ByteArrayOutputStream getBodyStream() {
        return bodyStream;
    }

    private void write2OutStream(ByteArrayOutputStream baos, byte[] data) {
        if (null == data) return;
        try {
            baos.write(data);
        } catch (IOException ex) {
            throw new RuntimeException("IO write on Error:", ex);
        }
    }

    private void clear() {
        try {
            outStream.close();
            bodyStream.close();
            outStream = null;
            bodyStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
