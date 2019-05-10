package com.artlongs.amq.http;

/**
 * Func :
 *
 * @author: leeton on 2019/3/19.
 */
public class Http {
    private Request request;
    private Response response;

    public Request getRequest() {
        return request;
    }

    public Http setRequest(Request request) {
        this.request = request;
        return this;
    }

    public Response getResponse() {
        return response;
    }

    public Http setResponse(Response response) {
        this.response = response;
        return this;
    }
}
