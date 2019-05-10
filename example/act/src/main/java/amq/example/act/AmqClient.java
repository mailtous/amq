package amq.example.act;

import com.artlongs.amq.core.*;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/4/1.
 */
public class AmqClient extends MqClientProcessor {

    public AmqClient() {
        try {
            final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
            AioMqClient<Message> client = new AioMqClient(new MqProtocol(), this);
            client.start(channelGroup);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class Module extends org.osgl.inject.Module {
        @Override
        protected void configure() {
            bind(MqClientAction.class).to(()->new AmqClient());
        }
    }

}
