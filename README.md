> <font color=red>示例代码github：[https://github.com/cm4j/cm4j-all](https://github.com/cm4j/cm4j-all) <br />
注意：本项目的agent热更方式需依赖于 cm4j-javaagent 的jar包<br />
因此请先下载 [cm4j-javaagent](https://github.com/cm4j/cm4j-javaagent) 项目并执行 mvn clean install 命令，把cm4j-javaagent打个jar包安装到本地maven仓库中</font>

从事游戏行业多年，一直使用Java做开发，不可避免的就经历了许多，其中也踩过不少坑。

- 最早的游戏是不支持热更的，导致出了BUG就必须停服；
- 后续项目引入热更，但也不是特别完美，再往后热更升级，引入第二版：动态加载子类热更；
- 受第二版热更方式启发，后来又加入了线上动态代码执行，主要用于规整数据，处理线上BUG；
- Arthas的横空出世，给线上解决方案打开了一个新思路。它的设计也非常巧妙，许多开源框架在它的基础上进行扩展，比如 [Bistoury](https://github.com/qunarcorp/bistoury) 的在线Debug等；
- 因为游戏业务特殊性，会产生许多跨进程接口调用，因此我们就期望一种本地和远程代码是同一种写法的API调用方式，于是就衍化出跨服本服调用的一致化。

由此可以看出，我们的技术演进与迭代也是逐步过来的，这中间也参考了许多开源的实现，其中 [Arthas](https://github.com/alibaba/arthas) 的思路不可或缺，这里要感谢Arthas技术团队。

这中间过程走了不少弯路，因此我这里整理了一个系列文章，主要讲解下这么多年遇到的问题、使用到的线上解决方案，以及其背后的原理。
主要涉及到的技术点包括：Agent、JavaCompiler代码编译、字节码生成、ClassLoader原理、框架的代码隔离与互调思路等等

系列介绍目录：[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

- [JAVA热更新1：Agent方式热更](//yeas.fun/archives/hotswap-agent)
- [JAVA热更新2：动态加载子类热更](//yeas.fun/archives/java-hotswap-compile)
- [JAVA线上执行代码（动态代码执行）](//yeas.fun/archives/java-eval)
- 在线调试Debug
- [像本服一样调用远程代码（跨进程远程方法直调）](//yeas.fun/archives/remoting-invoke)
- [像本服一样调用远程代码（优化版）](//yeas.fun/archives/remoting-invoke2)
- [Arthas原理：理解ClassLoader](//yeas.fun/archives/arthas-classloader)
- [Arthas原理：arthas如何做到与应用代码隔离？](//yeas.fun/archives/arthas-isolation)
- [Arthas原理：Arthas的命令分类及原理](//yeas.fun/archives/arthas-command-category)

多线程系列目录：
- [替换synchronized锁解决死锁](https://yeas.fun/archives/deadlock-solution)
- [单线程执行解决复杂的并发场景](https://yeas.fun/archives/singleton-module)