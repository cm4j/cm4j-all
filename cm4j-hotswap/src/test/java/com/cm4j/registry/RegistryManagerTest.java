package com.cm4j.registry;

import java.lang.reflect.Constructor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.demo.UtilRegistry;
import com.cm4j.demo.util.DemoUtil;

/**
 * @author yeas.fun
 * @since 2021/11/8
 */
public class RegistryManagerTest {

    private static final Logger log = LoggerFactory.getLogger(RegistryManagerTest.class);

    @Before
    public void init() {
        startRegistry(UtilRegistry.class);
    }

    @Test
    public void hotswapTest() throws Exception {
        Class<? extends DemoUtil> oldClass = DemoUtil.getInstance().getClass();
        log.error("热更新前的类：{}" , oldClass);

        // 热更新
        RegistryManager.getInstance().hotswap(new String[]{DemoUtil.class.getName()});

        Class<? extends DemoUtil> newClass = DemoUtil.getInstance().getClass();
        log.error("热更新后的类(已替换为原类的子类)：{}" , newClass);

        // newClass是oldClass的子类
        Assert.assertTrue(oldClass.isAssignableFrom(newClass));
    }

    /**
     * 启动注册类，用于测试<pre>
     * <font color=red>注意：一般该类需使用spring注解进行构建，当前测试类没配置spring启动，所以这里直接初始化</font>
     *
     * @param clazz
     */
    private static void startRegistry(Class<? extends AbstractRegistry> clazz) {
        try {
            Constructor<? extends AbstractRegistry> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}