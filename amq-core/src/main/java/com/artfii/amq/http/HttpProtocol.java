package com.artfii.amq.http;

import com.artfii.amq.core.aio.Protocol;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/3/18.
 */
public class HttpProtocol implements Protocol<Http> {

    @Override
    public ByteBuffer encode(Http http) {
        return http.getResponse().build();
    }

    @Override
    public Http decode(ByteBuffer buffer) {
        Http http = new Http();
        http.setRequest(new Request().build(buffer));//解析为 Request对象
        return http;
    }
}
