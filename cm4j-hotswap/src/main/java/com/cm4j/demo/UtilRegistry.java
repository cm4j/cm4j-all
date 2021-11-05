package com.cm4j.demo;

import com.cm4j.demo.util.IUtil;
import com.cm4j.registry.AbstractClassRegistry;

/**
 * util类注册类
 * 注意：如果使用spring，这个类可以配置@Component进行扫描注册
 */
public class UtilRegistry extends AbstractClassRegistry<Class<? extends IUtil>, IUtil> {

    /**
     * 扫描包进行注册
     */
    public UtilRegistry() {
        super(IUtil.class.getPackage().getName());
        instance = this;
    }

    private static UtilRegistry instance;

    public static UtilRegistry getInstance() {
        return instance;
    }
}
