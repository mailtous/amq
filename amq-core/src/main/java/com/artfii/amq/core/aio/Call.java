package com.artfii.amq.core.aio;

import java.io.Serializable;

/**
 * Func : Call back
 * V : 为收到的 Message
 * Created by leeton on 2019/1/15.
 */
@FunctionalInterface
public interface Call<V> extends Serializable {

    void back(V v);

}


