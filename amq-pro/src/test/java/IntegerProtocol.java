/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: IntegerProtocol.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

import com.artfii.amq.core.aio.Protocol;

import java.nio.ByteBuffer;

/**
 * Created by 三刀 on 2018/08/23.
 */
public class IntegerProtocol implements Protocol<Integer> {

    private static final int INT_LENGTH = 4;

    @Override
    public Integer decode(ByteBuffer data) {
        if (data.remaining() < INT_LENGTH){
            return null;
        }
        Integer nums = data.getInt();
        return nums;
    }

    @Override
    public ByteBuffer encode(Integer v) {
        byte[] cacheByte = new byte[8];
        cacheByte[0] = (byte) ((v >>> 24) & 0xFF);
        cacheByte[1] = (byte) ((v >>> 16) & 0xFF);
        cacheByte[2] = (byte) ((v >>> 8) & 0xFF);
        cacheByte[3] = (byte) ((v >>> 0) & 0xFF);

        ByteBuffer b = ByteBuffer.allocate(cacheByte.length);
       return b.wrap(cacheByte);
    }


}
