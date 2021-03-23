package amq.example.act;

import act.controller.Controller;
import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqAction;
import com.artfii.amq.core.anno.AmqService;
import com.artfii.amq.core.anno.Listener;
import com.artfii.amq.tester.TestUser;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Func : 示例
 * 请先执行 acceptjob 方法,即先提交一个任务订阅,再执行sendjob
 *
 * @author: leeton on 2019/4/1.
 */
@Controller
@AmqService
public class TestController {

    @Inject
    private MqAction mqAction;
    @Inject
    private ScanListener scanListener;

    @GetAction("/")
    public String hello(){
        return "Are u ok?";
    }

    /**
     * 订阅方
     * 接收一个 JOB,完成后反馈结果给 JOB 发布者
     * @return
     */
    @GetAction("/acceptjob")
    public String rec(){
        String topic = "topic_get_userById";
        subscribe(topic);
        return "ok";
    }

    @Listener(topic = "topic_get_userById")
    public void subscribe(String topic) {
        TestUser user = new TestUser(2, "alice");
        mqAction.acceptJob(topic, (Message job)->{
            if (job != null) {
                System.err.println("accept a job: " +job);
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    mqAction.<TestUser>pongJob(topic, user);
                }
                System.err.println(job);
            }
        });
    }

    /**
     * 发送方
     * 发布一个工作任务
     * @return
     */
    @GetAction("/sendjob")
    public Map send(){
        Map<String, Object> result = new HashMap<>();
        Message message = mqAction.pingJob("topic_get_userById",2);
        result.put("sendjob", "topic_get_userById");
        result.put("result", message);
        return result;
    }




}
