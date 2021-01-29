package amq.example.act;


import act.Act;
import act.app.event.SysEventId;
import act.job.OnSysEvent;
import com.artfii.amq.core.anno.AmqService;
import com.artfii.amq.core.anno.Listener;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Singleton
public class ScanListener {

    /**
     * 扫描 Listener 注解
     */
    @OnSysEvent(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED)
    public static void scanAndReg(){
        for (Class c : AmqService.Scan.inst.classList) {
            Arrays.stream(c.getMethods()).forEach(method -> {
                if (method.isAnnotationPresent(Listener.class)) {
                    try {
                        Listener listener = method.getAnnotation(Listener.class);
                        String topic = listener.topic();
                        method.setAccessible(true);
                        Object cc = Act.app().getInstance(c);
                        method.invoke(cc,topic);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

            });
        }
    }


}
