package com.artfii.amq.core;

import com.artfii.amq.conf.PropUtil;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * MQ及 IO 配置项
 * 在 amq.properties 里设置下面的条目都会起作用,并且优先.
 */
public enum MqConfig {
    inst;

    public Charset utf_8 = StandardCharsets.UTF_8;
    public String prop_file_and_path = "amq.properties";
    public String profile = "";

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

    public String amq_client_jks_file;
    public String amq_client_jks_pwd;
    public String amq_client_trust_file;
    public String amq_client_trust_pwd;
    //
    public String amq_server_jks_file;
    public String amq_server_jks_pwd;
    public String amq_server_trust_file;
    public String amq_server_trust_pwd;

    public String amq_pubkey_file;
    public String amq_selftkey_file;

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
                if(ppKey.startsWith(profile)){ // 按 env 取值
                    String v = props.getProperty((String) pKey).trim();
                    String fieldKey = ppKey.replace(profile + ".", "");
                    Field field = fieldMap.get(fieldKey);
                    PropUtil.setField(this,field, v);
                }
            }
            fieldMap.clear();
        }

    }


    public static void main(String[] args) {
        System.err.println(MqConfig.inst.profile);
        System.err.println(MqConfig.inst.port);
    }
}