package com.cm4j.singleton.impl;

import com.cm4j.singleton.SingletonModule;
import com.cm4j.thread.ThreadPoolName;

/**
 * 常规业务使用的单线程
 *
 * @author yeas.fun
 * @since 2021/7/19
 */
public class NormalSingletonModule extends SingletonModule {

    private NormalSingletonModule() {
        super(ThreadPoolName.SINGLETON);
    }

    private static class HOLDER {
        private static final SingletonModule instance = new NormalSingletonModule();
    }

    public static SingletonModule getInstance() {
        return HOLDER.instance;
    }
}
