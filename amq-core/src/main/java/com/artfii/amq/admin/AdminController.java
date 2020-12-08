package com.artfii.amq.admin;

import com.artfii.amq.core.Message;
import com.artfii.amq.core.Subscribe;
import com.artfii.amq.core.aio.plugin.MonitorPlugin;
import com.artfii.amq.core.store.Condition;
import com.artfii.amq.core.store.IStore;
import com.artfii.amq.core.store.Page;
import com.artfii.amq.http.BaseController;
import com.artfii.amq.http.Render;
import com.artfii.amq.http.routes.Get;
import com.artfii.amq.http.routes.Url;

import java.util.Date;

/**
 * Func : MQ 后台管理查询
 *
 * @author: leeton on 2019/3/15.
 */
@Url
public class AdminController extends BaseController {

    @Get("/topic")
    public Render topicIndex() {
        return Render.template("/topic.html");
    }

    /**
     * 按 TOPIC 查询消息内容
     * @param topic
     * @param begin
     * @param end
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @Get("/topic/q")
    public Render topicQurey(String topic, Date begin, Date end, int pageNumber, int pageSize) {
        Page<Message> page = new Page(pageNumber, pageSize);
        page = IStore.ofServer().getPage(IStore.server_mq_common_publish,
                new Condition<Message>(s -> s.getK().getTopic().startsWith(topic)),
                new Condition<Message>(s -> s.getStat().getCtime() >= begin.getTime() && s.getStat().getCtime() <= end.getTime()),
                page, Message.class);

        return Render.json(page);
    }

    @Get("/dashboard")
    public Render dashboard() {
        return Render.template("/dashboard.html");
    }

    /**
     * 当前的流量显示
     * @return
     */
    @Get("/dashboard/curr")
    public Render dashboardCurr() {
        return Render.json(MonitorPlugin.dashboard);
    }

    public static void main(String[] args) {
        Condition c = new Condition<Subscribe>(s -> s.getTopic().startsWith("hello"));
        System.err.println(c);
    }


}
