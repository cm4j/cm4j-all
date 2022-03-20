package com.cm4j.singleton;

import com.cm4j.thread.ThreadPoolName;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 单线程执行模块
 *
 * @author yeas.fun
 * @since 2019/10/10
 */
public abstract class SingletonModule {

    /** 必须是2的n次方，因为后面用它hash定位 */
    private final int SIZE = 1 << 8;

    private final SingletonTaskQueue[] queues;

    protected SingletonModule(ThreadPoolName threadPoolName) {
        queues = new SingletonTaskQueue[SIZE];
        for (int i = 0; i < SIZE; i++) {
            queues[i] = new SingletonTaskQueue(i, threadPoolName);
        }
    }

    /**
     * 添加任务 框架保证：
     * 
     * <pre>
     *     1.function 作为唯一键，该任务仅有1个线程会执行，避免并发和加锁
     *     2.callable如果抛异常了，则同步方法FutureSupport.get()也会上抛同样的异常，一般用于直接返回错误码
     * </pre>
     * 
     * 注意：因为是单线程执行，所以不要把执行特别耗时的任务放进来，否则会卡住所有任务
     *
     * @param function 功能枚举
     * @param callable 执行逻辑
     * @param <T> 返回值
     * @return future
     */
    public <T> FutureWrapper<T> addTask(SingletonEnum function, Callable<T> callable) {
        return addTask(function.name(), callable);
    }

    /**
     * 添加任务 框架保证：
     * 
     * <pre>
     *     1.function+uniqueId 作为唯一键，同一键同时仅有1个线程会执行，避免并发和加锁
     *     2.callable如果抛异常了，则同步方法FutureSupport.get()也会上抛同样的异常，一般用于直接返回错误码
     * </pre>
     *
     * 注意：因为是单线程执行，所以不要把执行特别耗时的任务放进来，否则会卡住所有任务
     *
     * @param function 功能枚举
     * @param uniqueId 唯一标识ID
     * @param callable 执行逻辑
     * @param <T> 返回值
     * @return future
     */
    public <T> FutureWrapper<T> addTask(SingletonEnum function, String uniqueId,
        Callable<T> callable) {
        uniqueId = StringUtils.isBlank(uniqueId) ? "" : uniqueId;
        return addTask(function.name() + uniqueId, callable);
    }

    /**
     * 添加任务执行
     *
     * @param <T>
     * @param hash
     * @param callable
     * @return
     */
    private <T> FutureWrapper<T> addTask(String hash, Callable<T> callable) {
        SingletonTask<T> task = new SingletonTask<>(hash, callable);
        int rehash = rehash(task.getHash().hashCode());

        int idx = rehash & (SIZE - 1);
        SingletonTaskQueue queue = queues[idx];
        // 增加到对应的队列
        queue.addTask(task);
        // 每个事件，都有对应的future
        return new FutureWrapper<T>(task.getFuture());
    }

    /**
     * 队列任务数量
     *
     * @return
     */
    public Map<Integer, Integer> getTaskNum() {
        SingletonTaskQueue[] dealers = this.queues;
        Map<Integer, Integer> result = new HashMap<>(dealers.length);
        for (int i = 0; i < dealers.length; i++) {
            SingletonTaskQueue dealer = dealers[i];
            result.put(dealer.getIdx(), dealer.getQueueSize());
        }
        return result;
    }

    /**
     * rehash算法，使hash值分布更均匀
     *
     * @param h
     * @return
     */
    private static int rehash(int h) {
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }
}
