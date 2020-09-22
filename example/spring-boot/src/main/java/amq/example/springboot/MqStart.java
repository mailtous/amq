package amq.example.springboot;

import com.artfii.amq.core.AioMqServer;

/**
 * Func :
 *
 * @author: leeton on 2020/9/22.
 */
public class MqStart {
    public static void main(String[] args) {
        AioMqServer.instance.start();
    }
}
