/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLService.java
 * Date: 2018-01-01
 * Author: sandao
 */

package com.artfii.amq.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLS/SSL服务
 *
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SSLService {
    private static final Logger logger = LoggerFactory.getLogger(SSLService.class);

    private boolean isClient;



    public SSLService(boolean isClient) {
        this.isClient = isClient;
    }



}
