package com.artfii.amq.tools.cipher;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * RAS算法
 * ① 选择两个大素数 p 和 q
 *  ② 计算乘积 n = p * q 和 Fn = (p - 1) * (q - 1)
 *  ③ 选择大于 1 小于 Fn 的随机整数 e，使得 gcd(e, Fn) = 1
 *  ④ 计算 d 使得 de = 1 mod Fn
 *  ⑤ 加密变换 C = M ^ e % n（C 为加密得到的密文，M 为明文）
 *  ⑥ 解密变换 M = C ^ d % n
 */
public class Rsa {

    private static int excut_length = 0; //每次最大可转换为数字的字符数
    private static final int BYTE_LEN = 4; //双字节bit长度
    private static int piece_size = 0; //最大的加密分块长度
    private static boolean FF_MODE = false; //快省加密方式(只加密解密第一个字符,适合追求数据安全快速,但对数据保密要求不严格的场景)
    private static boolean debug = false;
    private static BigInteger[] pub_key = new BigInteger[2]; //公钥
    private static BigInteger[] self_key = new BigInteger[2]; //私钥

    // 公钥私钥中用到的两个大质数p,q'''
    private static final BigInteger p = new BigInteger("106697219132480173106064317148705638676529121742557567770857687729397446898790451577487723991083173010242416863238099716044775658681981821407922722052778958942891831033512463262741053961681512908218003840408526915629689432111480588966800949428079015682624591636010678691927285321708935076221951173426894836169");
    private static final BigInteger q = new BigInteger("144819424465842307806353672547344125290716753535239658417883828941232509622838692761917211806963011168822281666033695157426515864265527046213326145174398018859056439431422867957079149967592078894410082695714160599647180947207504108618794637872261572262805565517756922288320779308895819726074229154002310375209");

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Rsa rsa = new Rsa();
        public Builder setKey(BigInteger[] pubKey, BigInteger[] selfKey) {
            rsa.pub_key = pubKey;
            rsa.self_key = selfKey;
            return this;
        }

        public Builder fast() {
            rsa.FF_MODE = true;
            return this;
        }

        public Builder debug() {
            rsa.debug = true;
            return this;
        }

