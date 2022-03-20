package com.cm4j.thread;

import java.util.concurrent.*;

/**
 * 线程池创建工厂
 *
 * @author yeas.fun
 * @since 2021-05-14
 */
public class ThreadPoolFactory {

    public static ExecutorService newCachedThreadPool(String poolName) {
        return new ThreadPoolExecutor(0, 4096, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                DefaultThreadFactory.threadFactory(poolName));
    }

    public static ExecutorService newSingleThreadExecutor(String poolName) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                DefaultThreadFactory.threadFactory(poolName));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String poolName) {
        return newScheduledThreadPool(1, poolName);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String poolName) {
        return new ScheduledThreadPoolExecutor(corePoolSize, DefaultThreadFactory.threadFactory(poolName));
    }
}
