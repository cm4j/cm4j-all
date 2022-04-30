package com.cm4j.registry.registry;

import com.cm4j.invoke.IRemotingClass;
import com.cm4j.registry.AbstractRegistry;
import com.cm4j.util.RemotingInvokerUtil;
import com.esotericsoftware.reflectasm.MethodAccess;

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

    @Override
    public void doAfterRegister(String registryKey, IRemotingClass newObject) {
        // 注册新的MethodAccess
        MethodAccess access = MethodAccess.get(newObject.getClass());//生成字节码的方式
        RemotingInvokerUtil.addInvoker(registryKey, access);
    }

    @Override
    public void doAfterHotswap(String registryKey, IRemotingClass oldObject, IRemotingClass newObject) {
        // 这里不要多调，会产生新的类无法被GC
        // 功能依然正常：reflectasm 是基于方法名字和参数来获取索引，从而调用target对应索引的方法，所以功能正常
        // doAfterRegister(registryKey, newObject);
    }

    @Override
    public void doAfterRegisterNewOne(String registryKey, IRemotingClass newObject) {
        // 注册新的MethodAccess
        doAfterRegister(registryKey, newObject);
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
