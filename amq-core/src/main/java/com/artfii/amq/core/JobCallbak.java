package com.artfii.amq.core;

import com.artfii.amq.core.aio.Call;

import java.io.Serializable;

/**
 * SUBSCRIBE 持久化的封装类
 */
public class JobCallbak implements Serializable  {
    private static final long serialVersionUID = 1L;

    private String key;
    private Call call;

    public static JobCallbak build(String key, Call call) {
        JobCallbak jobCallbak = new JobCallbak();
        jobCallbak.key = key;
        jobCallbak.call = call;
        return jobCallbak;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Call getCall() {
        return call;
    }

    public void setCall(Call call) {
        this.call = call;
    }
}
