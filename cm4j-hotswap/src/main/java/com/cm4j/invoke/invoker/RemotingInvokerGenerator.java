package com.cm4j.invoke.invoker;

import com.cm4j.hotswap.recompile.RecompileClassLoader;
import com.cm4j.invoke.IRemotingClass;
import com.cm4j.invoke.RemotingMethod;
import com.cm4j.invoke.proxy.LocalProxyGenerator;
import com.cm4j.util.PackageUtil;
import com.cm4j.util.RemotingInvokerUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态生成类，构建对象并设置到${@link RemotingInvokerUtil}
 */
public class RemotingInvokerGenerator {

    private static final Logger log = LoggerFactory.getLogger(RemotingInvokerGenerator.class);

    /**
     * 代码扫描，动态构建内部switch方法
     */
    @SuppressWarnings("unchecked")
    public static void init(String packageScann) throws Exception {
        ClassPool pool = ClassPool.getDefault();

        // https://www.javassist.org/tutorial/tutorial.html
        // 页面搜索"Class search path"
        // 运行在web容器(如tomcat)中的程序,可能存在多个ClassLoader,导致ClassPool.getDefault()找不到对应的class
        pool.insertClassPath(new ClassClassPath(IRemotingInvoker.class));

        Multimap<String, Class<?>> tmpClassMethodParamTypes = ArrayListMultimap.create();
        Map<String, Class<?>> tmpClassMethodReturnType = Maps.newHashMap();

        CtClass remotingInvokerInterface = pool.getCtClass(IRemotingInvoker.class.getName());
        CtClass[] paramTypes = {pool.get(String.class.getName()), pool.get(Object[].class.getName())};

        String packageName = remotingInvokerInterface.getPackageName();

        // 重写方法名
        String invokeInternalName = "invokeInternal";
        CtClass javaLangObject = pool.get("java.lang.Object");
        CtClass[] exceptionCtClass = {pool.get("java.lang.Exception")};

        // invoker类后缀信息
        String invokerSuffix = "Invoker";

        // 扫描类
        final Set<Class<?>> clazzes = PackageUtil.findPackageClass(packageScann);
        for (Class<?> clazz : clazzes) {
            if (IRemotingClass.class.isAssignableFrom(clazz)) {
                // 排除接口和抽象类
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                // 属于IRemotingClass，生成并注入注册代理类
                LocalProxyGenerator.proxy((Class<IRemotingClass>) clazz);

                // 创建invoker类
                String invokerSimpleName = clazz.getSimpleName() + invokerSuffix;
                String invokerClassName = packageName + "." + invokerSimpleName;
                CtClass ctClass = pool.makeClass(invokerClassName);
                ctClass.addInterface(remotingInvokerInterface);

                CtMethod method = new CtMethod(javaLangObject, invokeInternalName, paramTypes, ctClass);
                method.setExceptionTypes(exceptionCtClass);
                method.setBody(genMethodBody(clazz, clazz.getName(), tmpClassMethodParamTypes, tmpClassMethodReturnType));
                ctClass.addMethod(method);

                // 输出到文件中，查看结果
                byte[] dumped = ctClass.toBytecode();
                dump(dumped, invokerSimpleName);

                // 从classpool移除
                ctClass.detach();

                RecompileClassLoader recompileClassLoader = new RecompileClassLoader(clazz.getClassLoader(), invokerClassName, dumped);
                Class<?> newInvokerClass = recompileClassLoader.findClass(invokerClassName);

                // 构建新对象
                IRemotingInvoker newInvoker = (IRemotingInvoker) newInvokerClass.newInstance();
                RemotingInvokerUtil.addInvoker(clazz.getName(), newInvoker);
            }
        }

        classMethodParamTypes = tmpClassMethodParamTypes;
        classMethodReturnType = tmpClassMethodReturnType;

    }

