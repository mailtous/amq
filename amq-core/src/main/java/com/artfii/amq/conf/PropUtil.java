package com.artfii.amq.conf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Func :
 *
 * @author: leeton on 2019/3/27.
 */
public class PropUtil {
    public static Properties load(String file) {
        Properties properties = new Properties();
        InputStream reader = null;
        try {
            reader = PropUtil.class.getClassLoader().getResourceAsStream(file);
            properties.load(reader);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader = null;
            }
        }
        return properties;
    }

    public static void printAll(Properties props) {
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            String value = props.getProperty(key);
            System.out.println(key + " : " + value);
        }
    }

    public static void main(String[] args) {
        PropUtil.printAll(PropUtil.load("amq.properties"));
    }


}
