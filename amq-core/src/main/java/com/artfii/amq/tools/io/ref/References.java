package com.artfii.amq.tools.io.ref;

import java.lang.ref.ReferenceQueue;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;

import static java.security.AccessController.doPrivileged;

/**
 * Created by ${leeton} on 2018/10/19.
 */
public class References {

    private References() {
    }

    private static final Reference<?, ?> NULL = new Reference<Object, Object>() {
        public Object get() {
            return null;
        }

        public Object getAttachment() {
            return null;
        }

        public void clear() {
        }

        public Type getType() {
            return Type.NULL;
        }

        public String toString() {
            return "NULL reference";
        }
    };

    static final class ReaperThread extends Thread {
        static final ReferenceQueue<Object> REAPER_QUEUE = new ReferenceQueue<Object>();

        static {
            final AtomicInteger cnt = new AtomicInteger(1);
            final PrivilegedAction<Void> action = () -> {
                final ReaperThread thr = new ReaperThread();
                thr.setName("Reference Reaper #" + cnt.getAndIncrement());
                thr.setDaemon(true);
                thr.start();
                return null;
            };
            for (int i = 0; i < 3; i ++) {
                doPrivileged(action);
            }
        }

        public void run() {
            for (;;) try {
                final java.lang.ref.Reference<?> ref = REAPER_QUEUE.remove();
                if (ref instanceof CleanerReference) {
                    ((CleanerReference<?, ?>) ref).clean();
                }
                if (ref instanceof Reapable) {
                    reap((Reapable<?, ?>) ref);
                }
            } catch (InterruptedException ignored) {
                // we consume interrupts.
            } catch (Throwable cause) {
                cause.printStackTrace();
            }
        }

        @SuppressWarnings({ "unchecked" })
        private static <T, A> void reap(final Reapable<T, A> reapable) {
            reapable.getReaper().reap((Reference<T, A>) reapable);
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reaper argument is ignored.  If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param reaper the reaper to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment, Reaper<T, A> reaper) {
        if (value == null) return getNullReference();
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, reaper);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, reaper);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, reaper);
            case NULL:
                return getNullReference();
            default:
                throw new RuntimeException("cash error:"+type.toString());
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#STRONG} or {@link Reference.Type#NULL} then the reference queue argument is ignored.  If
     * the reference type is {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param referenceQueue the reference queue to use, if any
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment, ReferenceQueue<? super T> referenceQueue) {
        if (referenceQueue == null) return create(type, value, attachment);
        if (value == null) return getNullReference();
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment, referenceQueue);
            case PHANTOM:
                return new PhantomReference<T, A>(value, attachment, referenceQueue);
            case SOFT:
                return new SoftReference<T, A>(value, attachment, referenceQueue);
            case NULL:
                return getNullReference();
            default:
                throw new RuntimeException("cash error:"+type.toString());
        }
    }

    /**
     * Create a reference of a given type with the provided value and attachment.  If the reference type is
     * {@link Reference.Type#PHANTOM} then this method will return a {@code null} reference because
     * such references are not constructable without a queue or reaper.  If the reference type is
     * {@link Reference.Type#NULL} then the value and attachment arguments are ignored.
     *
     * @param type the reference type
     * @param value the reference value
     * @param attachment the attachment value
     * @param <T> the reference value type
     * @param <A> the reference attachment type
     * @return the reference
     */
    public static <T, A> Reference<T, A> create(Reference.Type type, T value, A attachment) {
        if (value == null) return getNullReference();
        switch (type) {
            case STRONG:
                return new StrongReference<T, A>(value, attachment);
            case WEAK:
                return new WeakReference<T, A>(value, attachment);
            case PHANTOM:
                return getNullReference();
            case SOFT:
                return new SoftReference<T, A>(value, attachment);
            case NULL:
                return getNullReference();
            default:
                throw new RuntimeException("cash error:"+type.toString());
        }
    }

    /**
     * Get a null reference.  This reference type is always cleared and does not retain an attachment; as such
     * there is only one single instance of it.
     *
     * @param <T> the reference value type
     * @param <A> the attachment value type
     * @return the null reference
     */
    @SuppressWarnings({ "unchecked" })
    public static <T, A> Reference<T, A> getNullReference() {
        return (Reference<T, A>) NULL;
    }
}
