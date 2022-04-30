# 像本服一样调用远程代码（跨进程远程方法直调）

## 系列介绍目录：

[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

## 背景

目前市面上游戏已经趋于存量竞争，玩家要求也越来越高，已经不再满足一个服内的生态，于是就越来越趋向于跨服竞争。这就对技术方面提出一个需求：像本服一样调用远程代码？也就是跨进程远程方法直调？

## 效果展示

还是老规矩，先上结果：

```java
class Test {
    public void test() {
        // 第一个参数固定是服务器id，用于标识发往哪个服务器进行逻辑处理
        String result = TestRpc.getInstance().handle(serverId, userId, data);
    }
}
```

## 核心思路

想要调用远程方法，传统的方式需要以下几个步骤：

- 需要先判断是否是本服请求。如果是本服，则执行本服逻辑；如果是跨服，则需要把请求发到对应服务器上；
- 跨服务之间的消息通信；
- 消息发送到跨服之后，如何根据参数来调用对应的代码？

于是问题就简化为：

- 本服：假设要保持效果展示中的代码写法，如何来判断请求是本服调用还是发往跨服？
- 传输：可采用各种远程调用方式，这里采用开源框架grpc。
- 远程：消息收到后，怎么定位到具体代码并执行，还要兼顾性能？

### 本服

首先我们需要标识出哪些类的哪些方法是支持远程调用的，我们可以编写接口IRemotingClass来标识类，用注解@RemotingMethod来标识方法。

```java
public class TestRpc implements IRemotingClass {

    @RemotingMethod
    public String rpcTest(int sid, String data) {
        System.out.println("执行线程：" + Thread.currentThread());
        return "sid:" + sid + ",data:" + data;
    }

    // 获取TestRpc类的代理对象
    public static TestRpc getInstance() {
        return LocalProxyGenerator.getProxy(TestRpc.class);
    }
}
```

那如何标识请求是发往本服还是跨服？那我们约定：<font color=red>方法的第一个参数就是服务器ID</font>。

如何在真正业务逻辑执行前加是否为本服的判断？这是典型的代理的使用场景。
所以我们在服务启动时，进行代码扫描，采用cglib框架对IRemotingClass的子类生成代理类。具体实现代码：RemotingInvokerGenerator#init(String packageScann)

#### 生成代理类示例：

```java
class LocalProxyGenerator {

    static <T extends IRemotingClass> T generateProxy(Class<T> remotingClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(remotingClass);

        // 注意：这里不捕获异常，这样如果出现异常会直接上抛。
        // 外部可统一捕获进行逻辑处理
        enhancer.setCallback((MethodInterceptor) (target, method, params, methodProxy) -> {
            String methodName = method.getName();

            // 仅处理代理的方法，其他方法则走正常调用
            RemotingMethod annotation = method.getAnnotation(RemotingMethod.class);
            if (annotation == null) {
                return methodProxy.invokeSuper(target, params);
            }

            // 请求的第一个参数，固定为服务器ID。下面基于服务器ID来判断是否为本服请求
            int sid = Integer.parseInt(String.valueOf(params[0]));
            // 非本服：直接远程RPC调用
            if (sid > 0 && !GrpcServer.isSameServer(sid)) {
                return grpc(remotingClass, methodName, params);
            }

            // 本服，调用热更对象【非调用代理对象】
            return RemotingInvokerUtil.invoke(remotingClass, methodName, params);
        });

        return (T) enhancer.create();
    }
}
```

至此：基于服务器ID进行是否本服还是跨服逻辑则实现完成。

### 传输

方式有很多，可以自己实现协议传输。也可以使用开源框架，示例里面使用的是grpc。选择它原因如下：

- 成熟的开源产品，使用广泛
- 支持protobuf，游戏内协议也是protobuf，刚好无缝对接

### 远程

通过传输，请求类、方法、参数都传输到远程服务器上，那如何使用类和方法名字来调用方法？

- 方案1：反射调用，频繁业务不建议
- 方案2：参考开源的一些实现，在服务启动时，进行代码扫描，采用javaassist框架动态代码生成类和对象，基于判断来进行方法直调。
  相关代码生成类：RemotingInvokerGenerator，具体实现就不细讲了，大家有兴趣可以去看下源码。注意：为了支持子类热更，我们这里调用的是***Registry类，
  有不清楚原理的可以参照：[JAVA热更新2：动态加载子类热更](https://yeas.fun/archives/java-hotswap-compile)

以下是生成的class（服务启动后会dump在项目的invoker-output目录下）:

```java
public class TestRpcInvoker implements IRemotingInvoker {
    public Object invokeInternal(String var1, Object[] var2) throws Exception {
        if ("rpcTest".equals(var1)) {
            int var3 = (Integer) var2[0];
            String var4 = (String) var2[1];
            TestRpc var5 = (TestRpc) InvokerRegistry.getInstance().get("com.cm4j.invoke.impl.TestRpc");
            String var6 = var5.rpcTest(var3, var4);
            return var6;
        } else {
            throw new RuntimeException("RemotingInvoker 方法没查询到:" + var1);
        }
    }

    public TestRpcInvoker() {
    }
}
```

## 关于性能

至此，三大块内容都已实现，那性能方面如何？

- 根据原理方面，所有动态类和代理类都是在启动服务器时生成的，且cglib也是生成class方式直接调用的，所以理论上没有反射方面的性能消耗，
- 性能消耗点还是GRPC传输
- 结论：此方式的性能和GRPC的调用方式一致

## 示例代码github：

[https://github.com/cm4j/cm4j-all](https://github.com/cm4j/cm4j-all)

### 单元测试代码

TestRpcTest：单元测试里启动了2服的服务端，设置当前应用是1服，则可看到运行结果是跨服走grpc调用到2服逻辑，且执行线程与启动线程不一样，代表是走到grpc了

## 总结

想要像本服一样调用远程代码（跨进程远程方法直调），得有3个流程：

- 本服：构建代理类
- 传输：grpc
- 远程：通过javaassist动态生成类来调用需要执行的逻辑

## 后续优化

此文中部分实现有点原始，后续已对其中2点进行优化，<font color=red>示例代码也被替换为优化后的方案</font>，具体优化请查看：[像本服一样调用远程代码（优化版）](//yeas.fun/archives/remoting-invoke2)

## --- END ---