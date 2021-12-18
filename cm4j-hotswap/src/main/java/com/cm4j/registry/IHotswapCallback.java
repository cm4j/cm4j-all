package com.cm4j.registry;

/**
 * 热更后回调
 *
 * @author yeas.fun
 * @since 2021/3/17
 */
public interface IHotswapCallback<K, V> {

    /**
     * 初始化注册：回调
     *
     * @param registryKey
     * @param newObject
     */
    default void doAfterRegister(K registryKey, V newObject){}

    /**
     * 热更：替换旧回调
     *
     * @param registryKey registry键
     * @param oldObject   旧对象
     * @param newObject   新对象
     */
    default void doAfterHotswap(K registryKey, V oldObject, V newObject){}

    /**
     * 热更：新增对象后回调
     *
     * @param registryKey
     * @param newObject
     */
    default void doAfterRegisterNewOne(K registryKey, V newObject){}
}
