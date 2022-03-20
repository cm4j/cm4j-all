package com.cm4j.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池拒绝策略
 * <pre>
 *     前10次触发拒绝都上报
 *     以后每100次上报一次
 * </pre>
 *
 * @author yeas.fun
 * @since 2021-08-18
 */
public final class ThreadPoolRejectedPolicy implements RejectedExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolRejectedPolicy.class);
    /**
     * 回车换行
     */
    public static final String CRLF = "\r\n";
    /**
     * 线程池名称
     */
    private final String threadPoolName;
    /**
     * 被拒绝计数器
     */
    private final AtomicInteger rejectedCounter;
    /**
     * 拒绝策略
     */
    private final RejectedExecutionHandler handler;
    private final RejectedPolicy policy;

    public ThreadPoolRejectedPolicy(String threadPoolName, RejectedPolicy policy) {
        this.threadPoolName = threadPoolName;
        this.rejectedCounter = new AtomicInteger();
        this.policy = policy;
        this.handler = createPolicy(policy);
    }

    public ThreadPoolRejectedPolicy(ThreadPoolName name, RejectedPolicy policy) {
        this(name.name(), policy);
    }

    /**
     * 根据拒绝策略创建不同的拒绝处理器
     * <pre>
     *     默认策略:CallerRunsPolicy
     * </pre>
     *
     * @param policy
     * @return
     */
    private RejectedExecutionHandler createPolicy(RejectedPolicy policy) {
        if (policy == RejectedPolicy.AbortPolicy) {
            return new ThreadPoolExecutor.AbortPolicy();
        }
        if (policy == RejectedPolicy.DiscardPolicy) {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
        if (policy == RejectedPolicy.DiscardOldestPolicy) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        int rejected = rejectedCounter.incrementAndGet();
        if (needReport(rejected)) {
            reportRejected(r, executor, rejected);
        }
        handler.rejectedExecution(r, executor);
    }

    /**
     * 判断是否需要上报拒绝信息
     * <pre>
     *     前10次拒绝都上报
     *     以后每100次上报一次
     * </pre>
     *
     * @param rejected 已触发的拒绝次数
     * @return
     */
    private boolean needReport(int rejected) {
        return rejected <= 10 || rejected % 100 == 0;
    }

    /**
     * 上报拒绝信息
     *
     * @param r
     * @param executor
     * @param rejected
     */
    private void reportRejected(Runnable r, ThreadPoolExecutor executor, int rejected) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(threadPoolName).append(" rejected ").append(rejected).append(" times");
        sb.append(" by ").append(policy).append(CRLF);
        sb.append("Task ").append(r).append(" rejected from ").append(executor).append(CRLF);
        for (StackTraceElement b : Thread.currentThread().getStackTrace()) {
            sb.append(b).append(CRLF);
        }
        String detail = sb.toString();
        log.error(detail);
    }

    /**
     * 线程池拒绝策略
     */
    public enum RejectedPolicy {
        /**
         * 抛异常
         */
        AbortPolicy,
        /**
         * 直接丢弃
         */
        DiscardPolicy,
        /**
         * 丢弃队列中最老的任务
         */
        DiscardOldestPolicy,
        /**
         * 将任务分给调用线程来执行
         */
        CallerRunsPolicy
    }
}
