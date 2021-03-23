package com.artfii.amq.tools;

import com.artfii.amq.core.MqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func:   : Mq Logger
 *
 * @author : Lqf(leeton)
 * @date : 2021/3/23.
 */
public class MqLogger {

    private Logger logger = null;

    private MqLogger() {
    }

    public static MqLogger build(Class<?> clz) {
        MqLogger mqLogger = new MqLogger();
        mqLogger.logger = LoggerFactory.getLogger(clz);
        return mqLogger;
    }

    public void debug(String format, Object... objects) {
       if(MqConfig.inst.show_debug){
           logger.debug(format,objects);
       }
    }

    public void info(String format, Object... objects) {
        logger.info(format,objects);
    }

    public void warn(String format, Object... objects) {
        logger.warn(format,objects);
    }

    public void error(String format, Object... objects) {
        logger.error(format,objects);
    }

}
