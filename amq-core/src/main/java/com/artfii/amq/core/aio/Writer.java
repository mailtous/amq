package com.artfii.amq.core.aio;

import com.artfii.amq.core.aio.plugin.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class Writer<T> implements CompletionHandler<Integer, AioPipe<T>> {
    private static final Logger logger = LoggerFactory.getLogger(Writer.class);

    @Override
    public void completed(Integer size, AioPipe<T> pipe) {
        try {
//            logger.debug("write completed .");
            // 接收到的消息进行预处理
            Monitor monitor = pipe.getServerConfig().getProcessor().getMonitor();
            if (monitor != null) {
                monitor.write(pipe, size);
            }
            // 清理
            pipe.clearWriteBufferAndUnLock();

        } catch (Exception e) {
            failed(e, pipe);
        }
    }

    @Override
    public void failed(Throwable exc, AioPipe<T> pipe) {
        pipe.clearWriteBufferAndUnLock();
        try {
            pipe.getServerConfig().getProcessor().stateEvent(pipe, State.OUTPUT_EXCEPTION, exc);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
        try {
            pipe.close();
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }
    }


}
