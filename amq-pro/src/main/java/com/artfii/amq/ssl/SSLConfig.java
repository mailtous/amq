/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: SSLConfig.java
 * Date: 2018-01-01
 * Author: sandao
 */

package com.artfii.amq.ssl;

/**
 * @author 三刀
 * @version V1.0 , 2018/1/1
 */
public class SSLConfig {
    /**
     * 配置引擎在握手时使用客户端（或服务器）模式
     */
    private boolean clientMode;
    private String keyFile;

    private String keystorePassword;

    private String keyPassword;
    private String trustFile;

    private String trustPassword;

    private ClientAuth clientAuth = ClientAuth.NONE;

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getTrustFile() {
        return trustFile;
    }

    public void setTrustFile(String trustFile) {
        this.trustFile = trustFile;
    }

    public String getTrustPassword() {
        return trustPassword;
    }

    public void setTrustPassword(String trustPassword) {
        this.trustPassword = trustPassword;
    }

    public boolean isClientMode() {
        return clientMode;
    }

    public void setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
    }

    public ClientAuth getClientAuth() {
        return clientAuth;
    }

    public void setClientAuth(ClientAuth clientAuth) {
        this.clientAuth = clientAuth;
    }
}