    private static final String NO_RETURN = "if (\"$METHODNAME\".equals($1)) {\n" + "\t$PARAM_TRANSFER\n" + "\t$CLASSNAME target = ($CLASSNAME)com.cm4j.registry.registry.InvokerRegistry.getInstance().get(\"$CLASSNAME\");\n" + "\ttarget.$METHODNAME($PARAMS);\n" + "\treturn null;\n" + "}";

    private static final String HAS_RETURN = "if (\"$METHODNAME\".equals($1)) {\n" + "$PARAM_TRANSFER\n" + "\t$CLASSNAME target = ($CLASSNAME)com.cm4j.registry.registry.InvokerRegistry.getInstance().get(\"$CLASSNAME\");\n" + "\t$RETURNTYPE result = target.$METHODNAME($PARAMS);\n" + "\t$RETURN\n" + "}";

    private static Multimap<String, Class<?>> classMethodParamTypes = ArrayListMultimap.create();

    private static Map<String, Class<?>> classMethodReturnType = Maps.newHashMap();

    /**
     * 生成方法的消息体
     *
     * @param clazz
     * @param className
     * @param tmpClassMethodParamTypes
     * @param tmpClassMethodReturnType
     * @return
     */
    private static String genMethodBody(Class<?> clazz, String className, Multimap<String, Class<?>> tmpClassMethodParamTypes, Map<String, Class<?>> tmpClassMethodReturnType) {
        StringBuilder sb = new StringBuilder();

        // body 整体是要用 {} 括起来的
        sb.append("{\n");

        boolean genMethod = false;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            RemotingMethod annotation = method.getAnnotation(RemotingMethod.class);
            if (annotation == null) {
                continue;
            }

            // 1.校验
            boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic) {
                throw new RuntimeException("方法修饰符非public, class:" + clazz + ",method:" + method);
            }

            // 2.校验
            String methodName = method.getName();
            String uniqueTag = className + "#" + methodName;
            if (tmpClassMethodParamTypes.containsKey(uniqueTag)) {
                throw new RuntimeException("存在重复的同名函数, class:" + clazz + ",method:" + method);
            }