        public Rsa build() {
            return rsa;
        }
    }

    /**
     * 生成公钥/私钥
     * <pre>
     * def gen_key(p, q):
     * n = p * q 两个最大的质数
     * fn = (p - 1) * (q - 1)
     * e = 3889 随机数
     * # generate d
     * a = e
     * b = fn
     * r, x, y = ext_gcd(e, b)
     * print x
     * d = x
     * # 公钥    私钥
     * return (n, e), (n, d)
     * </pre>
     *
     * @param p
     * @param q
     * @return
     */
    public static BigInteger[][] genKey(BigInteger p, BigInteger q, BigInteger e) {
        BigInteger n = p.multiply(q);
        BigInteger fn = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        // generate d
        BigInteger[] rxy = GCD.extGcd(e, fn);
        BigInteger d = rxy[1];
        piece_size = n.toString().length();
        // 公钥  私钥
        return new BigInteger[][]{{n, e}, {n, d}};
    }

    /**
     * 生成公钥/私钥
     *
     * @param e 生成因子,随机数
     * @return
     */
    public static BigInteger[][] genKey(BigInteger e) {
        return genKey(p, q, e);
    }

    /**
     * 加密
     *
     * @param src 原文
     * @return
     */
    public List<BigInteger> encrypt(String src) {
        return encrypt(src, pub_key);
    }

    /**
     * 加密
     *
     * @param m      被加密的信息转化成为大整数m
     * @param pubkey 公钥
     * @return
     */
    public BigInteger encrypt(BigInteger m, BigInteger[] pubkey) {
        BigInteger c = expMode(m, pubkey);
        return c;
    }

    public List<BigInteger> encrypt(String src, BigInteger[] pubkey) {
        long s = System.currentTimeMillis();
        BigInteger key = arrToOne(pubkey);
        List<BigInteger> codes = encodeToBigInt(src, getDealLength(key));
        for (BigInteger code : codes) {// 对转换出来的数字进行加密
            code = encrypt(code, pubkey);
            if (FF_MODE) break;
        }
        if (debug) {
            System.err.println("encrypt use time (ms):" + (System.currentTimeMillis() - s));
        }

        return codes;
    }


    /**
     * 解密
     *
     * @param encodes 密文
     * @return
     */
    public String decrypt(List<BigInteger> encodes) {
        return decrypt(encodes, pub_key, self_key);
    }

    /**
     * 解密
     *
     * @param c       密文
     * @param selfkey 私钥
     * @return
     */
    public BigInteger decrypt(BigInteger c, BigInteger[] selfkey) {
        BigInteger m = expMode(c, selfkey);
        return m;
    }

    public String decrypt(List<BigInteger> encodes, BigInteger[] pubKey, BigInteger[] selfkey) {
        long s = System.currentTimeMillis();
        for (BigInteger code : encodes) {// 解密
            code = encrypt(code, selfkey);
            if (FF_MODE) break;
        }
        String c = decodeFormBigInt(encodes, pubKey);
        if (debug) {
            System.err.println("decrypt use time (ms):" + (System.currentTimeMillis() - s));
        }
        return c;
    }

    private BigInteger expMode(BigInteger c, BigInteger[] keys) {
        BigInteger n = keys[0];
        BigInteger d = keys[1];
        BigInteger m = Exponent.expMode(c, d, n);
        return m;
    }

    public static String keyToStr(BigInteger[] keyArr) {
        StringBuffer kb = new StringBuffer();
        for (BigInteger i : keyArr) {
            kb.append(i);
        }
        return kb.toString();
    }

    /**
     * 把key按指定的格式化输出
     * @param keyArr 公钥|私钥
     * @return
     */
    public static String formatKey(BigInteger[] keyArr){
        StringBuffer kb = new StringBuffer();
        for (BigInteger i : keyArr) {
            kb.append(i).append("|");
        }
        kb.deleteCharAt(kb.length() - 1);
        return kb.toString();
    }

    /**
     * 按KEY文件按格式还原
     * @param keyStr KEY文件
     * @return
     */
    public static BigInteger[] unFormatKey(String keyStr){
        BigInteger[] key = new BigInteger[2];
        String[] keyArr = keyStr.split("\\|");
        key[0]= new BigInteger(keyArr[0]);
        key[1]= new BigInteger(keyArr[1].split("\n")[0]);
        return key;
    }

    /**
     * 每次最大可转换为数字的字符数
     * 因为 0<M<n,所以可以多个字符处理成一个数，然后把该数进行加密
     * 这样能大大提高加密效率
     *
     * @param pubkey
     * @return
     */
    public int getDealLength(BigInteger pubkey) {
        if (excut_length != 0) return excut_length;
        // 将公钥 N 右移 21 位，当右移后的结果大于 0 时，表示可以一次处理 3 个字符
        // 3 个字符 24 位，每个字符有 1 位为符号位，除符号位剩余 21 位
        if (pubkey.shiftRight(21).compareTo(new BigInteger("0")) == 1) {
            excut_length = 3;
        } else {
            // 右移 14 位
            if (pubkey.shiftRight(14).compareTo(new BigInteger("0")) == 1) {
                excut_length = 2;
            } else {
                excut_length = 1;
            }
        }

        return excut_length;
    }


    /**
     * 原文转为数字
     * @param src 原文
     * @param bytes 每次可转换的最大字符数
     * @return
     */
    private List<BigInteger> encodeToBigInt(String src, int bytes) {
        //先对字符串进行编码,防止有中文
        byte[] codeBase64 = base64Encode(src);
        src = new String(codeBase64);
        int messagelength = src.length();
        //将字符串转换为字符数组
        char[] message = src.toCharArray();
        //用于存放密文
        List<BigInteger> encode = new ArrayList<>(messagelength);
        int x, i, j;

        /**
         * i += bytes : 一次对 bytes 个字符进行加密
         * 当字符的个数不足 bytes 时，则一次处理剩余的字符
         * 如：bytes = 3；message.length = 13 时
         *    前4次处理一次性加密3个字符，最后一次加密剩余的
         *    一个字符
         */
        for (i = 0; i < message.length; i += bytes) {
            x = 0;
            /**
             * 作用：将 bytes 个字符转换为数字
             * j < bytes ：字符剩余大于等于 bytes时，一次处理 bytes 个字符
             * (i+j) < message.length : 表示剩余字符小于 bytes，一次处理 message.length - (i+j)个字符
             */
            for (j = 0; j < bytes && (i + j) < message.length; j++) {
                /**
                 *
                 * 对每个字符进行左移位运算
                 * message[i + j] *(1 << (7 * j))：
                 *         说明：一个字符共 8 位，有 1 位为符号位不用移动；所以每个字符左移 7 位
                 *              移位后转换为数字，然后对数字进行加密操作
                 *              1 << (7 * j) --> 表示 2^(7*j)
                 *
                 *         如：同时对 AB 进行加密(AB的 ASCII码分别为 65、66)
                 *         当对 A 处理时，j = 0，x += 65
                 *         当对 B 处理时，j = 1，x += 66 * (1 << (7 * 1)) = 66 * 2^7 = 8513
                 *         此时 x 的二进制为 0010 0001 0100 0001
                 *         后八位 0100 0001 = 65 --> A
                 *               0010 0001 0000 0000 = 8448 --> 66 * 2^7 --> B * (1 << (7 * 1))
                 *
                 */
                x += message[i + j] * (1 << (7 * j));
            }
            encode.add(new BigInteger(x + ""));
        }
        return encode;
    }

    public String decodeFormBigInt(List<BigInteger> encodes, BigInteger[] pubKey) {
        int messagelength = encodes.size();
        StringBuffer decode = new StringBuffer(encodes.size());
        excut_length = getDealLength(arrToOne(pubKey));
        BigInteger x;
        for (int i = 0; i < encodes.size(); i++) {
            x = encodes.get(i);
            /**
             * 由于加密时的字符串的长度不一定是 bytes 的整数倍
             * 所以当处理到最后一个密文时，需要计算加密时，最后一次的加密
             * 加密了几个字符，然后最后解密就一次解密出几个字符
             *
             * 如：bytes = 3, 明文 length = 5
             * 第一次能一次性处理前三个字符，最后剩余的两个字符也一次处理
             * 得到的第一个密文是由前三个字符处理加密得到的，第二个密文是
             * 由最后两个字符处理加密得到的。
             * 所以解密时解密第二个密文，只需处理两次，然后解密得到两个明文
             */
            int count = excut_length;
            // 当处理最后一个密文时
            if (i == messagelength - 1) {
                // 判断最后一个密文加密时被一次处理了几个字符个数是否是bytes的整数倍
                int len = messagelength % BYTE_LEN;
                if (len != 0) {
                    count = len;
                }
            }

            /**
             * 对同时处理加密的字符进行移位解密，一次解密出 count 个字符
             */
            for (int j = 0; j < count; j++) {
                BigInteger temp = x;

                /**
                 * 假设对 x 是 AB 两个字符的移位处理的结果（看加密过程）
                 * 即 x = 8513
                 * x 的二进制为 0010 0001 0100 0001
                 * j = 0 --> 8513 % 128 = 65 --> A
                 * j = 1 --> 0010 0001 0100 0001>>7 --> 0000 0000 0100 0010 = 66 --> B
                 */
                BigInteger mod = temp.shiftRight(7 * j).mod(new BigInteger("128"));
                decode.append((char) Integer.parseInt(mod.toString()));
            }
        }

        decode.deleteCharAt(decode.length() - 1);
        //对解密出来的字符串进行中文解码
        byte[] bytes = base64Decode(decode.toString());
        return new String(bytes);
    }


    public static byte[] base64Decode(String src) {
        try {
            byte[] srcByte = src.getBytes("UTF-8");
            return Base64.getDecoder().decode(srcByte);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static byte[] base64Encode(String src) {

        try {
            byte[] srcByte = src.getBytes("UTF-8");
            byte[] n = base64Encode(srcByte);
            return n;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static byte[] base64Encode(byte[] src) {
        return Base64.getEncoder().encode(src);
    }

    private boolean isNotBlank(String src) {
        if (null == src || src == "" || src.length() == 0) return false;
        return true;
    }

    /**
     * 把输入的数据按限定的长度切块
     *
     * @param src
     * @return
     */
    private List<BigInteger> cutBigInteger(BigInteger src) {
        List<BigInteger> box = new ArrayList<>();
        String all = src.toString();
        if (all.length() > piece_size) {
            String piece = all.substring(0, piece_size);
            all = all.substring(piece.length());
            box.add(new BigInteger(piece));
        } else {
            box.add(new BigInteger(all));
        }
        return box;
    }


    private BigInteger arrToOne(BigInteger[] src) {
        StringBuffer sb = new StringBuffer(2048);
        for (BigInteger s : src) {
            sb.append(s);
        }
        BigInteger m = new BigInteger(sb.toString());
        sb = null;
        return m;
    }

    /**
     * 主要用于计算超大整数超大次幂然后对超大的整数取模。
     * "蒙哥马利算法"。
     */
    public static class Exponent {

        /**
         * 超大整数超大次幂然后对超大的整数取模
         (base ^ exponent) mod n
         * @param base
         * @param exponent
         * @param n
         * @return
         */
        public static BigInteger expMode(BigInteger base, BigInteger exponent, BigInteger n){
            char[] binaryArray = new StringBuilder(exponent.toString(2)).reverse().toString().toCharArray() ;
            int r = binaryArray.length ;
            List<BigInteger> baseArray = new ArrayList<BigInteger>() ;

            BigInteger preBase = base ;
            baseArray.add(preBase);
            for(int i = 0 ; i < r - 1 ; i ++){
                BigInteger nextBase = preBase.multiply(preBase).mod(n) ;
                baseArray.add(nextBase) ;
                preBase = nextBase ;
            }
            BigInteger a_w_b = multi(baseArray.toArray(new BigInteger[baseArray.size()]), binaryArray, n) ;
            baseArray.clear();
            binaryArray = null;
            return a_w_b.mod(n) ;
        }


        private static BigInteger multi(BigInteger[] array, char[] bin_array, BigInteger n){
            BigInteger result = BigInteger.ONE ;
            for(int index = 0 ; index < array.length ; index ++){
                BigInteger a = array[index] ;
                if(bin_array[index] == '0'){
                    continue ;
                }
                result = result.multiply(a) ;
                result = result.mod(n) ;
            }
            return result ;
        }

    }

    /**
     * 求最大公约数
     */
    public static class GCD {
        /**
         * <p>辗转相除法求最大公约数
         * @param a
         * @param b
         * @return
         */
        public static BigInteger gcd(BigInteger a, BigInteger b){
            if(b.equals(BigInteger.ZERO)){
                return a ;
            }else{
                return gcd(b, a.mod(b)) ;
            }
        }
        /**
         * <p>扩展欧几里得算法：
         * <p>求ax + by = 1中的x与y的整数解（a，b互质）
         * @param a
         * @param b
         * @return
         */
        public static BigInteger[] extGcd(BigInteger a, BigInteger b){
            if(b.equals(BigInteger.ZERO)){
                BigInteger x1 = BigInteger.ONE ;
                BigInteger y1 = BigInteger.ZERO ;
                BigInteger x = x1 ;
                BigInteger y = y1 ;
                BigInteger r = a ;
                BigInteger[] result = {r, x, y} ;
                return result ;
            }else{
                BigInteger[] temp = extGcd(b, a.mod(b)) ;
                BigInteger r  = temp[0] ;
                BigInteger x1 = temp[1] ;
                BigInteger y1 = temp[2] ;

                BigInteger x = y1 ;
                BigInteger y = x1.subtract(a.divide(b).multiply(y1)) ;
                BigInteger[] result = {r, x, y} ;
                return result ;
            }
        }
    }


    public static void main(String[] args) {
        // 生成公钥私钥'''
        BigInteger[][] keys = Rsa.genKey(new BigInteger("290413939"));
        BigInteger[] pubkey = keys[0];
        BigInteger[] selfkey = keys[1];
        System.err.println("公钥: " + keyToStr(pubkey));
        System.err.println("私钥: " + keyToStr(selfkey));
        Rsa rsa = Rsa.builder().setKey(pubkey, selfkey).fast().debug().build();

        // 需要被加密的信息转化成数字，长度小于秘钥n的长度，如果信息长度大于n的长度，那么分段进行加密，分段解密即可。'''
        BigInteger m = new BigInteger("1356205320457610288745198967657644166379972189839804389074591563666634066646564410685955217825048626066190866536592405966964024022236587593447122392540038493893121248948780525117822889230574978651418075403357439692743398250207060920929117606033490559159560987768768324823011579283223392964454439904542675637683985296529882973798752471233683249209762843835985174607047556306705224118165162905676610067022517682197138138621344578050034245933990790845007906416093198845798901781830868021761765904777531676765131379495584915533823288125255520904108500256867069512326595285549579378834222350197662163243932424184772115345");
        System.out.println("原文：" + m);
        // 信息加密'''
        BigInteger c = rsa.encrypt(m,pubkey);
        System.out.println("密文：" + c);
        // 信息解密'''
        BigInteger d = rsa.decrypt(c,selfkey);
        System.out.println("解文：" + d);
        System.out.println("原文=解文：" + m.equals(d));

        System.out.println("=================================");
        List<BigInteger> stxt = rsa.encrypt("中国");
        System.out.println("密文：" + stxt);

        String ntxt = new String(rsa.decrypt(stxt));
        System.out.println("解文：" + ntxt);

        System.out.println(rsa.formatKey(pubkey));
//        System.out.println(rsa.unFormatKey(rsa.formatKey(pubkey)));
        System.out.println(rsa.formatKey(selfkey));
    }

}

