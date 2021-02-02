package amq.example.springboot;

import com.artfii.amq.core.AioMqClient;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqClientProcessor;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioProtocol;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func : 客户端启动类
 * 注意如果要测试本类，需要把 AmqSSLClient 关闭注入及组件扫描
 * @author: leeton on 2019/4/1.
 */
@Component
public class AmqClient extends MqClientProcessor  {
    private ApplicationContext context;

    public AmqClient() {
        try {
            final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
            AioMqClient<Message> client = new AioMqClient(new AioProtocol(), this);
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
