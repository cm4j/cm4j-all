package com.cm4j.thread;

/**
 * 线程池配置
 *
 * @author yeas.fun
 * @version 1.0
 */
public class ThreadPoolConfig {

    /**
     * 线程池名称
     */
    private String poolName;
    /**
     * 最大线程数目
     */
    private int maxThreads;
    /**
     * 最小线程数目
     */
    private int minThreads;
    /**
     * 线程存活时间(秒)
     */
    private long keepAlive;
    /**
     * 队列类型 1:LinkedBlockingQueue 2:SynchronousQueue (暂时为1)
     */
    private int queueType;
    /**
     * 队列最大数 (空为Integer.MAX_VALUE)(最好是有值否则会出现溢出)
     */
    private int maxQueues;
    /**
     * 优先级
     */
    private int priority;
    /**
     * 拒绝策略
     */
    private ThreadPoolRejectedPolicy.RejectedPolicy policy;

    public int getMaxQueues() {
        return maxQueues;
    }

    public void setMaxQueues(int maxQueues) {
        this.maxQueues = maxQueues;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getQueueType() {
        return queueType;
    }

    public void setQueueType(int queueType) {
        this.queueType = queueType;
    }

    public ThreadPoolRejectedPolicy.RejectedPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(ThreadPoolRejectedPolicy.RejectedPolicy policy) {
        this.policy = policy;
    }
}
