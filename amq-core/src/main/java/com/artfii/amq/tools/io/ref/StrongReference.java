package com.artfii.amq.tools.io.ref;

/**
 * Created by ${leeton} on 2018/10/19.
 */
public class StrongReference<T, A> implements Reference<T, A> {

    private volatile T referent;
    private final A attachment;

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     * @param attachment the attachment
     */
    public StrongReference(final T referent, final A attachment) {
        this.referent = referent;
        this.attachment = attachment;
    }

    /**
     * Construct a new instance.
     *
     * @param referent the referent
     */
    public StrongReference(final T referent) {
        this(referent, null);
    }

    public T get() {
        return referent;
    }

    public void clear() {
        referent = null;
    }

    public A getAttachment() {
        return attachment;
    }

    public Type getType() {
        return Type.STRONG;
    }

    public String toString() {
        return "strong reference to " + String.valueOf(get());
    }
}