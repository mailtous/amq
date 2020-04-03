package com.artlongs.amq.tools;

import org.osgl.util.S;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单 ID 生成
 * 长度 19 位 = "yyyyMMddHHmmssSSS"(17) + 原子顺序数累加(位长自定义)
 * long类型最大为19位,足于应付普通需求
 * Created by ${leeton} on 2018/11/21.
 */
public enum ID {
    ONLY;
    /**
     * 上次生成ID的时间截
     */
    private BigInteger lastTimestamp = BigInteger.valueOf(0);
    private static final AtomicInteger atomicNum = new AtomicInteger();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");//(17)位

    //原子顺序数长度
    public static final int atomic_num_two = 99;
    public static final int atomic_num_three = 999;
    public static final int atomic_num_four = 9999;
    public static final int atomic_num_five = 99999;


    private StringBuffer getYmdId() {
        return new StringBuffer(sdf.format(System.currentTimeMillis()));
    }

    public synchronized String id(int longs) {
        //数字长度为longs位，长度不够数字前面补0
        String format = "%0"+ Integer.valueOf(longs).toString().length()+"d";
        StringBuffer ymd = getYmdId();
        BigInteger currentTimes = getCurrentTimes(ymd, longs, format);
        while (currentTimes.compareTo(lastTimestamp)==-1) {
            ymd=getYmdId();
            currentTimes = getCurrentTimes(ymd, longs, format);
        }

        //上次生成ID的时间截
        lastTimestamp = currentTimes;

        return String.valueOf(currentTimes);
    }

    private BigInteger getCurrentTimes(StringBuffer ymd,int longs,String format){
        return new BigInteger(ymd.append(getNewAutoNum(longs, format)).toString());
    }

    /**
     * 生成原子顺序数字
     * @param longs 原子顺序数的长度
     * @return
     */
    private String getNewAutoNum(int longs,String format){
        //线程安全的原子操作，所以此方法无需同步
        int newNum = atomicNum.incrementAndGet();
        if(atomicNum.get()>longs){
            atomicNum.set(0);
            newNum=atomicNum.get();
        }

        String newStrNum = String.format(format, newNum);
        return newStrNum;
    }


    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            String idStr =ID.ONLY.id(atomic_num_five);
            System.err.println(idStr);
            String ymd = S.first(idStr,17);
            System.err.println(sdf.format(System.currentTimeMillis()).equals(ymd));
        }

    }

}
