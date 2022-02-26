# Arthas原理：Arthas的命令分类及原理

## 系列介绍目录：

[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

## 背景
在[Arthas原理：理解ClassLoader](//yeas.fun/archives/arthas-classloader)一文中，我们首先介绍了代码隔离的概念，并讲解了代码隔离的基础：ClassLoader；

在[Arthas原理：如何做到与应用代码隔离？](//yeas.fun/archives/arthas-isolation)一文中，我们介绍了利用ClassLoader实现代码隔离的原理，并讲解了Arthas与应用之间的代码在代码隔离的基础上如何进行代码互调的。

## 代码隔离的一个例子：jvm-sandbox

[jvm-sandbox](https://github.com/alibaba/jvm-sandbox) 是alibaba的另一个框架，他是代码增强的基础框架。这里是两者的区别：[https://www.cnblogs.com/ttzzyy/p/11414051.html](https://www.cnblogs.com/ttzzyy/p/11414051.html) 。

两者的核心作用是一样的，只是jvm-sandbox是工具箱，而arthas更像是一个产成品。所以两者的核心原理也都是一样的，都是代码隔离和代码增强。

![Arthas原理：Arthas的命令分类及原理](https://oss.yeas.fun/halo-yeas/arthas-command-category1.png)

上图是jvm-sandbox官网给出的一个原理图，几个ClassLoader的关系如下：
- BusinessClassLoader就是应用的ClassLoader；
- SandboxClassLoader是jvm-sandbox的自定义ClassLoader，它加载了sandbox-core.jar与sandbox-api.jar；
- Module其实就是各自实现的模块，标准的插件机制，我们可以不停服动态的增加和减少Module。IDEA和eclipse的插件机制原理和这个基本类似，对插件感兴趣的同学可以自行研究源码；
- ModuleJar的父ClassLoader是SandboxClassLoader，也就是Module可以直接调用sandbox-core.jar与sandbox-api.jar的逻辑代码。

代码隔离主要体现在（insulate就是隔离的意思）：
- SandboxClassLoader与BusinessClassLoader是并行的，也就是sandbox的代码正常情况下与业务代码是互不相通的；
- Module1与Module2的ClassLoader也是并行的，即各个模块之间也是互不相通的；
- Module与应用如何作用的？上一篇已经讲过，通过中间桥梁：SpyAPI。

## 命令分类

至此，arthas通过agent连接到应用，可以实现如下命令：

### 原理1：Instrumentation：
因arthas是通过agent连接目标应用的，所以可以获取到Instrumentation对象，而Instrument对象提供如下核心API：

**1. getAllLoadedClasses()**<br>
作用：查找所有的加载的类<br>
相关命令：sc、sm、classloader，以及用到类过滤，方法过滤

**2. redefineClasses()**<br>
作用：基于class文件重载实现，一般用于热更<br>
相关命令：redefine

**3. retransformClasses()**<br>
作用1：可以获取甚至是修改类的二进制字节 <br>
相关命令：dump、jad<br>
作用2：代码增强 <br>
一般都是EnhancerCommand的子类。其主要是对目标应用进行增强，在应用方法进入、退出、异常的调用插入SpyAPI的调用，而SpyAPI会回调arthas的逻辑，从而实现业务执行时触发arthas监控。<br>
相关命令：AOP动态增强类：watch、stack、monitor、tt、trace等<br>
**TIPS：** 代码增强后运行逻辑并不能直观看到，这时可以打开arthas的参数：options dump true，这样在增强时会把增强后的class dump到本地，方便查看增强后的实际代码。

### 原理2：调用目标应用的类：
调用目标应用ExtClassLoader加载的类，而jmx的类就包含其中，从而可以直接获取到系统的一些运行情况。<br>
相关命令：<br>
dashboard：查看当前系统的运行情况<br>
jvm：获取jvm相关信息<br>
mbean：获取jmx相关信息<br>
vmoption：通过jmx修改虚拟机参数<br>
sysenv：通过jmx修改虚拟机参数<br>
heapdump：直接修改系统的环境变量<br>
thread：获取到线程相关信息<br>
perfcounter：查看当前JVM的Perf Counter信息，jvm进程运行时，会记录一些实时的监控数据到perCounter文件中，可通过API获取这些数据

### 原理3：JDK内存编译:
作用：把java文件编译为class<br>
相关命令：mc<br>
相关源码：MemoryCompilerCommand

### 其他
还有一些命令是基于其他一些原理执行的<br>
比如：<br>
利用反射执行的：getstatic<br>
利用OGNL框架执行的：ognl<br>
火焰图：profiler，它使用async-profiler对应用采样，生成火焰图，从而可以监控应用的。可以参考这篇文章：[如何读懂火焰图](https://yeas.fun/archives/flame-graph)

## 总结
至此，我们就完成了arthas的核心原理的分析。本篇主要是针对底层设计思路分析，较少涉及具体的源码，建议有时间的同学多查看和断点源码以及参考其他网站进行深入分析。

--- END ---