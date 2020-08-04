import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqConfig;
import com.artfii.amq.core.aio.AioPipe;
import com.artfii.amq.core.aio.AioProtocol;
import com.artfii.amq.core.aio.BaseMessage;
import com.artfii.amq.ssl.SslClientProcessor;
import com.artfii.amq.transport.AioSSLMqClient;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2020/7/29.
 */
public class SslMqClientStart {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        final int threadSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r) -> new Thread(r));

        AioSSLMqClient sslQuickClient = new AioSSLMqClient(new AioProtocol(), new SslClientProcessor());
        AioPipe aioSession = sslQuickClient.start(channelGroup);
        BaseMessage msg = new BaseMessage();
        msg.setHead(new BaseMessage.HeadMessage());
        msg.setBody(Message.ofDef(new Message.Key(),"demo-hello"));
        aioSession.write(msg);
    }
}

