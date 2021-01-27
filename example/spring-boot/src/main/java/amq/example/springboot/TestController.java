package amq.example.springboot;

import com.artfii.amq.core.Message;
import com.artfii.amq.core.MqAction;
import com.artfii.amq.core.anno.AmqService;
import com.artfii.amq.core.anno.Listener;
import com.artfii.amq.tester.TestUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Func : 示例
 * 请先执行 acceptjob 方法,即先提交一个任务订阅,再执行sendjob
 *
 * @author: leeton on 2019/4/1.
 */
@RestController
@AmqService
public class TestController {

    @Autowired
    private AmqClient amqClient;

    public TestController() {
    }

    @RequestMapping("/")
    public String hello(){
        return "Are u ok?";
    }

    /**
     * 订阅方
     * 接收一个 JOB,完成后反馈结果给 JOB 发布者
     * @return
     */
    @RequestMapping("/acceptjob")
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
     * 监听任务主题，接收任务后反馈结果给 JOB 发布者
     * @param topic
     */
    @Listener(topic = "topic_get_userById")
    public void subscribe(String topic) {
        TestUser user = new TestUser(2, "alice");
        amqClient.acceptJob(topic, (Message job)->{
            if (job != null) {
                System.err.println("accept a job: " +job);
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    amqClient.<TestUser>finishJob(topic, user);
                }
            }
        });
    }

    /**
     * 发送方
     * 发布一个工作任务，并接收执行结果
     * @return
     */
    @RequestMapping("/sendjob")
    public Map send(){
        Map<String, Object> result = new HashMap<>();
        Message message = amqClient.publishJob("topic_get_userById",2);
        result.put("sendjob", "topic_get_userById");
        result.put("result", message);
        return result;
    }




}
