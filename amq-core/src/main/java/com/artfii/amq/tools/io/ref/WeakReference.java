package com.artfii.amq.tools.io.ref;

import java.lang.ref.ReferenceQueue;

/**
 * Created by ${leeton} on 2018/10/19.
 */
public class WeakReference<T, A> extends java.lang.ref.WeakReference<T> implements Reference<T, A>, Reapable<T, A> {
    private final A attachment;
    private final Reaper<T, A> reaper;

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     */
    public WeakReference(final T referent) {
        this(referent, null, (Reaper<T, A>) null);
    }

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     * @param attachment the attachment
     */
    public WeakReference(final T referent, final A attachment) {
        this(referent, attachment, (Reaper<T, A>) null);
    }

    /**
     * Construct a new instance with an explicit reference queue.
     *
     * @param referent the referent
     * @param attachment the attachment
     * @param q the reference queue to use
     */
    public WeakReference(final T referent, final A attachment, final ReferenceQueue<? super T> q) {
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
    public WeakReference(final T referent, final A attachment, final Reaper<T, A> reaper) {
        super(referent, References.ReaperThread.REAPER_QUEUE);
        this.attachment = attachment;
        this.reaper = reaper;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.WEAK;
    }

    public Reaper<T, A> getReaper() {
        return reaper;
    }

    public String toString() {
        return "weak reference to " + String.valueOf(get());
    }
}