package com.artfii.amq.tools.io.ref;


/**
 * Created by ${leeton} on 2018/10/19.
 */
public interface Reaper<T, A> {

    /**
     * Perform the cleanup action for a reference.
     *
     * @param reference the reference
     */
    void reap(Reference<T, A> reference);
}
