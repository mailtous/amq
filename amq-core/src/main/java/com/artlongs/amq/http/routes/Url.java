package com.artlongs.amq.http.routes;

import com.artlongs.amq.scanner.AnnotationDetector;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * 用来标志 Controller
 * Created by ${leeton} on 2018/11/2.
 */
@Target({ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unchecked")
public @interface Url {
    public String value() default "";

    enum Scan {
        inst;
        public List<Class> classList;

        Scan() {
            try {
                classList = AnnotationDetector.scanClassPath("com.artlongs.amq.admin","com.artlongs.amq.tester") // or: scanFiles(File... files)
                        .forAnnotations(Url.class) // one or more annotations
                        .on(ElementType.TYPE) // optional, default ElementType.TYPE. One ore more element types
//                        .filter((File dir, String name) -> !dir.getName().startsWith("com.artlongs.amq.tools")) // optional, default all *.class files
                        .collect(s -> s.getType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
