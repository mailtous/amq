package com.artfii.amq.core.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
