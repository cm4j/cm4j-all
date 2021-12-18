package com.cm4j.lock;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author yeas.fun
 * @since 2021/11/29
 */
public class LockerTest {

    /**
     * 基于对象使用锁
     */
    @Test
    public void test2() {
        TTT obj = new TTT();

        new Thread(() -> {
            try (InternalLock ignored = Locker.getLock(obj)) {
                System.out.println(Thread.currentThread() + ">>> olai olai ooo...");
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));
            }
        }).start();
        new Thread(() -> {
            try (InternalLock ignored = Locker.getLock(obj)) {
                System.out.println(Thread.currentThread() + ">>> olai olai ooo...");
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));
            }
        }).start();
        LockSupport.park();
    }

    @Test
    public void test3() {
        System.out.println(System.identityHashCode(new TTT()));
        System.out.println(System.identityHashCode(new TTT()));
    }

    /**
     * 嵌套锁测试
     */
    @Test
    public void deadLockTest() {
        TTT o1 = new TTT();
        TTT o2 = new TTT();

        // 下面代码运行时会出现嵌套锁
        Thread t1 = new Thread(() -> {
            try (InternalLock ignored = Locker.getLock(o1)) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                try (InternalLock ignored2 = Locker.getLock(o2)) {
                    System.out.println(Thread.currentThread() + "111111>>> olai olai ooo...");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            try (InternalLock ignored = Locker.getLock(o2)) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                try (InternalLock ignored2 = Locker.getLock(o1)) {
                    System.out.println(Thread.currentThread() + "222222>>> olai olai ooo...");
                }
            }
        });
        t1.start();
        t2.start();

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));
        System.out.println("t1状态：" + t1.getState());
        System.out.println("t2状态：" + t2.getState());
    }

    private static class TTT {

        @Override
        public int hashCode() {
            return 123456;
        }
    }
}