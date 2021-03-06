package com.cm4j.test.guava.consist.usage.caches.performance;

import com.cm4j.test.guava.consist.caches.TmpFhhdCache;
import com.cm4j.test.guava.consist.cc.ConcurrentCache;
import com.cm4j.test.guava.consist.cc.SingleReference;
import com.cm4j.test.guava.consist.entity.TmpFhhd;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程异步短时间过期+写入测试
 *
 * TRUNCATE `tmp_fhhd`;
 *
 * SELECT SUM(`n_cur_token`) FROM `tmp_fhhd`;
 *
 * @author Yang.hao
 * @since 2013-3-6 上午10:12:38
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test_1/spring-ds.xml"})
public class FunctionTest {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    private final Object lock = new Object();

    /**
     * 纯缓存操作：3线程  每个运行5W次   500个数据   一次操作就是一读一写
     * 计算消耗时间：3.395193967
     * 写入消耗时间：13.899111979
     *
     * @throws InterruptedException
     * @throws BrokenBarrierException
     */
    @Test
    public void funcTest() throws InterruptedException, BrokenBarrierException {
        int num = 3;
        CyclicBarrier barrier = new CyclicBarrier(num + 1);
        AtomicLong counter = new AtomicLong();
        for (int i = 0; i < num; i++) {
            new Thread(new randomThread(counter, barrier)).start();
        }
        barrier.await();
        long start = System.nanoTime();
        barrier.await();
        long end = System.nanoTime();

        ConcurrentCache.getInstance().stop();
        long writeEnd = System.nanoTime();

        System.out.println("======================");
        System.out.println("完成，数值sum为：" + counter.get());
        System.out.println("计算消耗时间：" + (double) (end - start) / 1000000000);
        System.out.println("写入消耗时间：" + (double) (writeEnd - end) / 1000000000);
    }

    public class randomThread implements Runnable {
        private AtomicLong counter;
        private CyclicBarrier barrier;

        public randomThread(AtomicLong counter, CyclicBarrier barrier) {
            this.counter = counter;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < 5000; i++) { // 执行20000次
                    try {
                        // 这里数值越大，代表数据量越大，持久化对象越多
                        int random = RandomUtils.nextInt(300);

                        // 这一段要放在锁内，否则多线程获取ref，另一个线程remove缓存，则当前线程的ref就过期了。
                        synchronized (lock) {
                            SingleReference<TmpFhhd> ref = new TmpFhhdCache(random).ref();
                            TmpFhhd fhhd = ref.get();
                            if (fhhd == null) {
                                ref.update(new TmpFhhd(random, 1, 1, ""));

                                // 计数器放在最下面，保证上面执行成功后再计数
                                counter.incrementAndGet();
                            } else {
                                double d = RandomUtils.nextDouble();
                                if (d >= 0.5) { // >=0 一定成立，则无删除
                                    fhhd.increaseValue();
                                    fhhd.update();

                                    counter.incrementAndGet();
                                } else {
                                    fhhd.delete();

                                    counter.addAndGet(-fhhd.getNCurToken());
                                }
                            }

                            /*double v = RandomUtils.nextDouble();
                            if (v > 0.8) {
                                ref.persistAndRemove();
                            }*/
                        }

                        // 为增加并发异常，暂停10ms，注意：性能测试要关闭这个
                         TimeUnit.MILLISECONDS.sleep(10);
                    } catch (Exception e) {
                        logger.error("THREAD ERROR[" + Thread.currentThread().getName() + "]", e);
                    }
                }
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
