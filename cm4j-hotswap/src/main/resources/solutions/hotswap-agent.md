# JAVA热更新1：Agent方式热更

## 系列介绍目录：

[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

## 正文

线上问题的解决一直是java程序员头疼的一个问题。有的应用很敏感，例如游戏行业，可能需要做到1个月都不能停服，那线上问题出了问题怎么办呢？谁也不能保证更新线上的逻辑百分百不出问题，这就需要我们在不停应用的情况下在线解决。随着技术的逐渐成熟，java社区也逐渐提供了一些线上解决方案，比如说下面3个方面：

- Java热更新（热部署）：不停应用的情况下，动态热更java的类，以替换线上运行逻辑；
- Java代码片段执行：就是编写一段代码，然后可以线上执行。可用于恢复某些异常数据，或者执行某些规整等。当然如果代码做一些调整，也可以做到代码的替换执行，略等于代码热更新；
- Java在线Debug：在线上打断点，当逻辑执行到断点之后，打印当前的线程、调用堆栈、当前类的成员变量、当前行的局部变量等信息，一切就和在本地debug一样。关键是不会影响线上的运行逻辑。

本篇主要介绍方案1：Java热更新（热部署） 顺便提一句：阿里的arthas框架的热更新就是用的这个方式

## Instrumentation功能

从JDK6开始，Java提供了一个新特性：Instrumentation功能，虽然这个接口包含的内容不多，但功能却很强大，这个接口主要提供了2个核心方法：

retransformClasses()：类似给method()穿了一层外衣，把内容content给覆盖了，这样每次执行都是执行的外衣的内容，既然是衣服，则可以脱掉removeTransformer()
，这样代码又恢复原始状态。一般用这个方法来进行动态方法注入。 redefineClasses()： 直接修改了方法内容content 当然这2个修改类的实现方式是有限制的，例如不允许修改方法签名，不允许增加方法参数等等。

有关于Instrumentation，网上介绍也比较多，有兴趣的朋友可以再深入研究下，许多知名的开源框架都是基于这个类进行动态的代码修改和注入的，比如阿里著名的arthas、jvm-sanbox，去哪儿旅行网的bistoury等等。这里由于篇幅，就不展开叙说了。

## 如何进行Java热更新呢

有了Instrumentation的接口，那如何调用它呢？简单点说，我们如何获取Instrumentation的实现？这里就不得不提到JDK的“代理”（agent）。JDK的agent简单点就是说在应用启动前或者应用运行时，JDK可以加载外部的一个agent包的代码，来动态修改或增强现有的代码逻辑。Instrumentation就是在agent过程中，由JVM提供的，通过
Instrumentation就可以修改代码。

### Agent的两种方式：

- jvm启动前：premain方式，必须在命令行指定代理jar，并且代理类必须在main方法前启动，它要求开发者在应用启动前就必须确认代理的处理逻辑和参数内容等等
- jvm启动后：agentmain方式，在应用程序的VM启动后再动态添加代理的方式 因为我们需要热更线上代码，所以需要采用agentmain的方式，这种方式需要提供一个agent jar，并且这个jar需要满足2个条件：

在manifest中指定Agent-Class属性，值为代理类全路径 代理类需要提供public static void agentmain(String args, Instrumentation inst)或public static
void agentmain(String args)方法。并且再二者同时存在时以前者优先。args和inst和premain中的一致。

### 如何加载Agent的jar包呢

通过JDK提供的tools.jar（存放在$JAVA_HOME/lib/tools.jar），里面有个VirtualMachine类，代码如下：

```
VirtualMachine vm = VirtualMachine.attach(当前进程ID);
vm.loadAgent("对应Agent的jar包路径");
```

注意：tools.jar在windows和linux环境下是不同的，所以如果程序跑在Linux下，需要添加Linux的tools.jar

如果Agent的jar包符合上面所说的2个条件，则虚拟机loadAgent的时候会调用到agentmain()方法

## 总结下代码Java热更新流程

- 首先项目需要添加当前进程对应的jdk的tools.jar包，因为 tools.jar提供了VirtualMachine类
- VirtualMachine类在加载agent的jar包时会触发agentmain方法，这个方法里面提供了Instrumentation
- 程序获取到Instrumentation之后，可以通过它进行代码的redefine或者retransform，从而实现对代码的热更新

## 依赖项目

<font color=red>注意：本项目依赖于cm4j-javaagent项目</font>，请先下载并安装jar包：[https://github.com/cm4j/cm4j-javaagent](https://github.com/cm4j/cm4j-javaagent)

## 示例代码github：

[https://github.com/cm4j/cm4j-all](https://github.com/cm4j/cm4j-all)

### 运行测试

运行JavaAgentTest.javaAgentTest()，结果如下：

```text

[main] ERROR com.cm4j.hotswap.agent.JavaAgent - java agent:jarPath:D:\repository\com\cm4j\cm4j-javaagent\1.0-SNAPSHOT\cm4j-javaagent-1.0-SNAPSHOT.jar
[main] ERROR com.cm4j.hotswap.agent.JavaAgent - current pid 17064
[main] ERROR com.cm4j.hotswap.agent.JavaAgent - java agent redefine classes started
0->sun.instrument.InstrumentationImpl@2bab9351
[main] ERROR com.cm4j.hotswap.agent.JavaAgent - class read from:/D:/Projects/others/cm4j-projects/cm4j-all/cm4j-hotswap/target/classes/
[main] ERROR com.cm4j.hotswap.agent.JavaAgent - class redefined:D:\Projects\others\cm4j-projects\cm4j-all\cm4j-hotswap\target\classes\com\cm4j\demo\util\DemoUtil.class
[main] ERROR com.cm4j.hotswap.agent.JavaAgent - java agent redefine classes end
```

## 线上使用

如果线上出了问题，则本地先修改好逻辑，把最新的class文件上传到服务器，然后执行上述agent热更，则程序会读取服务器上最新class，并替换jvm内部实现，从而实现不停服更改代码逻辑。

## 最后
JDK的热更新解决了一大问题，但也并不是唯一的热更新方式，因此这里介绍了另一种热更新方式：[JAVA热更新2：动态加载子类热更](//yeas.fun/archives/java-hotswap-compile)

尽管热更新能解决一部分问题，但已经发生的错误数据是无法通过热更新修复的，所以我们就期望直接在线上不停服执行代码。这就是 [JAVA不停服执行代码（动态代码执行）](//yeas.fun/archives/java-eval)

### -- END-- 