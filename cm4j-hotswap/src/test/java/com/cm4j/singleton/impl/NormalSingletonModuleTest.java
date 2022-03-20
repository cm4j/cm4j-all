package com.cm4j.singleton.impl;

import com.cm4j.singleton.FutureSupport;
import com.cm4j.singleton.FutureWrapper;
import com.cm4j.singleton.SingletonEnum;
import com.cm4j.thread.ThreadPoolServiceTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * 单线程执行：单元测试
 *
 * @author yeas.fun
 * @since 2022/3/17
 */
public class NormalSingletonModuleTest {

    @BeforeClass
    public static void before() {
        // 启动线程池
        ThreadPoolServiceTest.initThreadPoolService();
    }

    @Test
    public void test() throws InterruptedException {
        NormalSingletonModule.getInstance().addTask(SingletonEnum.TEST_BUSINESS, "1234", () -> {
            System.out.println("当前线程1：" + Thread.currentThread());
            TimeUnit.SECONDS.sleep(1);
            return null;
        });


        NormalSingletonModule.getInstance().addTask(SingletonEnum.TEST_BUSINESS, "1234", () -> {
            System.out.println("当前线程2：" + Thread.currentThread());
            TimeUnit.SECONDS.sleep(1);
            return null;
        });

        // 上述2个单线程执行，因为模块一样，执行线程一致

        // 下面的key不一样，则线程不一样
        FutureWrapper<String> future = NormalSingletonModule.getInstance().addTask(SingletonEnum.TEST_BUSINESS, "1111", () -> {
            System.out.println("当前线程3：" + Thread.currentThread());
            TimeUnit.SECONDS.sleep(1);
            return "abcd";
        });

        // 同步获取结果
        String result = FutureSupport.get(future);
        System.out.println("result:" + result);

        TimeUnit.SECONDS.sleep(3);
    }
}