            // 3.拼接重点段代码
            List<String> paramsTransferList = Lists.newArrayList();
            List<String> paramsList = Lists.newArrayList();

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || !RemotingInvokerUtil.checkFirstParamType(parameterTypes[0])) {
                // 第一个参数不是id参数（int类型）
                throw new RuntimeException("第一个参数不是id参数类型不支持, class:" + clazz + ",method:" + method);
            } else {
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!RemotingInvokerUtil.checkClassTypeSupport(parameterTypes[i])) {
                        throw new RuntimeException("不支持的参数类型, class:" + clazz + ",method:" + method + ", param:" + parameterTypes[i].getSimpleName());
                    }
                    paramsTransferList.add(wrapParams(parameterTypes[i], i));
                    paramsList.add("p" + i);
                    tmpClassMethodParamTypes.put(uniqueTag, parameterTypes[i]);
                }
            }

            if (genMethod) {
                sb.append(" else ");
            }

            boolean hasReturn = void.class != method.getReturnType();

            if (!RemotingInvokerUtil.checkClassTypeSupport(method.getReturnType())) {
                throw new RuntimeException("不支持的返回值类型, class:" + clazz + ",method:" + method + ", param:" + method.getReturnType().getSimpleName());
            }

            String paramString = StringUtils.join(paramsList, ",");
            String paramTransferString = StringUtils.join(paramsTransferList, "\n");
            tmpClassMethodReturnType.put(uniqueTag, method.getReturnType());
            String tmp = StringUtils.replace(hasReturn ? RemotingInvokerGenerator.HAS_RETURN : RemotingInvokerGenerator.NO_RETURN, "$CLASSNAME", className);
            tmp = StringUtils.replace(tmp, "$METHODNAME", methodName);
            tmp = StringUtils.replace(tmp, "$PARAM_TRANSFER", paramTransferString);
            tmp = StringUtils.replace(tmp, "$PARAMS", paramString);
            if (hasReturn) {
                tmp = StringUtils.replace(tmp, "$RETURNTYPE", method.getReturnType().getName());
                tmp = StringUtils.replace(tmp, "$RETURN", wrapReturn(method.getReturnType()));
            }
            sb.append(tmp);
            genMethod = true;
        }

        sb.append("else {\n");
        sb.append("    throw new RuntimeException(\"RemotingInvoker 方法没查询到:\" + $1);\n");
        sb.append("}\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * 基于方法参数类型，转为对应的代码
     *
     * @param paramType 调用方法需要的参数类型
     * @param idx       第几个参数
     * @return
     */
    private static String wrapParams(Class<?> paramType, int idx) {
        // https://blog.csdn.net/zhaohongfei_358/article/details/101267843
        // Java中的拆箱和装箱是个语法糖。是没有字节码的。所以Javassist的编译器不支持它们。需要手动拆装箱！！！

        // 这里外部调用是 Object[]， 也是就默认是 Integer，所以如果方法是 int，不转就会包类型不匹配。
        // 坑爹的设计，捣鼓了一天！！！
        if (paramType == byte.class) {
            return "\tbyte p" + idx + " = ((Byte)$2[" + idx + "]).byteValue();";
        } else if (paramType == short.class) {
            return "\tshort p" + idx + " = ((Short)$2[" + idx + "]).shortValue();";
        } else if (paramType == int.class) {
            return "\tint p" + idx + " = ((Integer)$2[" + idx + "]).intValue();";
        } else if (paramType == long.class) {
            return "\tlong p" + idx + " = ((Long)$2[" + idx + "]).longValue();";
        } else if (paramType == boolean.class) {
            return "\tboolean p" + idx + " = ((Boolean)$2[" + idx + "]).booleanValue();";
        } else if (paramType == float.class) {
            return "\tfloat p" + idx + " = ((Float)$2[" + idx + "]).floatValue();";
        } else if (paramType == double.class) {
            return "\tdouble p" + idx + " = ((Double)$2[" + idx + "]).doubleValue();";
        } else {
            // 其他统一强转：
            return paramType.getName() + " p" + idx + " = (" + paramType.getName() + ")$2[" + idx + "];";
        }
    }

    private static String wrapReturn(Class<?> returnType) {
        // 问题同：wrapParams()

        // 请求参数如果是int，但方法内标的是 Integer，则会报错
        // 同样的，请求参数如果是 int, 返回参数如果设置的是Object，也会报错。因为int不会自动装箱为Integer，也就不会转为Object
        if (returnType == byte.class) {
            return "return new Byte(result);";
        } else if (returnType == short.class) {
            return "return new Short(result);";
        } else if (returnType == int.class) {
            return "return new Integer(result);";
        } else if (returnType == long.class) {
            return "return new Long(result);";
        } else if (returnType == boolean.class) {
            return "return new Boolean(result);";
        } else if (returnType == float.class) {
            return "return new Float(result);";
        } else if (returnType == double.class) {
            return "return new Double(result);";
        } else {
            // 其他：直接返回
            return "return result;";
        }
    }

    private static void dump(byte[] targetBytes, String className) throws IOException {
        // class dump到日志中
        String basePath = new File("").getAbsolutePath();
        String finalPath = StringUtils.join(new String[]{basePath, "invoker-output", className + ".class"}, File.separator);
        File to = new File(finalPath);
        com.google.common.io.Files.createParentDirs(to);
        log.error("class dumpd: {}", to.getAbsolutePath());
        com.google.common.io.Files.write(targetBytes, to);
    }

    public static Multimap<String, Class<?>> getClassMethodParamTypes() {
        return classMethodParamTypes;
    }

    public static Map<String, Class<?>> getClassMethodReturnType() {
        return classMethodReturnType;
    }
}
