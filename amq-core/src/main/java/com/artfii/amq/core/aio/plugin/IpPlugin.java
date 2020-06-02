package com.artfii.amq.core.aio.plugin;


import com.artfii.amq.conf.PropUtil;
import org.osgl.util.C;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Func :
 *
 * @author: leeton on 2019/3/27.
 */
public class IpPlugin {
    private static List<String> ipBlackList = C.newList();
    private static List<String> getIPBlackList(){
        List<String> ipBlackList = C.newList();
        Properties props = PropUtil.load("amq.properties");
        if (!props.isEmpty()) {
            String blacks = props.getProperty("ip_black_list");
            String[] blackArr = blacks.split(";");
            ipBlackList.addAll(Arrays.asList(blackArr));
        }
        return ipBlackList;
    }

    public static boolean findInBlackList(String ip){
        if(ipBlackList.isEmpty()){
            ipBlackList = getIPBlackList();
        }
        for (String blackIP : ipBlackList) {
            if(ip.indexOf(blackIP)!=-1) return true;
        }
        return false;
    }
}
