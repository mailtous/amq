package com.artlongs.amq.http;

import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;
import com.artlongs.amq.http.routes.Url;

import java.util.ArrayList;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/3/19.
 */
@Url
public abstract class BaseController implements Controller  {

    private List<Controller> controllerList = new ArrayList<>();

    public BaseController() {
        set(this);
    }

    protected abstract void addController();

    public Controller[] getControllers() {
        addController();
        return controllerList.toArray(new Controller[controllerList.size()]);
    }

    public BaseController set(Controller controller) {
        this.controllerList.add(controller);
        return this;
    }

    /**
     * 静态文件路由
     */
    @Get("/views/asset/{all}")
    public Render asset(String all) {
        return Render.template("/asset/" + all);
    }


}
