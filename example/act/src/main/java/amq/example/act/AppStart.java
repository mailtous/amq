package amq.example.act;

import act.Act;

/**
 * Func :
 *
 * @author: leeton on 2019/4/1.
 */
public class AppStart {


    /**
     * 启动项目前.请先运行MQ 服务器: {@link com.artlongs.amq.tester.MqStart}
     * @param args
     * @throws Exception
     */
      public static void main(String[] args) throws Exception {
        Act.start("AMQ-ACT-EXAMPLE");
    }
}
