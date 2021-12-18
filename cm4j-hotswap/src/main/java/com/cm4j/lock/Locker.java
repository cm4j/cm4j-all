package com.cm4j.lock;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.util.concurrent.TimeUnit;

/**
 * 替换synchronized锁，一旦出现死锁，会打印死锁线程和当前线程
 *
 * <pre>
 *     // 新写法
 *     Object obj = new Object();
 *     try (InternalLock ignored = Locker.getLock(obj)) {
 *          System.out.println("olai olai ooo...");
 *          LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));
 *      }
 *
 *      // 原写法
 *      synchronized (obj) {
 *          System.out.println("olai olai ooo...");
 *      }
 * </pre>
 *
 * @author yeas.fun
 * @since 2021/11/25
 */
public class Locker {

    // 锁对象缓存，仅1min
    private static final LoadingCache<Object, InternalLock> lockCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new InternalLock());

    /**
     * 根据对象获取锁
     * 对象相同即是同一把锁
     *
     * @param lockTarget
     * @return
     */
    public static InternalLock getLock(Object lockTarget) {
        Preconditions.checkNotNull(lockTarget, "获取锁不允许null");
        Preconditions.checkArgument(!lockTarget.getClass().isPrimitive(),"对象不允许是primitive");

        try {
            InternalLock lock = lockCache.get(lockTarget);
            lock.tryLock();
            return lock;
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
        }
        // 一般不会走到
        return null;
    }
}
