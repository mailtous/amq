package amq.example.springboot;

import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqClientProcessor;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioProtocol;
import com.artfii.amq.transport.AioSSLMqClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func : AmqSSLClient
 *
 * @author: leeton on 2019/4/1.
 */
//@Component
public class AmqSSLClient extends MqClientProcessor {

    public AmqSSLClient() {
        try {
            final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
            AioSSLMqClient<Message> client = new AioSSLMqClient(new AioProtocol(), this);
            client.setBreakReconnectMs(5000); //设置断链重连的时间周期
            client.start(channelGroup);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
