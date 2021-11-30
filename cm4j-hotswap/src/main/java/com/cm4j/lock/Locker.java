package com.cm4j.lock;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * 替换synchronized锁，一旦出现死锁，会打印死锁线程和当前线程
 *
 * <pre>
 *     // 新写法
 *     Object obj = new Object();
 *     try (InternalLock ignored = Locker.getLockObjected(obj)) {
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
 * @author yanghao
 * @since 2021/11/25
 */
public class Locker {

    // 锁对象缓存，仅1min
    private static final LoadingCache<String, InternalLock> lockCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new InternalLock());

    /**
     * 根据名字获取锁
     * 名字相同即是同一把锁
     *
     * @param lockName
     * @return
     */
    public static InternalLock getLockNamed(String lockName) {
        try {
            InternalLock lock = lockCache.get(lockName);
            lock.tryLock();
            return lock;
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
        }
        return null;
    }

    /**
     * 根据对象获取锁
     * 对象相同即是同一把锁
     *
     * @param lockTarget
     * @return
     */
    public static InternalLock getLock(Object lockTarget) {
        Preconditions.checkNotNull(lockTarget, "获取锁不允许null");
        Preconditions.checkArgument(!(lockTarget instanceof String), "获取锁不允许是字符串:" + lockTarget);

        return getLockNamed(identity(lockTarget));
    }

    /**
     * 返回对象的唯一标识
     *
     * @param obj
     * @return
     */
    private static String identity(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
}
