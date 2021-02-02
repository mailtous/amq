package amq.example.springboot;

import com.artfii.amq.core.anno.AmqService;
import com.artfii.amq.core.anno.Listener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Component
public class ScanListener implements InitializingBean,ApplicationContextAware {
    private ApplicationContext context;
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
                        method.setAccessible(true);
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
