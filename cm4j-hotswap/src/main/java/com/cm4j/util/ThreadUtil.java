package com.cm4j.util;

import com.google.common.base.Joiner;

/**
 * @author yeas.fun
 * @since 2021/11/29
 */
public class ThreadUtil {

    /**
     * 获取指定线程堆栈信息
     *
     * @param thread
     * @return
     */
    public static String getThreadStackTrace(Thread thread) {
        return Joiner.on("\n").join(thread.getStackTrace());
    }
}
