package com.cm4j.registry.registry;

import com.cm4j.invoke.IRemotingClass;
import com.cm4j.registry.AbstractRegistry;

/**
 * 条件注册
 *
 * @author yeas.fun
 * @since 2021/1/12
 */
public class InvokerRegistry extends AbstractRegistry<String, IRemotingClass> {

    /**
     * 扫描包进行注册
     */
    protected InvokerRegistry() {
        super(IRemotingClass.class.getPackage().getName());
        instance = this;
    }

    private static InvokerRegistry instance;

    public static InvokerRegistry getInstance() {
        return instance;
    }

    @Override
    protected String[] getRegistryKeys(IRemotingClass iRemotingClass) {
        return new String[]{iRemotingClass.getClass().getName()};
    }
}
