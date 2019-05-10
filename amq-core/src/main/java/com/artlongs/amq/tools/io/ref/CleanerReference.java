package com.artlongs.amq.tools.io.ref;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ${leeton} on 2018/10/19.
 */
public class CleanerReference<T, A> extends PhantomReference<T, A> {
    private static final Set<CleanerReference<?, ?>> set = Collections.newSetFromMap(new ConcurrentHashMap<CleanerReference<?, ?>, Boolean>());

    /**
     * Construct a new instance with a reaper.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param reaper the reaper to use
     */
    public CleanerReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, attachment, reaper);
        set.add(this);
    }

    void clean() {
        set.remove(this);
    }

    public final int hashCode() {
        return super.hashCode();
    }

    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }
}
