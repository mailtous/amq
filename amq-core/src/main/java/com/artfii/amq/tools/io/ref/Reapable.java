package com.artfii.amq.tools.io.ref;

/**
 * Created by ${leeton} on 2018/10/19.
 */
interface Reapable<T, A> {

    /**
     * Get the associated reaper.
     *
     * @return the reaper
     */
    Reaper<T, A> getReaper();
}
