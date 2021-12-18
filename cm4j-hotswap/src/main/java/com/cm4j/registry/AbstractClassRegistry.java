package com.cm4j.registry;

import com.cm4j.registry.registered.IRegistered;

/**
 * 注册容器：类型为对象的class，值为对象本身
 *
 * @author yeas.fun
 * @since 2021/1/7
 */
public abstract class AbstractClassRegistry<K, V extends IRegistered> extends AbstractRegistry<K, V> {

    /**
     * 扫描包进行注册
     *
     * @param packScan 待扫描的包,有多个包时以","分隔
     */
    protected AbstractClassRegistry(String packScan) {
        super(packScan);
    }

    @Override
    protected K[] getRegistryKeys(V v) {
        K aClass = (K)v.getClass();
        return (K[])new Object[] {aClass};
    }
}
