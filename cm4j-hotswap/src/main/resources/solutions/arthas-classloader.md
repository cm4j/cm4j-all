# Arthas原理：理解ClassLoader

## 系列介绍目录：

[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

或者可以直接下一篇：[Arthas原理：如何做到与应用代码隔离？](https://yeas.fun/archives/arthas-isolation)

## 背景

阿里的arthas一经推出就大受好评，主要原因就是它提供了一套线上问题的解决方案，比如可以在线查看服务器状态；可以支持热更新，原理类似我们之前所讲的[JAVA热更新1：Agent方式热更](//yeas.fun/archives/hotswap-agent)
； 它还可以支持对线上的代码跟踪执行情况，打印执行参数和返回参数等功能。功能那是异常强大，关键的一点是它对应用是无侵入的，也就是不影响到目标应用的业务逻辑。

为了实现上述的一些功能，arthas需要解决以下几个问题：

- 应用（目标进程）如何通过ClassLoader实现Arthas的代码加载？
- 应用与Arthas如何隔离？也就是Arthas是如何做到无侵入的；
- 既然是互相隔离的，那应用与arthas又是如何进行代码相互调用的？

而这一切的一切，都要从JDK提供的ClassLoader机制说起。

## ClassLoader作用

ClassLoader主要作用就是通过一个类的全限定名来获取描述该类的二进制字节，他的来源不局限于从class类文件读取，可以从任意二进制字节来读取，甚至从http下载的二进制字节都可以。

### 为什么要进行数据隔离？

其实最早之前的ClassLoader是没有数据隔离的，这就会导致一种情况：假设JDK里面提供了String类，而我们也写一个同名同姓的String类，JVM加载类的时候就可能先加载到我们自己写的String类。
那我们就可以修改String类的逻辑，JDK的代码就不安全了。
于是就需要一种机制来保证JDK的代码优先加载，且业务层代码无法对JDK代码进行篡改。

### jvm对ClassLoader的保证

为了解决上述问题，JDK从1.2开始引入双亲委派模式。这是基于以下几个保证的：

- JVM内部，ClassLoader类似于类的命名空间
- 比较两个类是否相等，必须是在同一个类加载器上才有意义。例如以下几个方法判断：equals()，isAssignableFrom()，instanceof

结论：ClassLoader不同 > 类不同 > 对象不同。可以通过ClassLoader进行代码隔离。

## 怎么实现隔离呢？

答案就是：双亲委派机制。不知道为啥这么命名，但我们看下ClassLoader的源码大概也就知道类加载原理了。

```java
public class ClassLoader{
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 首先：检测类是否已经被加载过，加载过，直接返回
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    // 存在parent的ClassLoader
                    if (parent != null) {
                        c = parent.loadClass(name, false); // 调用parent的ClassLoader继续加载类
                    } else {
                        // 不存在parent，说明就是BootstrapClassLoader。JDK里BootstrapClassLoader是由jvm底层实现的，没有实际的类
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // 类没找到，说明是parent没加载到对应的类，这里不需要进行异常处理，继续后续逻辑
                }

                // 如果一层层往上都没加载到类，则本ClassLoader尝试findClass()查找类
                if (c == null) {
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
```

从源码可以看出：

- 查找class：从当前ClassLoader开始查找，如果当前ClassLoader已经加载了，则子类就直接用了，不会再次加载；如果没有加载，则往上一层查找，一直查找到顶层；
- 加载class：从顶层开始往下判断，看ClassLoader的搜索范围内是否包含这个class的path，如果包含则加载；如果没有，则往下一层继续判断，一直到当前的ClassLoader；

**文字描述相对抽象，大家可以参照流程图：**

![Arthas原理：理解ClassLoader](https://oss.yeas.fun/halo-yeas/arthas-classloader1_1642839277016.png)

上图是ClassLoader的加载流程，还是有点复杂，我这里再简述下：

- ClassLoader更像是一个树状结构
- 查找类是从下往上的，也就是：子ClassLoader加载的类可以访问父ClassLoader的加载类，反之或平级则不行
- 而加载是从上往下的，比如说BootstrapClassLoader就是加载jre\lib的所有包，ExtClassLoader就是加载ext目录下所有jar包
- 不同的ClassLoader的加载类是互相隔离的

基于上述的机制，我们也就无法篡改JDK的源码，因为JDK的很多代码都是在最上层的ClassLoader去加载的，而我们应用更多的是下层ClassLoader加载的，JDK会优先加载自己的类，即使我们写了同名的String类也不会加载到。
同时这种机制也提供了一种类隔离的机制，接下来我们举个例子来说明。

## Tomcat的实现类的隔离与共享

Tomcat是一个web容器，它可以同时启动多个web服务，那不能出现一个web的应用代码能够修改另一个web应用的逻辑，也就是多个web的代码就是需要隔离的。
但是都是web应用，里面有些jar包和逻辑是一样的，比如说tomcat-home/lib下的jar包，也不能每个web应用都去加载所有jar包，这样内存就太浪费了。

因此就产生这样的需求：web之间要数据隔离，web公用的jar包却要共享，那应该如何实现呢？

![Arthas原理：理解ClassLoader](https://oss.yeas.fun/halo-yeas/arthas-classloader2_1642839277016.png)

上图就是Tomcat的ClassLoader的树状图，Tomcat下可新增common、server、shared三组目录（默认不开放，需要指定配置），用于存放jar包。 下面列出不同ClassLoader对应加载的jar包内容：

| ClassLoader         | 加载目录或文件         | 说明                    |
|:--------------------|-----------------|-----------------------|
| CommonClassLoader   | /common         | 所有应用共享（包括tomcat）      |
| CatalinaClassLoader | /server         | tomcat的实现是独立隔离的       |
| SharedClassLoader   | /shared         | 所有web应用共享，但对tomcat不可见 |
| WebappClassLoader   | /WebApp/WEB-INF | 不同web应用相互隔离           |
| JasperLoader        | jsp             | 支持热更HotSwap           |

从上面我们还可以看出不同的ClassLoader加载不同的目录，且父ClassLoader的类是被子ClassLoader共享的。如果需要隔离，那就下放不同的子ClassLoader去加载。

同时也解释了为什么jsp一保存就可以立即生效而不需要重启Tomcat，因为保存时Tomcat会使用JasperLoader重新加载新的jsp页面，从而实现jsp的实时热更新。
其原理类似于：[JAVA热更新2：动态加载子类热更](//yeas.fun/archives/java-hotswap-compile)

## 总结

上述主要讲了JDK的ClassLoader的双亲委派的类加载机制，核心就是如何实现代码的隔离与共享。
这个和Arthas的框架加载和隔离机制高度相关，同样原理的开源框架有不少，例如：[bistoury](https://github.com/qunarcorp/bistoury)
、[jvm-sandbox](https://github.com/alibaba/jvm-sandbox) 等， 在下一篇我们正式进入Arthas实现原理篇：Arthas的加载和隔离机制，以及如何对Arthas框架内的源码进行调试。

### --- END ----