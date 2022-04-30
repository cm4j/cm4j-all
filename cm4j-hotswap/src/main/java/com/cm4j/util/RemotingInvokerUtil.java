package com.cm4j.util;

import com.cm4j.config.ErrorCode;
import com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ;
import com.cm4j.invoke.IRemotingClass;
import com.cm4j.invoke.invoker.RemotingInvokerScanner;
import com.cm4j.invoke.invoker.RemotingParamVO;
import com.cm4j.registry.registry.InvokerRegistry;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * 远程方法调用工具类
 *
 * @author yeas.fun
 * @since 2021/8/21
 */
@SuppressWarnings("unchecked")
public class RemotingInvokerUtil {

    private static final Logger logger = LoggerFactory.getLogger(RemotingInvokerUtil.class);

    private static final Map<String, MethodAccess> methodAccessMap = Maps.newHashMap();

    /**
     * 初始化
     *
     * @throws Exception
     */
    public static void init() throws Exception {
        RemotingInvokerScanner.init(IRemotingClass.class.getPackage().getName());
    }

    /**
     * 方法调用，非反射
     *
     * @param remotingClass
     * @param method
     * @param params
     * @param <R>
     * @return
     */
    public static <R> R invoke(Class<? extends IRemotingClass> remotingClass, String method, Object... params)
            throws Exception {
        return invoke(remotingClass.getName(), method, params);
    }

    /**
     * 方法调用，非反射
     *
     * @param className
     * @param method
     * @param params
     * @param <R>
     * @return
     */
    public static <R> R invoke(String className, String method, Object... params) throws Exception {
        // 获取注册的对象
        IRemotingClass targetObject = InvokerRegistry.getInstance().get(className);
        Preconditions.checkNotNull(targetObject, "注册regitstry系统未找到该类：" + className);

        // 获取methodAccess
        MethodAccess methodAccess = methodAccessMap.get(className);
        Preconditions.checkNotNull(methodAccess, "远程调用未初始化，请调用：RemotingInvokerScanner#init()");

        // 执行
        return (R) methodAccess.invoke(targetObject, method, params);
    }

    /**
     * 远端方法调用
     *
     * @param className
     * @param methodName
     * @param request
     * @return
     * @throws Exception
     */
    public static Object remoteInvoke(String className, String methodName, MS_METHOD_REQ request) throws Exception {
        Object[] params = decodeParams(request.getParams());
        return invoke(className, methodName, params);
    }

    /**
     * 检测是否支持该数据类型
     * <p>
     * 一般对象可能会有人在方法内修改对象的信息,这个grpc无法支持修改原有的引用信息,所以目前只之前前面的指定类型
     * <p>
     * protostuff理论上所有的pojo都能支持，这里限制主要是避免误用，（跨服方法调用只能传递值的信息，不能修改原有引用数据的信息）
     *
     * @return 是否是支持的数据类型
     */
    public static boolean checkClassTypeSupport(Class<?> classType) {
        if (classType.isPrimitive() || String.class.equals(classType)) {
            return true;
        }
        if (Number.class.isAssignableFrom(classType)) {
            return true;
        }

        // 不支持接口类和抽象类
        if (classType.isInterface() || Modifier.isAbstract(classType.getModifiers())) {
            return false;
        }

        if (ErrorCode.class.equals(classType)) {
            return true;
        }
        if (MessageLite.class.isAssignableFrom(classType)) {
            return true;
        }

        // 扩展其他类型
        return false;
    }

    /**
     * 第一个参数支持的数据类型, 用于判定方法执行的服务器
     */
    private static final Set<Class<?>> firstParamType = Sets.newHashSet(int.class, Integer.class, long.class,
            Long.class, String.class);

    public static boolean checkFirstParamType(Class<?> classType) {
        return firstParamType.contains(classType);
    }

    public static void addInvoker(String className, MethodAccess methodAccess) {
        MethodAccess old = methodAccessMap.put(className, methodAccess);
        if (old != null) {
            logger.error("class [{}] replace MethodAccess {} -> {}", className, old, methodAccess);
        }
    }

    /**
     * 转换远程传输参数
     *
     * @return protobuff的ByteString, 即proto文件中的bytes
     */
    public static ByteString encodeParams(Object[] params) {
        RemotingParamVO remotingParamVO = new RemotingParamVO();
        remotingParamVO.setParams(params);
        return ByteString.copyFrom(ProtoStuffUtil.encode(remotingParamVO));
    }

    /**
     * 还原参数
     *
     * @param paramBytes
     * @return
     */
    public static Object[] decodeParams(ByteString paramBytes) {
        RemotingParamVO remotingParamVO = ProtoStuffUtil.decode(paramBytes.toByteArray(), RemotingParamVO.class);
        return remotingParamVO.getParams();
    }

}