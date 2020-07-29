package com.artfii.amq.core.aio;

/**
 * Func : Call back
 * Created by leeton on 2019/1/15.
 */
@FunctionalInterface
public interface Call<V> {

    void back(V v);

}


