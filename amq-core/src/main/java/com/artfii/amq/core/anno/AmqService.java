package com.artfii.amq.core.anno;

import com.artfii.amq.scanner.AnnotationDetector;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;


/**
 * 标注为 AMQ Service 方便扫描
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AmqService {

    /**
     * 扫描加了本注解的类
     */
    enum Scan {
        inst;
        public List<Class> classList;

        Scan() {
            try {
                classList = AnnotationDetector.scanClassPath("amq.example.springboot") // or: scanFiles(File... files)
                        .forAnnotations(AmqService.class)
                        .on(ElementType.TYPE)
                        .collect(s -> s.getType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
