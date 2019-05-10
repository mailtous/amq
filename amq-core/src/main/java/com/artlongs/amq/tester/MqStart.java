package com.artlongs.amq.tester;

import com.artlongs.amq.core.AioMqServer;

/**
 * Func :
 *
 * @author: leeton on 2019/4/18.
 */
public class MqStart {
    public static void main(String[] args) {
        AioMqServer.instance.start();

    }
}
