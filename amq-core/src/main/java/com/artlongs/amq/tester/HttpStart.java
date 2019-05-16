package com.artlongs.amq.tester;

import com.artlongs.amq.http.AioHttpServer;
import com.artlongs.amq.http.Render;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;
import com.artlongs.amq.http.routes.Url;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
@Url
public class HttpStart extends Thread implements Controller {
	private static Logger logger = LoggerFactory.getLogger(HttpStart.class);

/*
    @Get("/hello")
    public Render index() {
        return Render.template("/hello.html");
    }
*/



    @Get("/hello")
    public Render index() {

        return Render.json("hello world!");
    }

    @Get("/user/{username}")
    public Render index(String username) {
        return Render.json(C.newMap("username", username));
    }

	public static void main(String[] args) {
        //
        AioHttpServer.instance.start();

        while (true) {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }



}
