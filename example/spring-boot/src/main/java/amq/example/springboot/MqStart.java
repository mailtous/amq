package amq.example.springboot;

import com.artfii.amq.core.AioMqServer;

/**
 * Func : 服务端启动类
 *
 * @author: leeton on 2020/9/22.
 */
public class MqStart {
    public static void main(String[] args) {
        AioMqServer.instance.start();
    }
}
