package com.cm4j.singleton;

/**
 * @author yeas.fun
 * @since 2019/9/20
 */
public class FutureResult<V> {

    private RuntimeException exception;

    private final V result;

    public FutureResult(V result) {
        this.exception = null;
        this.result = result;
    }

    public static <V> FutureResult<V> newFutureResultWithException(Exception exception) {
        FutureResult<V> result = new FutureResult<>(null);
        // 是RuntimeException，则直接返回，否则封装下
        if (exception instanceof RuntimeException) {
            result.exception = (RuntimeException)exception;
        } else {
            result.exception = new RuntimeException(exception);
        }
        return result;
    }

    /**
     * 是否正常返回【无异常】
     *
     * @return
     */
    public boolean isSuccess() {
        return this.exception == null;
    }

    public RuntimeException getException() {
        return exception;
    }

    public V getResult() {
        return result;
    }
}
