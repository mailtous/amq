import com.artfii.amq.transport.AioSSLMqServer;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2020/7/29.
 */
public class SslMqServerStart {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        AioSSLMqServer.instance.start();
    }

}
