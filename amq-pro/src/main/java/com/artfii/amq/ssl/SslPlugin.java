/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: TlsPlugin.java
 * Date: 2020-04-17
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.artfii.amq.ssl;

import com.artfii.amq.buffer.BufferFactory;
import com.artfii.amq.buffer.BufferPagePool;
import com.artfii.amq.conf.PropUtil;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.core.aio.BaseMsgType;
import com.artfii.amq.core.aio.State;
import com.artfii.amq.core.aio.plugin.Plugin;
import com.artfii.amq.serializer.ISerializer;
import com.artfii.amq.tools.cipher.AesUtil;
import com.artfii.amq.tools.cipher.Rsa;

import java.math.BigInteger;
import java.util.List;

/**
 * SSL/TLS通信插件
 *
 * @author 三刀
 * @version V1.0 , 2020/4/17
 */
public final class SslPlugin<T> implements Plugin<T> {
    public static BigInteger[] PUB_KEY = null;
    public static BigInteger[] SELFT_KEY = null;
    private static Rsa rsa = null;
    private BufferPagePool bufferPagePool;

    private static SslPlugin sslPlugin = null;

    public static SslPlugin build() {
        if (null == sslPlugin) {
            sslPlugin = new SslPlugin<>();
        }
        return sslPlugin;
    }

    public SslPlugin() {
        this.bufferPagePool = BufferFactory.DISABLED_BUFFER_FACTORY.create();
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
    public BaseMessage clientAuthInfo() {
        List<BigInteger> msg = rsa.encrypt(Auth.HELLO);
        byte[] headMsg = ISerializer.Serializer.INST.of().toByte(msg);
        BaseMessage baseMessage = new BaseMessage();
        BaseMessage.HeadMessage head = new BaseMessage.HeadMessage(BaseMsgType.SECURE_SOCKET_MESSAGE_REQ, headMsg);
        baseMessage.setHead(head);
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
        BaseMessage baseMessage = new BaseMessage();
        BaseMessage.HeadMessage head = new BaseMessage.HeadMessage(BaseMsgType.SECURE_SOCKET_MESSAGE_RSP, headMsg);
        baseMessage.setHead(head);
        return baseMessage;
    }

    /**
     * 服务端接收握手信息
     *
     * @param baseMessage
     * @return
     */
    public String clientReceMsg(BaseMessage baseMessage) {
        BaseMessage.HeadMessage head = baseMessage.getHead();
        if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_RSP == head.getBaseMsgType()) {
            byte[] receBytes = head.getInclude();
            List<BigInteger> receCode = ISerializer.Serializer.INST.of().getObj(receBytes,List.class);
            String receMsg = rsa.decrypt(receCode);
            return receMsg;
        }
        return "";
    }

    /**
     * 服务端解码收到的握手信息
     * @param baseMessage
     * @return
     */
    public boolean serverCheckAuth(BaseMessage baseMessage) {
        BaseMessage.HeadMessage head = baseMessage.getHead();
        if (null != head && BaseMsgType.SECURE_SOCKET_MESSAGE_REQ == head.getBaseMsgType()) {
            byte[] receBytes = head.getInclude();
            List<BigInteger> receCode = ISerializer.Serializer.INST.of().getObj(receBytes);
            String receMsg = rsa.decrypt(receCode);
            return Auth.checkHello(receMsg);
        }
        return false;
    }

    public BufferPagePool getBufferPagePool() {
        return bufferPagePool;
    }

    @Override
    public boolean preProcess(AioPipe<T> pipe, T message) {

        return true;
    }

    @Override
    public void stateEvent(State State, AioPipe<T> pipe, Throwable throwable) {

    }

    public static class Auth{
        private static final String HELLO = "HI-AMQ";
        private static final String OK = "OK";
        private static final String FAIL = "FAIL";

        private String receive; // 收到的信息
        private String send; // 发送的信息
        private String flag; //认证标志 OK/FAIL
        private String cipher; //后续通讯AEC的密钥
        private BaseMsgType type; //消息类别


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
            Auth auth = decodeAuthResult(receive);
            return Auth.OK.equals(auth.getFlag());
        }

        /**
         * 解码服务端返回的认证结果
         * @param receiveMsg
         * @return
         */
        private static Auth decodeAuthResult(String receiveMsg) {
            Auth auth = new Auth();
            if (null != receiveMsg && receiveMsg != "") {
                String[] receArr = receiveMsg.split("_");
                auth.setReceive(receiveMsg);
                auth.setFlag(receArr[0]);
                auth.setCipher(receArr[1]);
            }
            return auth;
        }

        public static String serverAuthSucc() {
            String aecPwd = AesUtil.build().encode(AesUtil.DEFULT_KEY);
            return OK + "_" + aecPwd;
        }

        public static String serverAuthFail() {
            return FAIL;
        }

        //========================================


        public String getReceive() {
            return receive;
        }

        public void setReceive(String receive) {
            this.receive = receive;
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