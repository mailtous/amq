package com.artlongs.amq.tools;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Func :
 * Created by leeton on 2019/1/7.
 */
public class DateUtils {

    private static final SimpleDateFormat sdf17 = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
     * 按(yyyyMMddHHmmssSSS)格式返回当前时间的17数长整数
     * @return
     */
    public static Long now(){
        return Long.valueOf(sdf17.format(System.currentTimeMillis()));
    }
    /**
     * 17位(yyyyMMddHHmmssSSS)长整数转为日期
     * @param l
     * @return
     */
    public static Date Long17ToDate(long l) {
        return new Date(l);
    }

    /**
     * 日期转为17位长整数(yyyyMMddHHmmssSSS)
     * @param date
     * @return
     */
    public static Long DateToLong17(DateTime date) {
        String d = date.toString("yyyyMMddHHmmssSSS");
        return Long.valueOf(d);
    }

    /**
     * 日期转为17位长整数(yyyyMMddHHmmssSSS)
     * @param date
     * @return
     */
    public static Long DateToLong17(Date date) {
        String d = sdf17.format(date);
        return Long.valueOf(d);
    }

}
