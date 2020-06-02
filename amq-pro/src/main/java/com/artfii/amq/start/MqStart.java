package com.artfii.amq.start;

import com.artfii.amq.transport.AioSSLMqServer;

/**
 * Func :
 *
 * @author: leeton on 2019/4/18.
 */
public class MqStart {
    public static void main(String[] args) {
        AioSSLMqServer.instance.start();

    }
}
