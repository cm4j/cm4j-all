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
public class RemotingInvokerScanner {

    private static Multimap<String, Class<?>> classMethodParamTypes = ArrayListMultimap.create();

    private static Map<String, Class<?>> classMethodReturnType = Maps.newHashMap();

    /**
     * 代码扫描，动态构建内部switch方法
     */
    @SuppressWarnings("unchecked")
    public static void init(String packageScann) throws Exception {
        Multimap<String, Class<?>> tmpClassMethodParamTypes = ArrayListMultimap.create();
        Map<String, Class<?>> tmpClassMethodReturnType = Maps.newHashMap();

        // 扫描类
        final Set<Class<?>> clazzes = PackageUtil.findPackageClass(packageScann);
        for (Class<?> clazz : clazzes) {
            if (IRemotingClass.class.isAssignableFrom(clazz)) {
                // 排除接口和抽象类
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                // 扫描类的相关信息
                scanAndCheckMethod(clazz, clazz.getName(), tmpClassMethodParamTypes, tmpClassMethodReturnType);

                // 生成代理类
                LocalProxyGenerator.proxy((Class<IRemotingClass>) clazz);
            }
        }

        classMethodParamTypes = tmpClassMethodParamTypes;
        classMethodReturnType = tmpClassMethodReturnType;
    }


    /**
     * 生成方法的消息体
     *
     * @param clazz
     * @param className
     * @param tmpClassMethodParamTypes
     * @param tmpClassMethodReturnType
     * @return
     */
    private static void scanAndCheckMethod(Class<?> clazz, String className,
                                           Multimap<String, Class<?>> tmpClassMethodParamTypes, Map<String, Class<?>> tmpClassMethodReturnType) {

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

            // 3.扫描信息
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || !RemotingInvokerUtil.checkFirstParamType(parameterTypes[0])) {
                // 第一个参数不是id参数（int类型）
                throw new RuntimeException("第一个参数不是id参数类型不支持, class:" + clazz + ",method:" + method);
            } else {
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!RemotingInvokerUtil.checkClassTypeSupport(parameterTypes[i])) {
                        throw new RuntimeException("不支持的参数类型, class:" + clazz + ",method:" + method + ", param:"
                                + parameterTypes[i].getSimpleName());
                    }

                    tmpClassMethodParamTypes.put(uniqueTag, parameterTypes[i]);
                }
            }

            if (!RemotingInvokerUtil.checkClassTypeSupport(method.getReturnType())) {
                throw new RuntimeException(
                        "不支持的返回值类型, class:" + clazz + ",method:" + method + ", param:" + method.getReturnType()
                                .getSimpleName());
            }

            tmpClassMethodReturnType.put(uniqueTag, method.getReturnType());
        }
    }

    public static Multimap<String, Class<?>> getClassMethodParamTypes() {
        return classMethodParamTypes;
    }

    public static Map<String, Class<?>> getClassMethodReturnType() {
        return classMethodReturnType;
    }
}