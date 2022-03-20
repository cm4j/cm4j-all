package com.cm4j.thread;

import com.google.common.collect.Lists;

import static org.junit.Assert.*;

/**
 * @author yeas.fun
 * @since 2022/3/17
 */
public class ThreadPoolServiceTest {

    /**
     * 初始化线程池
     */
    public static void initThreadPoolService() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setKeepAlive(60);
        config.setPoolName(ThreadPoolName.SINGLETON.name());
        config.setMinThreads(10);
        config.setMaxThreads(80);
        config.setQueueType(1);
        config.setMaxQueues(100);
        config.setPriority(5);
        config.setPolicy(ThreadPoolRejectedPolicy.RejectedPolicy.DiscardPolicy);

        ThreadPoolService.getInstance().setConfigPools(Lists.newArrayList(config));
        ThreadPoolService.getInstance().init();
    }
}