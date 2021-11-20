package com.cm4j.eval;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.cm4j.eval.compiler.DynamicCompiler;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Java动态代码执行<br />
 * <p>
 * 使用方法：<br />
 * 直接写一个类，里面必须要有一个 public static 方法，就可以调用该方法
 */
public class JavaEvalUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JavaEvalUtil.class);

    private static void dump(String javaSource) throws IOException {
        String className = getClassName(javaSource);

        String basePath = new File("").getAbsolutePath();
        String finalPath = StringUtils.join(new String[]{basePath, "eval-output", className + ".java"}, File.separator);

        // 创建上级目录
        File file = new File(finalPath);
        Files.createParentDirs(file);

        LOG.error("java eval output:{}", file.getAbsolutePath());
        LOG.error("\n{}", javaSource);
        Files.write(javaSource.getBytes(Charsets.UTF_8), file);
    }

    /**
     * 编译并生成class
     *
     * @param sourceCode 源码内容
     * @return
     */
    private static Class<?> compile(String sourceCode) {
        String className = getClassName(sourceCode);

        DynamicCompiler dynamicCompiler = new DynamicCompiler(JavaEvalUtil.class.getClassLoader());
        dynamicCompiler.addSource(className, sourceCode);

        Map<String, Class<?>> build = dynamicCompiler.build();
        if (build.isEmpty()) {
            throw new RuntimeException("java eval compile error");
        }
        return build.values().iterator().next();
    }

    private static String getClassName(String sourceCode) {
        return StringUtils.trim(StringUtils.substringBetween(sourceCode, "public class ", "{"));
    }

    /**
     * 反射调用
     *
     * @param methtClass
     * @return
     * @throws Exception
     */
    private static Object call(Class<?> methtClass) throws Exception {
        Method[] declaredMethods = methtClass.getDeclaredMethods();

        for (Method declaredMethod : declaredMethods) {
            if (Modifier.isPublic(declaredMethod.getModifiers()) && Modifier.isStatic(declaredMethod.getModifiers())) {
                Object result = declaredMethod.invoke(null);

                LOG.error("JavaEval return: {}", JSON.toJSON(result));
                return result;
            }
        }

        throw new RuntimeException("NO method is [public static], cannot eval");
    }

    /**
     * 对外API，执行文件得结果
     *
     * @param content
     * @return
     * @throws Exception
     */
    public static Object eval(String content) throws Exception {
        // 类dump
        dump(content);

        // 编译生成内存二进制，并加载为class
        Class<?> compile = compile(content);

        // 反射调用class
        return call(compile);
    }
}
