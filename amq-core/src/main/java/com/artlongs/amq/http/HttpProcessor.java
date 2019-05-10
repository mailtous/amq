package com.artlongs.amq.http;

import com.artlongs.amq.core.aio.*;
import com.artlongs.amq.core.aio.plugin.Monitor;
import com.artlongs.amq.core.aio.plugin.Plugin;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Router;

/**
 * Func : Http 服务端处理中心
 * 这里通过 handle 模式,反射去执行对应的 Response
 *
 * @author: leeton on 2019/3/19.
 */
public class HttpProcessor implements AioProcessor<Http> {

    private HttpHandler handler = null;

    public HttpProcessor addController(Controller... controllers) {
        this.handler = Router.asRouter(controllers);
        return this;
    }

    @Override
    public void process(AioPipe<Http> pipe, Http http) {
        this.handler.handle(http.getRequest(), new Response(pipe));
    }

    @Override
    public void stateEvent(AioPipe pipe, State state, Throwable throwable) {

    }

    @Override
    public void addPlugin(Plugin plugin) {

    }

    @Override
    public Monitor getMonitor() {
        return null;
    }
}
