Artfii-MQ 简称(AMQ), 是一个极轻量化,依赖极度少的 MQ 消息系统.
使用方便,高性能,并且对硬件要求低,是它的特色.
1. 在追求小的同时,高性能也是必须的,消息的分派使用了 LMAX-Ringbuffer 技术以保证性能.
2. 默认情况下,发布的消息存活期为一天,以避免像 RocketMQ 那样产生大量过期无效的消息堆积.
3. 支持多个消费者签收后,才自动注销发布的消息(ALL-ACK),支持自定义消息存活周期(永久,ALL-ACK,闪存)
4. 所有消息默认情况下,接收成功后就会自动标记[已送达].
5. 客户未签收(NACK)的情况下,支持设置重发次数与重发的时间间隔.
6. 发送失败(消费者断连)的情况下,支持设置重发次数与重发的时间间隔.
7. 特色的 PING/PONG 消息模式,以支持类似 RPC 的功能调用,却无 RPC 强制偶合的缺点,性能也比 RPC 调用更好.(微服务的最佳伴侣)
8. 普通的(发布者/订阅者)模式则支持大批量,高并发的写日志模式的 IO 消息业务.
9. 自带流量监控及后台管理功能
10. 黑名单自动拒绝连接
11. 项目完成后,我才发现原来有个叫 MQTT 协议(物联网协议)的东东,一不小心本项目做成了它的实例.


# AMQ 的架构整体思路

1. 通过 AIO 处理 IO 数据流
2. 协议处理器转化 IO 数据流为 MSG 消息对象
3. 通过 RingBuffer 框架技术分发及处理消息任务
4. 持久化中心：接收保存消息

![整体架构图](img/AMQ-one2one.png)

# AMQ 开发实现要点

* 启动一个 AsynchronousServerSocketChannel 接收客户端消息，并创建AioPipe
```java
public class AioServer<T> implements Runnable {
    public void start() throws IOException {
          if (config.isBannerEnabled()) {
              LOGGER.info(config.BANNER + "\r\n :: amq-socket ::\t(" + config.VERSION + ")");
          }
          start0((AsynchronousSocketChannel channel)->new AioPipe<T>(channel, config,config.isSsl(),config.isServer()));
      }
}
```

* 创建AioClient,返回 pipe.

```java

public class AioClient<T> implements Runnable {
    
    /**
     * 启动客户端。
     * <p>
     * 在与服务端建立连接期间，该方法处于阻塞状态。直至连接建立成功，或者发生异常。
     * </p>
     * <p>
     * 该start方法支持外部指定AsynchronousChannelGroup，实现多个客户端共享一组线程池资源，有效提升资源利用率。
     * </p>
     *
     * @param asynchronousChannelGroup IO事件处理线程组
     */
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        this.asynchronousChannelGroup = asynchronousChannelGroup;
        this.socketChannel = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        //set socket options
        if (config.getSocketOptions() != null) {
            for (SocketOption option : config.getSocketOptions()) {
                socketChannel.setOption(option, option.type());
            }
        } else {
            setDefSocketOptions(socketChannel);
        }
        //bind host
        socketChannel.connect(new InetSocketAddress(config.getHost(), config.getPort())).get();
        //连接成功则构造 AIO-PIPE 对象
        pipe = new AioPipe<>(socketChannel, config);
        pipe.setAioClient(this);
        pipe.startRead();
        // 启动断链重连TASK
        ClientReconectTask.start(pipe, config.getBreakReconnectMs());
        logger.warn("amq-socket client started on {} {}, pipeId:{}", config.getHost(), config.getPort(),pipe.getId());
        return pipe;
    }
}

```

* 创建AioMqServer,启动时加载消息处理器（MqServerProcessor），协议（AioProtocol）

```java
public class AioMqServer extends AioServer {
    public void start() {
            AioServer<ByteBuffer> aioServer = new AioServer(MqConfig.inst.host, MqConfig.inst.port, new AioProtocol(), new MqServerProcessor());
    }
}
```



* 解码消息流后，发送到消息处理器

```java
/**
 * Func : Mq 消息处理
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<BaseMessage> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);

    private void directSend(AioPipe pipe, Message message) {
        ProcessorImpl.INST.onMessage(pipe, message);
    }

}
```

* 消息处理中心，进一步处理消息（发布、订阅，持久化...)

