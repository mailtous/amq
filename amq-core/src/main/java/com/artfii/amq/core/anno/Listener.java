package com.artfii.amq.core.anno;

import com.artfii.amq.core.MqClientProcessor;
import com.artfii.amq.http.routes.Route;
import com.artfii.amq.scanner.AnnotationDetector;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用来标志 Listener
 * 加了 @Listener 的方法会在MQ系统启动时自动加载
 * Created by ${leeton} on 2018/11/2.
 */
@Target({ElementType.METHOD,ElementType.PARAMETER,ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unchecked")
public @interface Listener {

    /**
     * 监听的主题
     * @return
     */
    String topic() default "";

}
