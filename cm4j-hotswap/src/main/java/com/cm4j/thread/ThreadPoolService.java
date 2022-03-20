package com.cm4j.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import static com.cm4j.thread.ThreadPoolName.COMMON;
import static com.cm4j.thread.ThreadPoolRejectedPolicy.RejectedPolicy.*;

/**
 * 线程池实现类
 */
public class ThreadPoolService {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolService.class);

    private static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    private HashMap<ThreadPoolName, ThreadPoolExecutor> poolsMap = new HashMap<>();
    /**
     * 线程池配置项
     */
    private List<ThreadPoolConfig> configPools = Collections.emptyList();
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private static ThreadPoolService instance = new ThreadPoolService();

    public static ThreadPoolService getInstance() {
        return instance;
    }

    private ThreadPoolService() {
    }

    @Override
    public void finalize() {
        this.destroyAll();
        try {
            super.finalize();
        } catch (Throwable e) {
            log.error("", e);
        }
    }

    /**
     * 用指定的线程池执行一个任务
     *
     * @param task     - 任务对象
     * @param poolName - 线程池名
     */
    public void runTask(Runnable task, ThreadPoolName poolName) {
        ThreadPoolExecutor pool = pool(poolName);
        execute(pool, task);
    }

    /**
     * 关闭全部线程池
     *
     * @return
     */
    public boolean destroyAll() {
        Iterator<ThreadPoolExecutor> iter = poolsMap.values().iterator();
        while (iter.hasNext()) {
            ThreadPoolExecutor pool = iter.next();
            if (pool != null) {
                pool.shutdown();
            }
        }
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
        return true;
    }

    /**
     * 获取对应的线程池
     *
     * @param poolName 线程池名称
     * @return
     */
    public ThreadPoolExecutor pool(ThreadPoolName poolName) {
        return poolsMap.get(poolName);
    }

    public void init() {
        for (ThreadPoolConfig config : configPools) {
            ThreadPoolName name = ThreadPoolName.valueOf(config.getPoolName());
            ThreadPoolRejectedPolicy.RejectedPolicy policy = config.getPolicy();
            if (policy == null) {
                policy = CallerRunsPolicy;
            }
            BlockingQueue<Runnable> workQueue;
            if (config.getQueueType() == 2) {
                workQueue = new SynchronousQueue<>();
            } else {
                //默认工作队列
                workQueue = new LinkedBlockingQueue<>(config.getMaxQueues() > 0 ? config.getMaxQueues() : 10000);
            }
            ThreadFactory threadFactory = DefaultThreadFactory.threadFactory(config.getPoolName(),
                    config.getPriority());
            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolRejectedPolicy(name, policy);
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(config.getMinThreads(), config.getMaxThreads(),
                    config.getKeepAlive(), TimeUnit.SECONDS, workQueue, threadFactory, rejectedExecutionHandler);
            if (config.getKeepAlive() > 0) {
                threadPool.allowCoreThreadTimeOut(true);
            }
            poolsMap.put(name, threadPool);
            if (log.isInfoEnabled()) {
                log.info("{} ThreadPoolExecutor({},{},{}s,{}) inited", config.getPoolName(), config.getMinThreads(),
                        config.getMaxThreads(), config.getKeepAlive(), config.getMaxQueues());
            }
        }
        if (!poolsMap.containsKey(COMMON)) {
            initCommonPool();
        }
        initScheduledPool();
        instance = this;
    }

    /**
     * 带定时/延时功能的线程池
     */
    private void initScheduledPool() {
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4, DefaultThreadFactory.threadFactory("STPE"),
                new ThreadPoolRejectedPolicy("STPE", CallerRunsPolicy));
    }

    /**
     * 通用线程池
     */
    private void initCommonPool() {
        int poolSize = CPU_NUM * 2;
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(4096), DefaultThreadFactory.threadFactory(COMMON.name()),
                new ThreadPoolRejectedPolicy(COMMON, CallerRunsPolicy));
        threadPool.allowCoreThreadTimeOut(true);
        poolsMap.put(COMMON, threadPool);
        if (log.isInfoEnabled()) {
            log.info("{} ThreadPoolExecutor({},{},{}s,{}) inited", COMMON, poolSize, poolSize, 60, 4096);
        }
    }

    public List<ThreadPoolConfig> getConfigPools() {
        return configPools;
    }

    public void setConfigPools(List<ThreadPoolConfig> configPools) {
        this.configPools = configPools;
    }

    /**
     * 延时指定毫秒执行task
     *
     * @param task
     * @param delay 需要延时的毫秒数
     * @return
     */
    public ScheduledFuture<?> schedule(final Runnable task, long delay) {
        return schedule(scheduledThreadPoolExecutor, task, Math.max(0, delay), TimeUnit.MILLISECONDS);
    }

    /**
     * 以固定频率来执行任务，如果任务执行时间大于固定频率，在任务执行完成后会立即执行下一次任务
     *
     * <pre>
     * 如initialDelay=0，period=3，任务执行时间为5，则执行任务的周期为0->5->10->15...
     * </pre>
     *
     * @param task
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduleAtFixedRate(scheduledThreadPoolExecutor, task, initialDelay, period, unit);
    }

    /**
     * 任务执行完后再延时固定的时间后再执行下一次任务
     *
     * <pre>
     *     如initialDelay=0，delay=3，任务执行时间为5，则执行任务的周期为0->8->16->24...
     * </pre>
     *
     * @param task
     * @param initialDelay
     * @param delay
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return scheduleWithFixedDelay(scheduledThreadPoolExecutor, task, initialDelay, delay, unit);
    }

    /**
     * 用指定的线程池执行一个任务
     *
     * @param pool - 线程池
     * @param task - 任务对象
     */
    public void execute(ExecutorService pool, Runnable task) {
        pool.execute(task);
    }


    /**
     * 延时指定毫秒执行task
     *
     * @param pool
     * @param task
     * @param delay 需要延时的数值
     * @param unit  时间单位
     * @return
     */
    public ScheduledFuture<?> schedule(ScheduledExecutorService pool, final Runnable task, long delay, TimeUnit unit) {
        return pool.schedule(task, delay, unit);
    }

    /**
     * 以固定频率来执行任务，如果任务执行时间大于固定频率，在任务执行完成后会立即执行下一次任务(默认监控且上报)
     *
     * <pre>
     * 如initialDelay=0，period=3，任务执行时间为5，则执行任务的周期为0->5->10->15...
     * </pre>
     *
     * @param threadPoolExecutor
     * @param task
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleAtFixedRate(ScheduledExecutorService threadPoolExecutor, Runnable task,
                                                  long initialDelay, long period, TimeUnit unit) {
        return scheduleAtFixedRate(threadPoolExecutor, task, initialDelay, period, unit);
    }


    /**
     * 任务执行完后再延时固定的时间后再执行下一次任务
     *
     * <pre>
     *     如initialDelay=0，delay=3，任务执行时间为5，则执行任务的周期为0->8->16->24...
     * </pre>
     *
     * @param threadPoolExecutor
     * @param task
     * @param initialDelay
     * @param delay
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(ScheduledExecutorService threadPoolExecutor, Runnable task,
                                                     long initialDelay, long delay, TimeUnit unit) {
        return threadPoolExecutor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

}
