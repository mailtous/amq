package com.artfii.amq.tools;

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

    private BigInteger lastId = BigInteger.valueOf(0); //上次生成ID的时间截
    private int last_atomic_num = 0; //上次的顺序数
    private static final AtomicInteger atomicNum = new AtomicInteger();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");//(17)位

    //原子顺序数长度
    public static final int atomic_num_two = 99;
    public static final int atomic_num_three = 999;
    public static final int atomic_num_four = 9999;
    public static final int atomic_num_five = 99999;

    public long getDefId(){
        return ID.ONLY.id(atomic_num_two).longValue();
    }


    private StringBuffer getYmdId() {
        return new StringBuffer(sdf.format(System.currentTimeMillis()));
    }

    public synchronized BigInteger id(int longs) {
        if(last_atomic_num != longs){ // 上次运行时定义的顺序数长度与本次不同
            lastId = BigInteger.valueOf(0);
        }
        //数字长度为longs位，长度不够数字前面补0
        String format = "%0" + Integer.valueOf(longs).toString().length() + "d";
        StringBuffer ymd = getYmdId();
        BigInteger currentId = getCurrentId(ymd, longs, format);
        while (currentId.compareTo(lastId) <= 0) {
            ymd = getYmdId();
            currentId = getCurrentId(ymd, longs, format);
        }

        //上次生成ID的时间截
        lastId = currentId;
        last_atomic_num = longs;
        return currentId;
    }

    private BigInteger getCurrentId(StringBuffer ymd, int longs, String format) {
        return new BigInteger(ymd.append(getNewAutoNum(longs, format)).toString());
    }

    /**
     * 生成原子顺序数字
     *
     * @param longs 原子顺序数的长度
     * @return
     */
    private String getNewAutoNum(int longs, String format) {
        int newNum = atomicNum.incrementAndGet();
        if (atomicNum.get() > longs) {
            atomicNum.set(0);
            newNum = atomicNum.get();
        }

        String newStrNum = String.format(format, newNum);
        return newStrNum;
    }


    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            String idStr = String.valueOf(ID.ONLY.id(atomic_num_five));
            System.err.println(idStr);
            String ymd = idStr.substring(0, 17);
            System.err.println(sdf.format(System.currentTimeMillis()).equals(ymd));
        }

        System.err.println("19len="+ID.ONLY.getDefId());

    }

}
