package com.cm4j.invoke.invoker;

/**
 * @author yeas.fun
 * @since 2021/8/23
 */
public interface IRemotingInvoker {

    /**
     * 方法调用
     *
     * @param methodName 方法定位标识
     * @param params     方法需要的参数
     * @return 方法的返回结果
     */
    Object invokeInternal(String methodName, Object[] params) throws Exception;
}