```java
public enum ProcessorImpl implements Processor{

    public void onMessage(AioPipe pipe, Message message) {
        if (!shutdowNow && null != message) {
            if (MqConfig.inst.start_store_all_message_to_db) { // 持久化所有消息
                if (!message.subscribeTF()) {
                    tiggerStoreAllMsgToDb(persistent_worker, message);
                }
            }
            String msgId = message.getK().getId();
            if (message.ackMsgTF()) { // ACK 消息
                incrAck();
                if (Message.Life.SPARK == message.getLife()) {
                    removeSubscribeCacheOnAck(msgId);
                    removeDbDataOfDone(msgId);
                } else {
                    Integer clientNode = getNode(pipe);
                    upStatOfACK(clientNode, message);
                }
            } else {
                if (message.subscribeTF()) { // subscribe msg
                    addSubscribeIF(pipe, message);
                    if (isAcceptJob(message)) { // 如果工作任务已经先一步发布了,则触发-->直接把任务发给订阅者
                        incrAccpetJob();
                        triggerDirectSendJobToAcceptor(pipe, message);
                    }else {
                        incrCommonSubscribe();
                    }
                    //
                    return;

                } else {
                    if (isPublishJob(message)) { // 发布的消息为工作任务(pingpong)
                        incrPublishJob();
                        cachePubliceJobMessage(msgId, message);
                        buildSubscribeWaitingJobResult(pipe, message);
                        Subscribe acceptor = getSubscribe(message.getK().getTopic());
                        if (null != acceptor) { // 本任务已经有订阅者
                            sendMessageToSubcribe(message,acceptor);
                            return;
                        }
                    }else {
                        incrCommonPublish();
                        if (Message.Life.SPARK != message.getLife()) {
                            cacheCommonPublishMessage(msgId, message);
                        }
                    }
                    // 发布消息
                    publishJobToWorker(message);
                }
            }
        } 
    }
 }
```


# AMQ 的使用(SPRING-BOOT示例)

1. 创建服务端并优先启动

```java

public class MqStart {
    public static void main(String[] args) {
        AioMqServer.instance.start();
    }
}
```

2. 创建客户端：

```java
@Component
public class AmqClient extends MqClientProcessor {

    public AmqClient() {
        try {
            final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
            AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
            AioMqClient<Message> client = new AioMqClient(new AioProtocol(), this);
            client.setBreakReconnectMs(5000); //设置断链重连的时间周期
            client.start(channelGroup);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
```

3. 发布或接收消息

```java
/**
 * Func : 示例
 * 请先执行 acceptjob 方法,即先提交一个任务订阅,再执行sendjob
 *
 * @author: leeton on 2019/4/1.
 */
@RestController
public class TestController {

    @Resource
    private AmqClient amqClient;

    @RequestMapping("/")
    public String hello(){
        return "Are u ok?";
    }

    /**
     * 订阅方
     * 接收一个 JOB,完成后反馈结果给 JOB 发布者
     * @return
     */
    @RequestMapping("/acceptjob")
    public String rec(){
        TestUser user = new TestUser(2, "alice");
        String jobTopc = "topic_get_userById";
        amqClient.acceptJob(jobTopc, (Message job)->{
            if (job != null) {
                System.err.println("accept a job: " +job);
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    amqClient.<TestUser>finishJob(jobTopc, user);
                }
            }
        });
        return "ok";
    }

    /**
     * 发送方
     * 发布一个工作任务，并接收执行结果
     * @return
     */
    @RequestMapping("/sendjob")
    public Map send(){
        Map<String, Object> result = new HashMap<>();
        Message message = amqClient.publishJob("topic_get_userById",2);
        result.put("sendjob", "topic_get_userById");
        result.put("result", message);
        return result;
    }

}

```



# Disruptor 架构及 Ringbuffer 技术点的简单介绍（3.0版本）

![Ringbuffer示意图](img/RingBufferReplay.png)

RingBuffer看起来是一个环型结构，实际上是一个顺序的数组结构，请看 CODE：

```java
 // 左缓存数据块
 public class LhsPadding
 {
     protected long p1, p2, p3, p4, p5, p6, p7;
 }
 // 右缓存数据块
 public class RhsPadding extends Value
 {
     protected long p9, p10, p11, p12, p13, p14, p15;
 }
 //最终组合成一个顺序位槽结构 Sequence(序列), 里面使用 CAS 技术写入数据
 public class Sequence extends RhsPadding {
    public boolean compareAndSet(final long expectedValue, final long newValue)
    {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expectedValue, newValue);
    }
}

// 最终交给 RingBuffer 类去包装 Sequence 并对其进行读写操作：

public final class RingBuffer<E> extends RingBufferFields<E> implements Cursored, EventSequencer<E>, EventSink<E>
{
    public static final long INITIAL_CURSOR_VALUE = Sequence.INITIAL_VALUE;
    protected long p1, p2, p3, p4, p5, p6, p7;
    
    /**
     * Construct a RingBuffer with the full option set.
     */
    RingBuffer(EventFactory<E> eventFactory,Sequencer sequencer)
    {
        super(eventFactory, sequencer);
    }

        
        public E get(long sequence)
        {
            return elementAt(sequence);
        }
        

        public void publishEvent(EventTranslator<E> translator)
        {
            final long sequence = sequencer.next();
            translateAndPublish(translator, sequence);
        }
    
}

```

[distuptor3.0 示意图](img/distuptor3.0.png)

[Disruptor及Ringbuffer的使用示例](https://www.cnblogs.com/haiq/p/4112689.html)








