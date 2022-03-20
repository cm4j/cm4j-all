package com.cm4j.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DefaultThreadFactory
 *
 * @author yeas.fun
 * @since 2020-09-25
 */
public class DefaultThreadFactory implements ThreadFactory {

    private static final AtomicInteger POOL_SEQ = new AtomicInteger();
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();
    private static final String DEFAULT_POOL_NAME = "pool";
    private static final String DEFAULT_THREAD_NAME = "thread";
    private final ThreadGroup group;
    private final String namePrefix;
    private final boolean daemon;
    private final int priority;

    private DefaultThreadFactory(String poolName, String threadName, boolean daemon, int priority) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = poolName + "-" + POOL_SEQ.incrementAndGet() + "-" + threadName + "-";
        this.daemon = daemon;
        this.priority = priority;
    }

    /**
     * 创建线程工厂
     *
     * @param poolName   线程池名称
     * @param threadName 线程名称前缀
     * @param daemon     是否是守护线程
     * @param priority   线程优先级
     * @return poolName-threadName-N
     */
    public static ThreadFactory threadFactory(String poolName, String threadName, boolean daemon, int priority) {
        return new DefaultThreadFactory(poolName, threadName, daemon, priority);
    }

    /**
     * @param poolName
     * @param threadName
     * @return poolName-threadName-N
     */
    public static ThreadFactory threadFactory(String poolName, String threadName) {
        return threadFactory(poolName, threadName, false, Thread.NORM_PRIORITY);
    }

    /**
     * @param poolName
     * @return poolName-thread-N
     */
    public static ThreadFactory threadFactory(String poolName) {
        return threadFactory(poolName, DEFAULT_THREAD_NAME, false, Thread.NORM_PRIORITY);
    }

    /**
     * @param poolName
     * @param daemon
     * @return poolName-thread-N
     */
    public static ThreadFactory threadFactory(String poolName, boolean daemon) {
        return threadFactory(poolName, DEFAULT_THREAD_NAME, daemon, Thread.NORM_PRIORITY);
    }

    public static ThreadFactory threadFactory(String poolName, int priority) {
        return threadFactory(poolName, DEFAULT_THREAD_NAME, false, priority);
    }

    /**
     * @return pool-thread-N
     */
    public static ThreadFactory threadFactory() {
        return threadFactory(DEFAULT_POOL_NAME);
    }

    /**
     * @param daemon
     * @return pool-thread-N
     */
    public static ThreadFactory threadFactory(boolean daemon) {
        return threadFactory(DEFAULT_POOL_NAME, daemon);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + THREAD_SEQ.incrementAndGet(), 0);
        t.setDaemon(daemon);
        if (t.getPriority() != priority) {
            t.setPriority(priority);
        }
        return t;
    }
}
