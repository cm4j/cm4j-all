package com.cm4j.singleton;

import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * future 辅助类
 *
 * @author yeas.fun
 * @since 2019/9/18
 */
public class FutureSupport {

    private static final Logger log = LoggerFactory.getLogger(FutureSupport.class);

    // 默认超时：5s
    public static final int DEFAULT_TIMEOUT = 5;

    /**
     * 同步等待获取future数据 注意：如果callable内部发生异常，则get的时候会捕获到该异常，继续上抛 ---- 比较有用的就是上抛错误码异常
     *
     * @param futureWrapper
     * @param <V>
     * @return
     */
    public static <V> V get(FutureWrapper<V> futureWrapper) {
        ListenableFutureTask<FutureResult<V>> futureTask = futureWrapper.getFutureTask();
        FutureResult<V> futureResult = get(futureTask, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        // 发生异常了，则继续上抛异常
        boolean success = futureResult.isSuccess();
        if (!success) {
            RuntimeException ex = futureResult.getException();
            log.error("future result exception", ex);
            throw ex;
        }
        return futureResult.getResult();
    }

    /**
     * 同步等待获取future数据
     *
     * @param future
     * @param timeout
     * @param timeUnit
     * @param <V>
     * @return
     */
    private static <V> V get(Future<V> future, long timeout, TimeUnit timeUnit) {
        try {
            if (future.isCancelled()) {
                return (V) FutureResult.newFutureResultWithException(new RuntimeException("future cancelled"));
            }
            return future.get(timeout, timeUnit);
        } catch (Exception e) {
            log.error("future get error", e);
            throw new RuntimeException(e);
        }
    }
}
