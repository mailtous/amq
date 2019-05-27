package amq.example.act;

import act.controller.Controller;
import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqClientAction;
import com.artlongs.amq.tester.TestUser;
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
public class TestController {

    @Inject
    private MqClientAction amqClient;

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
        TestUser user = new TestUser(2, "alice");
        String jobTopc = "topic_get_userById";
        amqClient.acceptJob(jobTopc, (Message job)->{
            if (job != null) {
                System.err.println("accept a job: " +job);
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    amqClient.<TestUser>finishJob(jobTopc, user);
                }
            }
        });
        return "ok";
    }


    /**
     * 发送方
     * 发布一个工作任务
     * @return
     */
    @GetAction("/sendjob")
    public Map send(){
        Map<String, Object> result = new HashMap<>();
        Message message = amqClient.publishJob("topic_get_userById",2);
        result.put("sendjob", "topic_get_userById");
        result.put("result", message);
        return result;
    }




}
