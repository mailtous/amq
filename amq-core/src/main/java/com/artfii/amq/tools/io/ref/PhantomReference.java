package com.artfii.amq.tools.io.ref;

import java.lang.ref.ReferenceQueue;

/**
 * Created by ${leeton} on 2018/10/19.
 */
public class PhantomReference<T, A> extends java.lang.ref.PhantomReference<T> implements Reference<T, A>, Reapable<T, A> {
    private final A attachment;
    private final Reaper<T, A> reaper;

    /**
     * Construct a new instance with an explicit reference queue.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param q the reference queue to use
     */
    public PhantomReference(final T referent, final A attachment, final ReferenceQueue<? super T> q) {
        super(referent, q);
        this.attachment = attachment;
        reaper = null;
    }

    /**
     * Construct a new instance with a reaper.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param reaper the reaper to use
     */
    public PhantomReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, References.ReaperThread.REAPER_QUEUE);
        this.reaper = reaper;
        this.attachment = attachment;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.PHANTOM;
    }

    public Reaper<T, A> getReaper() {
        return reaper;
    }

    public String toString() {
        return "phantom reference";
    }
}
