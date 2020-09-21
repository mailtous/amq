/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.artfii.amq.ssl;

import com.artfii.amq.conf.PropUtil;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.BaseMsgType;
import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.tools.cipher.Aes;
import com.artfii.amq.tools.cipher.Rsa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

/**
 * SSL/TLS通信插件
 * @function : 加载自定义 RSA 生成的公钥/私钥,加密/解密 SSL通讯的握手信息
 * @author leeton
 */
public class SslBasePlugin {
    private static Logger logger = LoggerFactory.getLogger(SslBasePlugin.class);
    public static BigInteger[] PUB_KEY = null;
    public static BigInteger[] SELFT_KEY = null;
    private static Rsa rsa = null;
    private static SslBasePlugin sslPlugin = null;

    public synchronized static SslBasePlugin build() {
        if (null == sslPlugin) {
            sslPlugin = new SslBasePlugin();
        }
        return sslPlugin;
    }

    public SslBasePlugin() {
        initRSA();
    }

    private void initRSA() {
        String pubKeyFile = MqConfig.inst.amq_pubkey_file;
        String selftKeyFile =MqConfig.inst.amq_selftkey_file;
        if (null == pubKeyFile || "" == pubKeyFile.trim()) {
            throw new RuntimeException("加载 RSA 公钥失败,请检查 [MqConfig.inst.amq_pubkey_file] ");
        }
        if (null == selftKeyFile || "" == selftKeyFile.trim()) {
            throw new RuntimeException("加载 RSA 公钥失败,请检查 [MqConfig.inst.amq_selftkey_file] ");
        }

        pubKeyFile = PropUtil.loadFileContent(pubKeyFile);
        selftKeyFile = PropUtil.loadFileContent(selftKeyFile);
        PUB_KEY = Rsa.unFormatKey(pubKeyFile);
        SELFT_KEY = Rsa.unFormatKey(selftKeyFile);
        rsa = Rsa.builder().setKey(PUB_KEY, SELFT_KEY).fast().build();
    }

    /**
     * 客户端发送握手信息
     *
     * @return
     */
    public BaseMessage clientReqAuthInfo() {
        List<BigInteger> msg = rsa.encrypt(Auth.HELLO);
        byte[] headMsg = ISerializer.Serializer.INST.of().toByte(msg);
        BaseMessage baseMessage = BaseMessage.ofHead(BaseMsgType.SECURE_SOCKET_MESSAGE_REQ, headMsg);
        return baseMessage;
    }

    /**
     * 服务端发送认证成功的信息
     * 认证成功:"OK_AEC密码"
     * 认证失败:"FAIL"
     * @return
     */
    public BaseMessage serverRspAuthResult(String backMsg) {
        List<BigInteger> msg = rsa.encrypt(backMsg);
        byte[] headMsg = ISerializer.Serializer.INST.of().toByte(msg);
        BaseMessage baseMessage = BaseMessage.ofHead(BaseMsgType.SECURE_SOCKET_MESSAGE_RSP, headMsg);
        return baseMessage;
    }

    /**
     * 客户端接收握手信息
     *
     * @param handMessage
     * @return
     */
    public SslBasePlugin.Auth clientReadAuthResult(BaseMessage handMessage) {
        SslBasePlugin.Auth auth = Auth.ofFail();
        BaseMessage.Head head = handMessage.getHead();
        if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_RSP == head.getKind()) {
            byte[] receBytes = head.getSlot();
            List<BigInteger> receCode = ISerializer.Serializer.INST.of().getObj(receBytes,List.class);
            String receMsg = rsa.decrypt(receCode);
            auth = SslBasePlugin.Auth.decodeAuthResult(receMsg);
        }
        return auth;
    }

    /**
     * 服务端解码收到的握手信息
     * @param baseMessage
     * @return
     */
    public boolean serverCheckAuth(BaseMessage baseMessage) {
        BaseMessage.Head head = baseMessage.getHead();
        if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_REQ == head.getKind()) {
            byte[] receBytes = head.getSlot();
            List<BigInteger> receCode = ISerializer.Serializer.INST.of().getObj(receBytes);
            String receMsg = rsa.decrypt(receCode);
            return Auth.checkHello(receMsg);
        }
        return false;
    }

    /**
     * 认证
     */
    public static class Auth{
        private static final String HELLO = "HI-AMQ";
        private static final String OK = "OK";
        private static final String FAIL = "FAIL";

        private String msg;  // 收到的信息
        private String send; // 发送的信息
        private String flag; // 认证标志 OK/FAIL
        private String cipher; //后续通讯AEC的密钥
        private BaseMsgType type; //消息类别

        public static Auth ofFail() {
            Auth auth = new Auth();
            auth.setFlag(FAIL);
            return auth;
        }


        /**
         * 服务端核对收到的握手信息
         *
         * @param receive 收到的握手信息
         * @return
         */
        public static boolean checkHello(String receive) {
            return Auth.HELLO.equals(receive.trim());
        }

        /**
         * 客户端通讯认证是否成功
         *
         * @param receive 收到的握手信息
         * @return
         */
        public static boolean isAuthSucc(String receive) {
            return Auth.OK.equals(receive);
        }

        /**
         * 解码服务端返回的认证结果
         * @param receiveMsg
         * @return
         */
        public static Auth decodeAuthResult(String receiveMsg) {
            Auth auth = new Auth();
            if (null != receiveMsg && receiveMsg != "") {
                String[] receArr = receiveMsg.split("_");
                auth.setMsg(receiveMsg);
                auth.setFlag(receArr[0]);
                auth.setCipher(receArr[1]);
            }
            return auth;
        }

        public static Auth buildServerAuthSucc() {
            String aecPwd = Aes.getRandomKey(8);
            Auth auth = new Auth();
            auth.setFlag(OK);
            auth.setCipher(aecPwd);
            auth.setMsg(OK + "_" + aecPwd);
            return auth;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Auth{");
            sb.append("msg='").append(msg).append('\'');
            sb.append(", send='").append(send).append('\'');
            sb.append(", flag='").append(flag).append('\'');
            sb.append(", cipher='").append(cipher).append('\'');
            sb.append(", type=").append(type);
            sb.append('}');
            return sb.toString();
        }
        //========================================


        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getSend() {
            return send;
        }

        public void setSend(String send) {
            this.send = send;
        }

        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            this.flag = flag;
        }

        public String getCipher() {
            return cipher;
        }

        public void setCipher(String cipher) {
            this.cipher = cipher;
        }

        public BaseMsgType getType() {
            return type;
        }

        public void setType(BaseMsgType type) {
            this.type = type;
        }
    }
}
