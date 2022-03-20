package com.cm4j.singleton;

import com.cm4j.thread.ThreadPoolName;
import com.cm4j.thread.ThreadPoolService;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yeas.fun
 * @since 2019/10/10
 */
public class SingletonTaskQueue implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonTaskQueue.class);

    private final int idx;
    private final ThreadPoolName threadPoolName;
    /**
     * 状态标识位，同一个线程为true时才可执行
     */
    private final AtomicBoolean isDealing;
    /**
     * 队列
     */
    private final LinkedBlockingQueue<SingletonTask> queue;

    SingletonTaskQueue(int idx, ThreadPoolName threadPoolName) {
        this.idx = idx;
        this.threadPoolName = threadPoolName;
        this.queue = Queues.newLinkedBlockingQueue(2048);
        isDealing = new AtomicBoolean(false);
    }

    public void addTask(SingletonTask task) {
        boolean success = queue.offer(task);
        // 添加队列失败
        if (!success) {
            task.onAddQueueFailed();
            return;
        }
        if (executable()) {
            ThreadPoolService.getInstance().runTask(this, threadPoolName);
        }
    }

    private boolean executable() {
        return isDealing.compareAndSet(false, true);
    }

    private void execute() {
        try {
            while (true) {
                try {
                    SingletonTask task = this.queue.poll();

                    // 没有对象，则结束循环
                    if (task == null) {
                        // 并发问题：如果有断点在这里
                        // 另一个线程把event放入队列中，且因为没有获取到锁，则快速失败，当前线程也break了，则有event无法消耗
                        // 所以：在finally段队列二次检查
                        break;
                    }

                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Singleton task[{}] triggered", task.getHash());
                    }
                    ListenableFutureTask future = task.getFuture();
                    // 同步执行
                    future.run();
                } catch (Exception e) {
                    LOG.error("SingletonTaskQueue[{}] error", SingletonTaskQueue.this.idx, e);
                }
            }
        } finally {
            // 最后一定要把标识位修改为false
            this.isDealing.set(false);

            // 因为上面已经已经把状态isDealing重置了，如果队列里有对象，则继续放到线程池执行
            if (!this.queue.isEmpty() && executable()) {
                execute();
            }
        }
    }

    @Override
    public void run() {
        execute();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getIdx() {
        return idx;
    }

}
