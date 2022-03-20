package com.cm4j.singleton;

import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 任务封装类
 *
 * @author yeas.fun
 * @since 2019/10/10
 */
public class SingletonTask<T> {

    private static final Logger log = LoggerFactory.getLogger(SingletonTask.class);

    private final String hash;
    private final ListenableFutureTask<FutureResult<T>> future;

    public SingletonTask(String hash, Callable<T> callable) {
        this.hash = hash;
        this.future = ListenableFutureTask.create(() -> {
            try {
                return new FutureResult<>(callable.call());
            } catch (Exception e) {
                // 非错误码异常：则异常上报
                log.error("SingletonTask error", e);
                return FutureResult.newFutureResultWithException(e);
            }
        });
    }

    /**
     * 加入队列已满
     */
    public void onAddQueueFailed() {
        // 中止future执行
        this.future.cancel(true);
    }

    public String getHash() {
        return hash;
    }

    /**
     * 外部调用，有则返回，没有则为null
     *
     */
    public ListenableFutureTask<FutureResult<T>> getFuture() {
        return this.future;
    }
}
