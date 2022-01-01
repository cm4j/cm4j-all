# JAVA不停服执行代码（动态代码执行）

## 系列介绍目录：

[Java线上解决方案系列目录](//yeas.fun/archives/solution-contents)

## 背景

尽管我们有了[JAVA热更新1：Agent方式热更](//yeas.fun/archives/hotswap-agent)、[JAVA热更新2：动态加载子类热更](//yeas.fun/archives/java-hotswap-compile)，能修复大部分线上的BUG，在项目上线之后，不可避免的会遇到出数据错乱的情况。之前的做法可能是提前写好一段代码，然后通过后台接口来进行调用，用以解决线上数据规整。但这种方式必须得提前写好规整逻辑，但不能覆盖所有情况。
因此我们就期望直接在线上执行一段代码，来进行我们业务数据的规整。

例如：我们直接获取用户1234的信息，然后把用户年龄改为15，然后把修改后的值返回出来。

```java
public class ChangeInfoTest {

    public static int changeUserInfo() {
        UserInfoVO info = UserInfoCache.getUserInfo(1234);
        info.setAge(15);
        return info.getAge();
    }
}
```

## 设计思路

如果要实现上述功能，本质上也就是我们期望写一段代码然后后在应用上执行。其实JDK的底层本身就提供了动态加载类文件的能力，它就是JavaCompiler。

如果使用JavaCompiler动态加载类文件内容，那就需要经过下述流程：

- 把Java代码组装成一个格式正确的java源码，编译为class字节流
- 利用ClassLoader将class字节流加载进入JVM，得到对应的class
- 基于class则可以反射调用对应的逻辑

### JavaCompiler的标准工作流程

如果代码片段格式正确，我们就通过Java编译器动态编译源代码得到了class。

```java
// 以下仅为示例代码，具体实际可运行代码可参考文末的示例代码
public class JavaCompilerUsage {

    public void compileTest() throws ClassNotFoundException {
        // 这里设置类名和源码，content必须是符合语法规范的类文件内容
        String className = "", content = "";

        // cl：是作为DynamicClassLoader的parent，一般是用当前应用的classloader
        // 主要作用是通过它来实现线上的代码对代码片段的可见性（双亲委派）
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        // 动态ClassLoader，主要用它来加载编译好的class文件
        DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(cl);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        // 文件管理器
        StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(null, null, null);
        JavaFileManager fileManager = new DynamicJavaFileManager(standardFileManager, dynamicClassLoader);
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

        // 添加类名和对应的源码
        List<JavaFileObject> compilationUnits = new ArrayList<>(new StringSource(className, content));

        // 构建编译任务
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, collector, new ArrayList<>(), null,
                compilationUnits);

        // 执行编译过程
        boolean result = task.call();
        if (result) {
            // 通过dynamicClassLoader获取编译之后的类文件
            Map<String, Class<?>> classes = dynamicClassLoader.getClasses();
        }
    }
}
```

### 线上如何执行代码？

得到class之后，我们想要调用class的方法，最直接的就是反射调用，相对就比较简单了，下面就是一段示例代码，直接调用类中第一个 public static 方法

```java
// 以下仅为示例代码，具体实际可运行代码可参考文末的示例代码
public class ClassCaller {

    private static Object call(Class<?> methtClass) throws Exception {
        Method[] declaredMethods = methtClass.getDeclaredMethods();

        for (Method declaredMethod : declaredMethods) {
            // 调用类中第一个public static的方法
            if (Modifier.isPublic(declaredMethod.getModifiers()) && Modifier.isStatic(declaredMethod.getModifiers())) {
                Object result = declaredMethod.invoke(null);

                LOG.error("JavaEval return: {}", JSON.toJSON(result));
                return result;
            }
        }

        throw new RuntimeException("NO method is [public static], cannot eval");
    }
}
```

## 问题：为什么我们写的类能调用到的目标jvm的代码?

上面我们看到 new DynamicClassLoader(cl)的时候传递了一个参数：cl，这是DynamicClassLoader的parent，也就是它的父ClassLoader。
基于ClassLoader的双亲委派的原则，子ClassLoader是可以访问父ClassLoader里面的类的，所以我们写的代码是可以直接访问到线上的代码逻辑，而不会报类不存在。

关于ClassLoader的实现细节，我们在讲Arthas的原理时会详细再讲解。

## 示例代码github：

[https://github.com/cm4j/cm4j-all](https://github.com/cm4j/cm4j-all)

### 运行测试

JavaEvalUtilTest.evalTest1()：直接运行java源码，运行即可计算1+2得到结果3

JavaEvalUtilTest.evalTest2()：读取本地的一个类文件，并执行运行第一个public static 方法，结果与上一个方法同样

## 总结

我们想要线上动态执行代码来进行业务调整，需要经过以下步骤：

- 实现端代码片段，里面包含自己的业务逻辑，组装成一个格式正确的java源码
- 使用JavaCompiler，编译上述的字符串，并利用ClassLoader加载出对应的class
- 利用反射动态调用class里面的逻辑

## 最后

当前我们有多种方式对本服线上问题进行处理，但涉及到跨服调用的时候API总是很丑，不够方便，最好的是我们能 [像本服一样调用远程代码（跨进程远程方法直调）](//yeas.fun/archives/remoting-invoke)，这就是下一篇的文章内容。

### ---END---
