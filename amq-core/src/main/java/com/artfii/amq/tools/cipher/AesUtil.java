package com.artfii.amq.tools.cipher;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class AesUtil {
    /** 加密器容器 */
    private static ConcurrentHashMap<Integer, ConcurrentHashMap<String, Cipher>> cipherMap = null;

    private static final String CIPHER_TYPE = "AES/ECB/PKCS5Padding";
    public static final String DEFULT_KEY = "AMQ&LEETON168";
    private static AesUtil INST = null;
    private static byte[] KEY = new byte[16];  // 要求密码的最大长度为16bit

    private AesUtil() {
    }

    public static synchronized AesUtil build(String key) {
        if (INST == null) {
            INST = new AesUtil();
            INST.KEY = createKey(key);
            INST.cipherMap = new ConcurrentHashMap<>();
        }
        return INST;
    }

    public static synchronized AesUtil build() {
       return build(getRandomKey(8));
}

    private static byte[] createKey(String key) { // 要求密码的最大长度为16bit
        return md5Raw(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密
     * @param content 加密内容
     * @return 加密后的Base64字符串
     */
    public String encode(String content){
        try {
            // 创建密码器
            Cipher cipher = generateCipher(KEY, Cipher.ENCRYPT_MODE);
            // 获取加密内容的字节数组
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            // 加密
            byte[] byteEncode = cipher.doFinal(byteContent);
            // 将加密后的数据转换为base64字符串
            return new String(Base64.getEncoder().encode(byteEncode));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String decode(String content){
        try {
            // 创建密码器
            Cipher cipher = generateCipher(KEY, Cipher.DECRYPT_MODE);
            // 将加密的base64内容转成字节数组
            byte[] byteContent = Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8));
            // 解密
            byte[] byteDecode = cipher.doFinal(byteContent);
            // 返回明文
            return new String(byteDecode, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 创建密码器
     * @param key key
     * @param mode 模式 加密/解密
     * @return 密码器
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private Cipher generateCipher(byte[] key, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        ConcurrentHashMap<String, Cipher> map = cipherMap.get(mode);
        if(Objects.isNull(map)){
            map = new ConcurrentHashMap<>();
            cipherMap.put(mode, map);
        }
        Cipher cipher = map.get(new String(key));
        if(Objects.isNull(cipher)){
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            // 创建AES密码器 "算法/模式/补码方式"
            cipher = Cipher.getInstance(CIPHER_TYPE);
            // 第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的SecretKey
            cipher.init(mode, secretKey);
            // 避免并发创建大量密码器
            map.put(new String(key), cipher);
        }
        return cipher;
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
     * 产生一个随机密码
     * @param len 密码长度
     * @return
     */
    public static String getRandomKey(int len) {
        final String plantTxt = "!@#$%^&*[]ABCDEFGHIJKLMNOPQRSTWXYZ1234567890abcdefghijklmnopqrstwxyz"; //68
        char[] plantChar = plantTxt.toCharArray();
        StringBuilder box = new StringBuilder(len);
        int planttxtLen = plantTxt.length();
        for (int i = 0; i < len; i++) {
            Random r = new Random();
            box.append(plantChar[r.nextInt(planttxtLen)]);
        }

        return box.toString();
    }

    public static void main(String[] args) {
        String txt = "1000101w#E#测试ssAASASSC127.0.0.1lif123gsjkdsgvjxeh";
        AesUtil aes = AesUtil.build();
        String encodeText = aes.encode(txt);
        System.out.println("enctxt: "+encodeText);
        System.out.println("plant: "+txt);
        String decodeText = aes.decode(encodeText);
        System.out.println("dec  : "+decodeText);
        System.out.println("randomKey=  : "+ getRandomKey(8));

    }
}