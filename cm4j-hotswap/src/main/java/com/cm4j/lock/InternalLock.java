package com.cm4j.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.util.ThreadUtil;

/**
 * @author yeas.fun
 * @since 2021/11/25
 */
public class InternalLock implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(InternalLock.class);

    /**
     * 获取锁的超时时间，单位：s
     */
    private final int LOCK_TIME_OUT = 5;

    private final Lock lock = new ReentrantLock();
    // 当前持有锁的线程
    private Thread holdThread;

    public void tryLock() {
        boolean success = false;
        try {
            success = lock.tryLock(LOCK_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        if (success) {
            holdThread = Thread.currentThread();
        } else {
            // 堆栈信息
            String currentStack = ThreadUtil.getThreadStackTrace(Thread.currentThread());
            String holdStack = ThreadUtil.getThreadStackTrace(holdThread);

            log.error("=========== 获取InternalLock超时，可能发生死锁===========\n【当前线程】堆栈：{}\n{}\n【持有锁线程】堆栈：{}\n{}",
                    Thread.currentThread(), currentStack, holdThread, holdStack);
            throw new RuntimeException("获取InternalLock超时，可能发生死锁");
        }
    }

    @Override
    public void close() {
        holdThread = null;
        lock.unlock();
    }
}
