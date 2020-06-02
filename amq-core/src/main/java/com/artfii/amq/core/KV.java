package com.artfii.amq.core;

import java.io.Serializable;

/**
 *
 * Created by ${leeton} on 2018/12/13.
 */
public interface KV<K,V> extends Serializable {

    V get(K k);
    KV put(K k,V v);


}
