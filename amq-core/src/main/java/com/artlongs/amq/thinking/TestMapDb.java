package com.artlongs.amq.thinking;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/15.
 */
public class TestMapDb {

    public static void main(String[] args) {

        DB db = DBMaker.fileDB("/volumes/work/mapdb.db")
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .allocateIncrement(1024)
                .closeOnJvmShutdown()
                .transactionEnable()
                .cleanerHackEnable()
                .make();


        BTreeMap<Long, int[]> myMap = db.treeMap("data")
                .keySerializer(Serializer.LONG)
                .valueSerializer(Serializer.INT_ARRAY)
                .createOrOpen();

        double x = 127.4521364;
        double y = 56.1452147;
        myMap.put(10001L, new int[]{(int)(x*100000),(int)(y*100000)});
        int[] point = myMap.get(10001L);

        System.err.println("Point("+point[0]/1000000d+","+point[1]/1000000d+")");

        //用完请关闭db

        db.close();
    }

}
