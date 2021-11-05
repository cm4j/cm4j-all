package com.cm4j.hotswap.agent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.agent.JavaDynAgent;
import com.cm4j.agent.JavaDynAgentLocation;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * API：Agent方式热更新
 */
public class JavaAgent {

    public static final Logger log = LoggerFactory.getLogger(JavaAgent.class);

    private static String jarPath;
    private static VirtualMachine vm;
    private static String pid;

    static {
        jarPath = getJarPath();
        log.error("java agent:jarPath:{}", jarPath);

        // 当前进程pid
        String name = ManagementFactory.getRuntimeMXBean().getName();
        pid = StringUtils.substringBefore(name, "@");
        log.error("current pid {}", pid);
    }

    /**
     * 获取jar包路径
     *
     * @return
     */
    private static String getJarPath() {
        // 基于jar包中的类定位jar包位置
        String path = JavaDynAgentLocation.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        // 定位绝对路径
        return new File(path).getAbsolutePath();
    }

    private static void init()
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        if (JavaDynAgent.getInstrumentation() != null) {
            // 已经有此对象，则无需再次初始化获取
            return;
        }
        // 连接虚拟机，并attach当前agent的jar包
        // agentmain()方法会设置Instrumentation
        vm = VirtualMachine.attach(pid);
        vm.loadAgent(jarPath);

        // 从而获取到当前虚拟机
        Instrumentation instrumentation = JavaDynAgent.getInstrumentation();
        if (instrumentation == null) {
            log.error("instrumentation is null");
        }
    }

    private static void destroy() throws IOException {
        if (vm != null) {
            vm.detach();
        }
        log.error("java agent redefine classes end");
    }

    /**
     * 从jar包重新加载类
     *
     * @param classArr
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws UnmodifiableClassException
     * @throws AttachNotSupportedException
     * @throws AgentLoadException
     * @throws AgentInitializationException
     */
    public static void javaAgent(String[] classArr)
            throws ClassNotFoundException, IOException, UnmodifiableClassException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        log.error("java agent redefine classes started");
        init();
        try {
            LinkedHashMap<String, LinkedHashSet<Class<?>>> redefineMap = Maps.newLinkedHashMap();
            // 1.整理需要重定义的类
            List<ClassDefinition> classDefList = new ArrayList<ClassDefinition>();
            for (String className : classArr) {
                Class<?> c = Class.forName(className);
                String classLocation = c.getProtectionDomain().getCodeSource().getLocation().getPath();
                LinkedHashSet<Class<?>> classSet = redefineMap.computeIfAbsent(classLocation,
                        k -> Sets.newLinkedHashSet());
                classSet.add(c);
            }
            if (!redefineMap.isEmpty()) {
                for (Entry<String, LinkedHashSet<Class<?>>> entry : redefineMap.entrySet()) {
                    String classLocation = entry.getKey();
                    log.error("class read from:{}", classLocation);
                    if (classLocation.endsWith(".jar")) {
                        try (JarFile jf = new JarFile(classLocation)) {
                            for (Class<?> cls : entry.getValue()) {
                                String clazz = cls.getName().replace('.', '/') + ".class";
                                JarEntry je = jf.getJarEntry(clazz);
                                if (je != null) {
                                    log.error("class redefined:\t{}", clazz);
                                    try (InputStream stream = jf.getInputStream(je)) {
                                        byte[] data = IOUtils.toByteArray(stream);
                                        classDefList.add(new ClassDefinition(cls, data));
                                    }
                                } else {
                                    throw new IOException("JarEntry " + clazz + " not found");
                                }
                            }
                        }
                    } else {
                        File file;
                        for (Class<?> cls : entry.getValue()) {
                            String clazz = cls.getName().replace('.', '/') + ".class";
                            file = new File(classLocation, clazz);
                            log.error("class redefined:{}", file.getAbsolutePath());
                            byte[] data = FileUtils.readFileToByteArray(file);
                            classDefList.add(new ClassDefinition(cls, data));
                        }
                    }
                }
                // 2.redefine
                JavaDynAgent.getInstrumentation().redefineClasses(classDefList.toArray(new ClassDefinition[0]));
            }
        } finally {
            destroy();
        }
    }

}
