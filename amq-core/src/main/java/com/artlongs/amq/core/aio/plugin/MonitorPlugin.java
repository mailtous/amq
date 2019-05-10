package com.artlongs.amq.core.aio.plugin;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.tools.QuickTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> extends QuickTimerTask implements Monitor<T>,Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MonitorPlugin.class);

    public static Dashboard dashboard = new Dashboard(); // 仪表盘,只为显示用
    private Dashboard curBashboard = null;
    //
    public MonitorPlugin() {
        this.curBashboard = new Dashboard();
    }

    @Override
    protected long getDelay() {
        return getPeriod();
    }

    @Override
    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public boolean preProcess(AioPipe pipe, Object o) {
        curBashboard.processMsgNum.incrementAndGet();
        curBashboard.totalProcessMsgNum.incrementAndGet();
        return true;
    }

    @Override
    public void stateEvent(State State, AioPipe pipe, Throwable throwable) {
        switch (State) {
            case PROCESS_EXCEPTION:
                curBashboard.processFailNum.incrementAndGet();
                break;
            case NEW_PIPE:
                curBashboard.newConnect.incrementAndGet();
                break;
            case PIPE_CLOSED:
                curBashboard.disConnect.incrementAndGet();
                break;
        }
    }

    @Override
    public void read(AioPipe<T> pipe, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        curBashboard.inFlow.addAndGet(readSize);
    }

    @Override
    public void write(AioPipe<T> session, int writeSize) {
        curBashboard.outFlow.addAndGet(writeSize);
    }

    @Override
    public void run() {
        long curInFlow = curBashboard.inFlow.getAndSet(0);
        long curOutFlow = curBashboard.outFlow.getAndSet(0);
        long curDiscardNum = curBashboard.processFailNum.getAndSet(0);
        long curProcessMsgNum = curBashboard.processMsgNum.getAndSet(0);
        int connectCount = curBashboard.newConnect.getAndSet(0);
        int disConnectCount = curBashboard.disConnect.getAndSet(0);
        long totalsubscribe = totalSubscribe();
        logger.debug("\r\n-----这一分钟发生了什么----"
                + "\r\n[AIO]流入流量:\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n[AIO]流出流量:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n[AIO]处理失败消息数:\t" + curDiscardNum
                + "\r\n[AIO]已处理消息量:\t" + curProcessMsgNum
                + "\r\n[AIO]已处理消息总量:\t" + curBashboard.totalProcessMsgNum.get()
                + "\r\n[MQ]总订阅数:\t" + totalsubscribe
                + "\r\n[MQ]普通发布数:\t" + curBashboard.commonPublish.get()
                + "\r\n[MQ]普通订阅数:\t" + curBashboard.commonSubscribe.get()
                + "\r\n[MQ]已受任务数:\t" + curBashboard.accpetJob.get()
                + "\r\n[MQ]发布任务数:\t" + curBashboard.publishJob.get()
                + "\r\n[MQ]消息签收数:\t" + curBashboard.ack.get()
                + "\r\n[MQ]消息发送成功数:\t" + curBashboard.sendSucc.get()
                + "\r\n[MQ]消息发送失败数:\t" + curBashboard.sendFail.get()
                + "\r\n[AIO]新建连接数:\t" + connectCount
                + "\r\n[AIO]断开连接数:\t" + disConnectCount
                + "\r\n[AIO]在线连接数:\t" + curBashboard.onlineCount.addAndGet(connectCount - disConnectCount)
                + "\r\n[AIO]总连接次数:\t" + curBashboard.totalConnect.addAndGet(connectCount));

        dashboard = this.curBashboard;
    }

    public Integer totalSubscribe(){
        curBashboard.totalSubscribe.set(curBashboard.commonSubscribe.get()+curBashboard.accpetJob.get());
        return curBashboard.totalSubscribe.get();
    }

    public void incrCommonSubscribe(){
        curBashboard.commonSubscribe.incrementAndGet();
    }
    public void incrCommonPublish(){
        curBashboard.commonPublish.incrementAndGet();
    }

    public void incrAccpetJob(){
        curBashboard.accpetJob.incrementAndGet();
    }

    public void incrPublishJob(){
        curBashboard.publishJob.incrementAndGet();
    }
    public void incrSendFail(){
        curBashboard.sendFail.incrementAndGet();
    }
    public void incrSendSucc(){
        curBashboard.sendSucc.incrementAndGet();
    }
    public void incrAck(){
        curBashboard.ack.incrementAndGet();
    }


    /**
     * 监控仪表盘
     */
    public static class Dashboard{
        /** 当前周期内消息 流量监控*/
        private AtomicLong inFlow = new AtomicLong(0);

        /**当前周期内消息 流量监控*/
        private AtomicLong outFlow = new AtomicLong(0);

        /**当前周期内处理失败消息数*/
        private AtomicLong processFailNum = new AtomicLong(0);

        /**当前周期内处理消息数*/
        private AtomicLong processMsgNum = new AtomicLong(0);

        private AtomicLong totalProcessMsgNum = new AtomicLong(0);

        /**新建连接数*/
        private AtomicInteger newConnect = new AtomicInteger(0);

        /**断链数*/
        private AtomicInteger disConnect = new AtomicInteger(0);

        /**在线连接数*/
        private AtomicInteger onlineCount = new AtomicInteger(0);

        private AtomicInteger totalConnect = new AtomicInteger(0);

        // 总订阅数
        private AtomicInteger totalSubscribe = new AtomicInteger(0);

        // 普通订阅数
        private AtomicInteger commonSubscribe = new AtomicInteger(0);

        // 普通发布消息数
        private AtomicInteger commonPublish = new AtomicInteger(0);

        // 接受任务数
        private AtomicInteger accpetJob = new AtomicInteger(0);

        // 发布任务数
        private AtomicInteger publishJob = new AtomicInteger(0);

        // 发送成功数
        private AtomicInteger sendSucc = new AtomicInteger(0);
        // 发送失败数
        private AtomicInteger sendFail = new AtomicInteger(0);
        // 签收数
        private AtomicInteger ack = new AtomicInteger(0);



        //=========================================================

        public AtomicLong getInFlow() {
            return inFlow;
        }

        public void setInFlow(AtomicLong inFlow) {
            this.inFlow = inFlow;
        }

        public AtomicLong getOutFlow() {
            return outFlow;
        }

        public void setOutFlow(AtomicLong outFlow) {
            this.outFlow = outFlow;
        }

        public AtomicLong getProcessFailNum() {
            return processFailNum;
        }

        public void setProcessFailNum(AtomicLong processFailNum) {
            this.processFailNum = processFailNum;
        }

        public AtomicLong getProcessMsgNum() {
            return processMsgNum;
        }

        public void setProcessMsgNum(AtomicLong processMsgNum) {
            this.processMsgNum = processMsgNum;
        }

        public AtomicLong getTotalProcessMsgNum() {
            return totalProcessMsgNum;
        }

        public void setTotalProcessMsgNum(AtomicLong totalProcessMsgNum) {
            this.totalProcessMsgNum = totalProcessMsgNum;
        }

        public AtomicInteger getNewConnect() {
            return newConnect;
        }

        public void setNewConnect(AtomicInteger newConnect) {
            this.newConnect = newConnect;
        }

        public AtomicInteger getDisConnect() {
            return disConnect;
        }

        public void setDisConnect(AtomicInteger disConnect) {
            this.disConnect = disConnect;
        }

        public AtomicInteger getOnlineCount() {
            return onlineCount;
        }

        public void setOnlineCount(AtomicInteger onlineCount) {
            this.onlineCount = onlineCount;
        }

        public AtomicInteger getTotalConnect() {
            return totalConnect;
        }

        public void setTotalConnect(AtomicInteger totalConnect) {
            this.totalConnect = totalConnect;
        }

        public AtomicInteger getTotalSubscribe() {
            return totalSubscribe;
        }

        public void setTotalSubscribe(AtomicInteger totalSubscribe) {
            this.totalSubscribe = totalSubscribe;
        }

        public AtomicInteger getCommonSubscribe() {
            return commonSubscribe;
        }

        public void setCommonSubscribe(AtomicInteger commonSubscribe) {
            this.commonSubscribe = commonSubscribe;
        }

        public AtomicInteger getCommonPublish() {
            return commonPublish;
        }

        public void setCommonPublish(AtomicInteger commonPublish) {
            this.commonPublish = commonPublish;
        }

        public AtomicInteger getAccpetJob() {
            return accpetJob;
        }

        public void setAccpetJob(AtomicInteger accpetJob) {
            this.accpetJob = accpetJob;
        }

        public AtomicInteger getPublishJob() {
            return publishJob;
        }

        public void setPublishJob(AtomicInteger publishJob) {
            this.publishJob = publishJob;
        }

        public AtomicInteger getSendFail() {
            return sendFail;
        }

        public void setSendFail(AtomicInteger sendFail) {
            this.sendFail = sendFail;
        }

        public AtomicInteger getSendSucc() {
            return sendSucc;
        }

        public void setSendSucc(AtomicInteger sendSucc) {
            this.sendSucc = sendSucc;
        }

        public AtomicInteger getAck() {
            return ack;
        }

        public void setAck(AtomicInteger ack) {
            this.ack = ack;
        }
    }

}
