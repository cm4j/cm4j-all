package com.cm4j.registry;

import java.util.Map;
import java.util.Set;

import com.cm4j.hotswap.recompile.RecompileHotSwap;
import com.cm4j.registry.registered.IRegistered;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 所有的注册容器管理类
 *
 * @author yeas.fun
 * @since 2021/1/9
 */
public class RegistryManager {

    private Set<AbstractRegistry> allRegistry = Sets.newConcurrentHashSet();

    public void addRegistry(AbstractRegistry registry) {
        allRegistry.add(registry);
    }

    public Set<AbstractRegistry> getAllRegistry() {
        return allRegistry;
    }

    /**
     * 热更注册系统内的对象
     *
     * @param originClassNames
     * @throws Exception
     */
    public void hotswap(String[] originClassNames) throws Exception {
        Map<Class<?>, IRegistered> map = Maps.newLinkedHashMap();

        for (String originClassName : originClassNames) {
            Class<?> originClass = Class.forName(originClassName);
            if (!IRegistered.class.isAssignableFrom(originClass)) {
                throw new RuntimeException("class is not IRegistered:" + originClassName);
            }

            Class<?> newClass = RecompileHotSwap.recompileClass(originClass);
            IRegistered newObj = (IRegistered) newClass.newInstance();

            map.put(originClass, newObj);
        }

        // 全部都成功了，再一个个进行热更替换

        // 注册对象替换
        map.forEach((originClass, newObj) -> {
            for (AbstractRegistry registry : RegistryManager.getInstance().getAllRegistry()) {
                try {
                    registry.hotswap(originClass, newObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * 动态注册新对象
     *
     * @param originClassNames
     */
    public void registerNewOne(String[] originClassNames) throws Exception {
        Set<IRegistered> objects = Sets.newHashSet();
        for (String originClassName : originClassNames) {
            Class<?> originClass = Class.forName(originClassName);
            if (!IRegistered.class.isAssignableFrom(originClass)) {
                throw new RuntimeException("class is not IRegistered:" + originClassName);
            }

            objects.add((IRegistered) originClass.newInstance());
        }

        for (IRegistered target : objects) {
            for (AbstractRegistry registry : RegistryManager.getInstance().getAllRegistry()) {
                try {
                    registry.registerNewOne(target);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class HOLDER {

        private static final RegistryManager instance = new RegistryManager();
    }

    public static RegistryManager getInstance() {
        return HOLDER.instance;
    }
}
