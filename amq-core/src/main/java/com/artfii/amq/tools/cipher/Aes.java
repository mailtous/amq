package com.artfii.amq.tools.cipher;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Aes {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String ALGORITHM = "AES";
    private SecretKeySpec secretKey;
    private static ConcurrentHashMap<Integer, Cipher> CIPHER_MAP = new ConcurrentHashMap<>(2);//推荐设置为2的倍数
    private static ConcurrentHashMap<String, Aes> INST_MAP = new ConcurrentHashMap<>(192); //推荐设置为3的倍数

    private Aes() {
    }
    public Aes(String key) {
        builKey(key);// generate secret key
    }

    public static synchronized Aes build(String key) {
        Aes aes = INST_MAP.get(key);
        if (aes == null) {
            aes = new Aes(key);
            INST_MAP.putIfAbsent(key, aes);
        }
        return aes;
    }

    public static synchronized Aes buildOfDef() {
        return new Aes(getRandomKey(8));
    }

    public void builKey(String strKey) {
        byte[] bk = md5Raw(strKey.getBytes(UTF_8));
        this.secretKey = new SecretKeySpec(bk, ALGORITHM);
    }

    public static byte[] md5Raw(byte[] data) {
        byte[] md5buf = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            md5buf = md5.digest(data);
//            System.err.println("md5buf="+new String(md5buf));
        } catch (Exception e) {
            md5buf = null;
            e.printStackTrace();
        }
        return md5buf;
    }


    /**
     * @param str
     * @return
     * @Description Aes encrypt
     */
    public String encode(String str) {
        byte[] encryptBytes = encode(str.getBytes());
        String base64Str = new String(Base64.getEncoder().encode(encryptBytes));
        return base64Str;
    }

    public byte[] encode(byte[] src) {
        byte[] encryptBytes = null;
        try {
            Cipher cipher= getChipher(Cipher.ENCRYPT_MODE);
            return encryptBytes = cipher.doFinal(src);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param str
     * @return
     * @Description Aes decrypt
     */
    public String decode(String str) {
        byte[] decryptBytes = Base64.getDecoder().decode(str);
        decryptBytes = decode(decryptBytes);
        return null == decryptBytes?"": new String(decryptBytes);
    }

    public byte[] decode(byte[] src) {
        byte[] decryptBytes = null;
        try {
            Cipher cipher= getChipher(Cipher.DECRYPT_MODE);
            decryptBytes = cipher.doFinal(src);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return decryptBytes;
    }

    private Cipher getChipher(int model){
        Integer key = model + secretKey.hashCode();
        Cipher cipher = CIPHER_MAP.get(key);
        if (null == cipher) {
            try {
                cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(model, this.secretKey);
                CIPHER_MAP.put(key, cipher);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
        return cipher;
    }

    final static String plantTxt = "!@#$%^&*[]ABCDEFGHIJKLMNOPQRSTWXYZ1234567890abcdefghijklmnopqrstwxyz"; //68
    /**
     * 产生一个随机密码
     *
     * @param len 密码长度
     * @return
     */
    public static String getRandomKey(int len) {
        char[] plantChar = plantTxt.toCharArray();
        StringBuilder box = new StringBuilder(len);
        int planttxtLen = plantTxt.length();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            box.append(plantChar[r.nextInt(planttxtLen)]);
        }

        return box.toString();
    }

    /**
     * test
     */
    public static void main(String[] args) {
//        String txt = "1000101w#E#测试ssAASASSC127.0.0.1lif123gsjkdsgvjxe--h_hello []{}";
        String txt = Aes.getRandomKey(8);
        Aes aes = Aes.buildOfDef();
        //encode
        String encodeText = "";
        final int TIMES = 1;
        int run_times = TIMES;
        long s1 = System.currentTimeMillis();
        while (run_times > 0) {
            encodeText = aes.encode(txt);
            run_times--;
        }
        System.err.println("encoder user time(ms):" + (System.currentTimeMillis() - s1));

        // decode
        String decodeText = "";
        long s2 = System.currentTimeMillis();
        run_times = TIMES;
        while (run_times > 0) {
            decodeText = aes.decode(encodeText);
            run_times--;
        }
        System.err.println("decoder user time(ms):" + (System.currentTimeMillis() - s2));

        System.out.println("encodeText  : " + encodeText);
        System.out.println("encodeTxtLen: " + encodeText.length());
        System.out.println("plantTxt    : " + txt);
        System.out.println("decodeText  : " + decodeText);
        System.out.println("randomKey=  : " + getRandomKey(8));
        System.out.println("解密是否正确  : " + decodeText.equals(txt));
    }
}