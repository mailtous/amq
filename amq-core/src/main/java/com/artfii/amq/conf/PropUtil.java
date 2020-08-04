package com.artfii.amq.conf;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Func :加载属性文件
 *
 * @author: leeton on 2019/3/27.
 */
public class PropUtil {
    private static byte[] buffer;
    private static int size; // the number of significant bytes read
    private static int pointer; // the "read pointer"

    public static Properties load(String file) {
        Properties properties = new Properties();
        InputStream reader = null;
        try {
            reader = loadFile(file);
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

    /**
     * 按文件路径取得文件流
     * @param fileAndPath 要填写相对class路径
     * @return
     */
    public static InputStream loadFile(String fileAndPath) {
        //fileAndPath要填写相对路径
        return PropUtil.class.getClassLoader().getResourceAsStream(fileAndPath);
    }

    public static String loadFileContent(String fileAndPath) {
        try {
            InputStream in = loadFile(fileAndPath);
            readFrom(in);
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return new String("");
        }
    }

    public static void printAll(Properties props) {
        Enumeration en = props.propertyNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            String value = props.getProperty(key);
            System.out.println(key + " : " + value);
        }
    }

    public static void setField(Object clz,Field field, String v) {
        try {
            if (field.getType() == String.class) {
                field.set(clz, v);
            }
            if (field.getType() == Integer.class || field.getType() == int.class) {
                field.set(clz, Integer.valueOf(v));
            }
            if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                field.set(clz, Boolean.valueOf(v));
            }
            if (field.getType() == Long.class || field.getType() == long.class) {
                field.set(clz, Long.valueOf(v));
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void readFrom(final InputStream in) throws IOException {
        buffer = new byte[2 * 1024];
        pointer = 0;
        size = 0;
        int n;
        do {
            n = in.read(buffer, size, buffer.length - size);
            if (n > 0) {
                size += n;
            }
            resizeIfNeeded();
        } while (n >= 0);
    }

    private static void resizeIfNeeded() {
        if (size >= buffer.length) {
            final byte[] newBuffer = new byte[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
    }


    public static void main(String[] args) {
        PropUtil.printAll(PropUtil.load("amq.properties"));
    }


}
