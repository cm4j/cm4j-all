package com.cm4j.registry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cm4j.registry.registered.IRegistered;
import com.cm4j.util.PackageUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * 注册容器
 *
 * @author yeas.fun
 * @since 2021/1/7
 */
public abstract class AbstractRegistry<K, V extends IRegistered> implements IHotswapCallback<K, V> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractRegistry.class);
    // private final Class<K> keyType;
    private final Class<V> valueType;

    /**
     * 扫描包进行注册
     *
     * @param packScan 待扫描的包,有多个包时以","分隔
     */
    protected AbstractRegistry(String packScan) {
        // 泛型中的类型
        Type[] types = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments();

        // keyType = (Class<K>)types[0];
        valueType = (Class<V>) types[1];

        initRegistry(packScan);

        RegistryManager.getInstance().addRegistry(this);
    }

    /**
     * 扫描给定包中满足条件的类,初始化
     *
     * @param packScan 待扫描的包,有多个包时以","分隔
     */
    protected void initRegistry(String packScan) {
        Set<Class<?>> packageClass = PackageUtil.findPackageClass(packScan);
        packageClass.forEach(clazz ->{
            // 排除接口和抽象类
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                return;
            }
            initRegistry(clazz);
        });
    }

    /**
     * 初始化指定的class
     *
     * @param clazz
     */
    protected void initRegistry(Class<?> clazz) {
        // 排除接口和抽象类
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }
        try {
            registerClass(clazz);
        } catch (Throwable e) {
            log.error("newInstance error: {}", clazz.getName(), e);
            Throwables.throwIfUnchecked(e);
        }
    }

    /**
     * 初始化指定的class
     *
     * @param clazz
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    private void registerClass(Class<?> clazz)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // 与注解类型相同，且类不是自己，则进行注册
        if (valueType.isAssignableFrom(clazz) && valueType != clazz) {
            try {
                // 要有默认构造函数
                Constructor c0 = clazz.getDeclaredConstructor();
                c0.setAccessible(true);

                V obj = (V) c0.newInstance();
                K[] registryKeys = getRegistryKeys(obj);
                for (K registryKey : registryKeys) {
                    register(registryKey, obj);
                }
            } catch (NoSuchMethodException e) {
                // 没有默认构造函数，忽略该类，不注册
            }
        }
    }

    /**
     * 基于对象的注册，有可能多个key对应同一个对象
     *
     * @param v
     * @return
     */
    protected abstract K[] getRegistryKeys(V v);

    /** 映射关系 */
    protected final Map<K, V> mapping = Maps.newConcurrentMap();

    /**
     * 获取注册对象
     *
     * @param k
     * @return
     */
    public V get(K k) {
        return mapping.get(k);
    }

    /**
     * 注册
     *
     * @param k
     * @param v
     */
    protected void register(K k, V v) {
        Preconditions.checkNotNull(v, "RegistryManager is null");
        V old = mapping.get(k);
        if (old != null) {
            throw new RuntimeException(MessageFormat.format("target is already existed:[{0}] -> [{1}]", k, v));
        }
        mapping.put(k, v);
        doAfterRegister(k, v);
        if (log.isDebugEnabled()) {
            log.debug("[register] success:{} -> {}", k, v);
        }
    }

    /**
     * 热更：替换对应的映射对象
     *
     * @param originClass 原始的class
     * @param newObject   新构建的热更对象，可能是SUBCLASS
     * @return true-热更成功,false-热更失败(类型不匹配,代码问题等)
     */
    public boolean hotswap(Class<V> originClass, V newObject)
            throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        // 值的类型不匹配，不是该注册类
        if (!valueType.isAssignableFrom(originClass)) {
            return false;
        }
        K[] registryKeys = getRegistryKeys(originClass.newInstance());
        K registryKey = registryKeys[0];
        V oldObject = get(registryKey);

        // 原来注册过的对象，才允许进行热更替换
        if (oldObject == null) {
            throw new RuntimeException("[hotswap] failed, cannot found oldObject:" + registryKey);
        }

        for (K k : registryKeys) {
            mapping.put(k, newObject);
            doAfterHotswap(k, oldObject, newObject);
            log.error("[hotswap] success:{} -> {}", k, newObject);
        }
        return true;
    }

    /**
     * 新注册对象
     *
     * @param v
     * @return true-注册成功,false-注册失败
     */
    public boolean registerNewOne(V v) {
        Class<? extends IRegistered> originClass = v.getClass();
        // 值的类型不匹配，不是该注册类
        if (!valueType.isAssignableFrom(originClass)) {
            return false;
        }

        K[] registryKeys = getRegistryKeys(v);
        for (K registryKey : registryKeys) {
            V existed = get(registryKey);

            // 已经有了，则无法新增对象，只能走hotswap
            if (existed != null) {
                throw new RuntimeException("registry is existed: " + registryKey);
            }
        }

        for (K registryKey : registryKeys) {
            mapping.put(registryKey, v);
            doAfterRegisterNewOne(registryKey, v);
            log.error("[registry NEW] success:{} -> {}", registryKey, v);
        }

        return true;
    }

}
