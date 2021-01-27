package amq.example.springboot;

import com.artfii.amq.core.*;
import com.artfii.amq.core.aio.AioProtocol;
import com.artfii.amq.core.anno.AmqService;
import com.artfii.amq.core.anno.Listener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Func : 客户端启动类
 * 注意如果要测试本类，需要把 AmqSSLClient 关闭注入及组件扫描
 * @author: leeton on 2019/4/1.
 */
@Component
public class AmqClient extends MqClientProcessor implements InitializingBean,ApplicationContextAware {
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

    /**
     * SPRING 初始化完成后，执行扫描 MQ 订阅类
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        scanAndRegSubscribe();

    }

    /**
     * 扫描 Listener 注解
     */
    private void scanAndRegSubscribe(){
        for (Class c : AmqService.Scan.inst.classList) {
            Arrays.stream(c.getMethods()).forEach(method -> {
                if (method.isAnnotationPresent(Listener.class)) {
                    try {
                        Listener listener = method.getAnnotation(Listener.class);
                        String topic = listener.topic();
                        method.invoke(context.getBean(c),topic);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (BeansException e) {
                        e.printStackTrace();
                    }
                }

            });
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
