package com.cm4j.singleton;

import com.google.common.util.concurrent.ListenableFutureTask;

/**
 * 外部不需要直接调用到futureTask。必须通过 FutureSupport.get(futureWrapper) 获取值
 *
 * @author yeas.fun
 * @since 2022/2/24
 */
public class FutureWrapper<V> {

    private final ListenableFutureTask<FutureResult<V>> futureTask;

    public FutureWrapper(ListenableFutureTask<FutureResult<V>> futureTask) {
        this.futureTask = futureTask;
    }

    /**
     * 同步等待获取结果，这里会走FutureSupport，默认等待5s
     *
     * @return
     */
    public V getResult() {
        return FutureSupport.get(this);
    }

    ListenableFutureTask<FutureResult<V>> getFutureTask() {
        return futureTask;
    }
}
