package com.cm4j.demo.util;

import com.cm4j.demo.UtilRegistry;

/**
 * @author yeas.fun
 * @since 2021/11/4
 */
public class DemoUtil implements IUtil {

    public void hello() {
        System.out.println("hello world !");
    }

    public static DemoUtil getInstance() {
        return (DemoUtil) UtilRegistry.getInstance().get(DemoUtil.class);
    }
}
