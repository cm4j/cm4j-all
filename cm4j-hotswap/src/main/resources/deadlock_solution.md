# 多线程并发解决方案：替换synchronized锁解决死锁

## 背景

在游戏开发过程中，多线程技术是非常重要的技术，多线程的引入最大的好处就是能解决游戏中的性能问题。
在加锁的逻辑上java提供的synchronized锁是非常简单而实用的，但随着业务逐渐增多且复杂，即使是简单的synchronized锁使用不合理也会引发死锁导致巨大的灾难。

在经历过线上的几次事故，最终引入了synchronized锁替换的解决方案，从根本上解决死锁问题。

## 最终效果

话不多说，先上优化后的代码:

```java
class Test {

    public void test() {
        Object obj = new Object();

        // 优化后的写法
        try (InternalLock ignored = Locker.getLockObjected(obj)) {
            System.out.println("olai olai ooo...");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));
        }

        // synchronized写法
        synchronized (obj) {
            System.out.println("olai olai ooo...");
        }
    }
}
```

从上面对比可以看出，优化后的代码基本和synchronized写法保持一致。功能上从根本上解决了死锁问题，而且一旦发生死锁，还可以打印死锁线程和当前线程的堆栈，可以辅助快速排查问题。

## 为什么业务中倾向于使用synchronized锁

```java
class LockTest {

    public void test() {
        synchronized (this) {
            System.out.println("这里是业务逻辑");
        }
    }
}
```

上面这段代码，对于java开发者应该都很熟悉，最主要的原因就是：java提供的synchronized锁非常简单且实用。

- 使用简单：不用特意去加锁解锁，只要把逻辑放到锁块下面即可
- 只要注意加锁对象和加锁顺序
- 死锁一定是嵌套锁的顺序出问题了
- JDK会对synchronized的性能优化

因此：在没有特殊需求的情况下，一般推荐使用synchronized锁。

弊端： 一旦出现死锁，则锁无法释放，也就是说必须要重启服务器。这对于游戏应用来说是致命的，死锁可能会导致玩家大批量掉线，运营事故也会导致玩家流失。

## 线上的宕机事故

synchronized如果只锁单个对象，是不会出现死锁的，而一旦出现死锁，那基本上就是锁嵌套且锁的执行顺序不一致导致的。 例如下面的例子就会发生死锁，运行之后，t1和t2的线程状态就会进入BLOCKED状态。

```java
class Test {

    @Test
    public void deadLock_Test() {
        TTT o1 = new TTT();
        TTT o2 = new TTT();

        // 下面代码运行时会出现嵌套锁
        Thread t1 = new Thread(() -> {
            // 出现锁的情况：嵌套锁+锁的执行顺序不一样
            synchronized (o1) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                synchronized (o2) {
                    System.out.println(Thread.currentThread() + "111111>>> olai olai ooo...");

                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (o2) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                synchronized (o1) {
                    System.out.println(Thread.currentThread() + "111111>>> olai olai ooo...");
                }
            }
        });
        t1.start();
        t2.start();

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        System.out.println("t1状态：" + t1.getState()); // 这里会打印 BLOCKED
        System.out.println("t2状态：" + t2.getState());
    }
}
```

真实情况下，一般也不会这么写，线上出现死锁大部分都是无意识写出来的。因为游戏业务的复杂性，且各个模块之间有互相关联，为了解决并发问题可能会针对多个对象加锁。这就为死锁埋下了隐患。

一开始只是写了锁1，锁1调外部一个方法，随着业务越来越复杂，方法里面又调其他方法，N层之后，某一个方法因为业务需要对锁2进行加锁了。如果其他业务里面有先锁2再锁1，这种情况下也就形成的嵌套锁。
如果某些条件是有条件的加锁，那这种嵌套锁就更隐蔽，而且触发概率极低，即使测试环境下也不一定复现，一旦上线，就会酿成巨大灾难。

## synchronized锁替代方案

因为synchronized锁是由jvm提供的，无法中断，所以我们可以在lock上想办法。如果想避免死锁，我们可以使用tryLock的方式，如下面代码：

```java
class Test {

    public void test() {
        Lock lock = new ReentrantLock();
        boolean success = lock.tryLock(3, TimeUnit.SECONDS);
        if (success) {
            try {
                System.out.println("这里是业务逻辑");
            } finally {
                lock.unlock();
            }
        }
    }
}
```

如果业务逻辑都需要按照上面逻辑去写，那还是挺麻烦的，能不能有一种方法类似于synchronized的写法，又能达到同样的效果？答案就是jdk提供的try-with-resource机制。

### 利用try-with-resource机制来unlock

于是我们实现了一个自定义类InternalLock，实现AutoCloseable，在close中来进行unlock操作。
同时类里面我们加了一个holdThread对象，在加锁成功后把holdThread设置为当前线程，这样一旦发生死锁，就能定位到是哪里发生了死锁。

```java
public class InternalLock implements AutoCloseable {

    /**
     * 获取锁的超时时间，单位：s
     */
    private final int LOCK_TIME_OUT = 5;

    private final Lock lock = new ReentrantLock();
    // 当前持有锁的线程，死锁时，用来标识当前锁被哪个线程锁住了
    private Thread holdThread;

    public void tryLock() {
        boolean success = false;
        try {
            success = lock.tryLock(LOCK_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        if (success) {
            holdThread = Thread.currentThread();
        } else {
            // 堆栈信息
            String currentStack = ThreadUtil.getThreadStackTrace(Thread.currentThread());
            String holdStack = ThreadUtil.getThreadStackTrace(holdThread);

            log.error("=========== 获取InternalLock超时，可能发生死锁===========\n【当前线程】堆栈：{}\n{}\n【持有锁线程】堆栈：{}\n{}",
                    Thread.currentThread(), currentStack, holdThread, holdStack);
            throw new RuntimeException("获取InternalLock超时，可能发生死锁");
        }
    }

    @Override
    public void close() {
        holdThread = null;
        lock.unlock();
    }
}
```

### 如何基于对象找到锁？

JDK提供了一个方法：System.identityHashCode()，可以标识出对象的唯一hashCode，不管这个对象是否覆写hashCode()方法。基于此我们就可以得到对象在jvm中唯一的对象ID，具体代码如下：

```java
class Test {

    private static String identity(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
}
```

Caffeine缓存的对象判断，最终也是基于此进行判断的，因此我们可以新增一个Caffeine缓存，key就是对象，value就是我们上面的自定义锁对象InternalLock

```java
class Test {

    // 锁对象缓存，仅1min
    private static final LoadingCache<Object, InternalLock> lockCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(key -> new InternalLock());
}
```

## 串联所有流程

至此，我们所有细节点都实现了，剩下来只要把上述代码串联，即可实现文章开始的加锁效果。更多细节可参考下面的示例代码。

## 示例代码github：

[https://github.com/cm4j/cm4j-all](https://github.com/cm4j/cm4j-all)

### 单元测试

LockerTest：提供了几种多线程死锁的单元测试

## 总结

多线程并发情况下，既想要保留synchronized锁的简单，又期望解决死锁的问题，万一发生死锁还需要快速定位死锁线程，我们需要以下几步：

- 利用try-with-resource机制来实现锁的unlock
- 利用System.identityHashCode()可以唯一标识出对象
- 增加lock缓存，key为对象唯一标识，value为自定义的锁对象

### ---END---