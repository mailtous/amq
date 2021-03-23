package com.artfii.amq.core;

import com.artfii.amq.conf.PropUtil;
import com.google.common.base.Strings;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MQ及 IO 配置项
 * 在 amq.properties 里设置下面的条目都会起作用,并且优先.
 */
public enum MqConfig {
    inst;

    public Charset utf_8 = StandardCharsets.UTF_8;
    public String prop_file_and_path = "amq.properties";
    public String profile = "";
    //显示调试信息
    public boolean show_debug = false;

    //开启本地设置本机监听的主题列表独占模式，影响：localhost_listen_topic_list
    public boolean open_localhost_topic_oneself = false;

    //设置本机监听的主题列表(主题的前缀以模糊匹配)，以便独占消费
    public String localhost_listen_topic_list = "";

    //========================== IO CONFIG =====================================
    public String host = "127.0.0.1";
    public int port = 8888;
    public int admin_http_port=8889;

    // 服务端的连接线程池大小(2的倍数,等同于开了多少个服务端)
    public int server_connect_thread_pool_size = 16;
    // 服务端的每一个channel 处理事件的线程数大小,实际上是(AsynchronousChannelGroup)的大小
    public int server_channel_event_thread_size = 16;

    // 客户端的连接线程池大小(2的倍数,等同于开了多少个客户端)
    public int client_connect_thread_pool_size = 2;
    // 客户端的的每一个channel 处理事件的线程数大小,实际上是(AsynchronousChannelGroup)的大小
    public int client_channel_event_thread_size = 16;

    // Ringbuff 工作线程池大小(2的倍数)
    public int worker_thread_pool_size = 128;
    // Ringbuff 工作线程最大时长(秒)
    public long worker_keepalive_second = 30 * 60;

    //========================== MQ CONFIG =====================================
    public String client_listener_scan_package = "amq.example.springboot,amq.example.act";//多项用,分隔
    // 自动确认收到消息
    public boolean mq_auto_acked = true;

    // 开启重发-->未签收消息
    public boolean start_msg_not_acked_resend = true;
    // 间隔x秒,重发未签收消息
    public int msg_not_acked_resend_period = 10;
    // 重发未签收消息的最大次数
    public int msg_not_acked_resend_max_times = 3;

    //开启重发-->发送失败的消息
    public boolean start_msg_falt_message_resend = true;
    // 间隔x秒,发送失败的消息重发间隔
    public int msg_falt_message_resend_period = 60;
    //发送失败的消息重发的最大次数
    public int msg_falt_message_resend_max_times = 3;

    //订阅的缓存队列容量
    public int mq_subscribe_quene_cache_sizes = 1024;

    //保存所有的消息(持久化),(感觉没有必要)
    public boolean start_store_all_message_to_db = false;
    // 消息的默认存活时间(秒)
    public long msg_default_alive_time_second = 86400;

    //启动客户端心跳检测
    public boolean start_check_client_alive = true;
    //启动流量显示
    public boolean start_flow_monitor = true;
    //启动 MQ 后台管理系统
    public boolean start_mq_admin = true;

    //========================== SSL CONFIG =====================================
    public String amq_pubkey_file;         // AMQ SSL 公钥文件
    public String amq_selftkey_file;       // AMQ SSL 私钥文件
    public boolean ssl_fast_token_model;   // 只校验token的快速通讯模式,避免加密所有通讯内容来增加通讯效率.

    //========================== DB CONFIG =====================================
    // MAPDB 数据库文件
    public String mq_db_store_file_path = "/volumes/work/mapdb/";

    MqConfig() {
        final Properties props = PropUtil.load(prop_file_and_path);
        this.profile = props.getProperty("profile");
        Field[] fields = this.getClass().getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();

        if (!props.isEmpty()) {
            for (Field field : fields) {
                field.setAccessible(true);
                fieldMap.put(field.getName(), field);
            }
            for (Object pKey : props.keySet()) {
                String ppKey = (String)pKey;
                String v = props.getProperty((String) pKey).trim().intern();
                if(ppKey.startsWith(profile)){ // 按 env 取值
                    ppKey = ppKey.replace(profile + ".", "");
                }
                if(null != fieldMap.get(ppKey)){
                    PropUtil.setField(this,fieldMap.get(ppKey), v);
                }
            }
            fieldMap.clear();
        }

    }

    /**
     * 是否匹配本机设置的监听主题
     * @param searchTopic
     * @return
     */
    public boolean isMatchLocalTopic(String searchTopic) {
        if("dev".equalsIgnoreCase(profile) && open_localhost_topic_oneself){// DEV 环境且独占模式开启
            List<String> topicList = getLocalHostListenTopicList();
            if(topicList.size()==0) return false;
            for (String s : topicList) {
                if(searchTopic.startsWith(s))return true;
            }
            return false;
        }
        return true;
    }

    private List<String> getLocalHostListenTopicList() {
        List list = new ArrayList(20);
        if (!Strings.isNullOrEmpty(localhost_listen_topic_list)) {
            String[] arr = localhost_listen_topic_list.split(",");
            for (String s: arr){
                list.add(s.trim().intern().replace("*",""));
            }
        }
        return list;
    }


    public static void main(String[] args) {
        System.err.println(MqConfig.inst.profile);
        System.err.println(MqConfig.inst.port);
    }
}