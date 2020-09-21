package amq.example.springboot;


import com.artfii.amq.transport.AioSSLMqServer;

/**
 * Func : MQ SSL 服务端启动
 *
 * @author: leeton on 2019/4/18.
 */
public class MqSSLStart {

    public static void main(String[] args) {
        AioSSLMqServer.instance.start();
    }
}
