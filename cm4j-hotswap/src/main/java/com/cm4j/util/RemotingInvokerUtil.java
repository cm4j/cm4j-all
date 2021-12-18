package com.cm4j.util;

import com.cm4j.config.ErrorCode;
import com.cm4j.grpc.proto.MS_METHOD_GRPC;
import com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ;
import com.cm4j.grpc.proto.MS_METHOD_GRPC.PRIMITIVE_PARAM;
import com.cm4j.invoke.IRemotingClass;
import com.cm4j.invoke.invoker.IRemotingInvoker;
import com.cm4j.invoke.invoker.RemotingInvokerGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Collection;
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

    private static final Logger log = LoggerFactory.getLogger(RemotingInvokerUtil.class);


    /**
     * "#" - 井号分隔符
     */
    private static final String SPLIT_SHARP = "#";

    private static Map<String, IRemotingInvoker> invokerMap = Maps.newHashMap();

    private static Map<Class<?>, Parser<?>> protoMessageParser = Maps.newHashMap();

    /**
     * grpc null传递特殊字符串
     */
    private static final String NULL_PARAM_TRANSFER_FLAG = "NULL_PARAM_TRANSFER_FLAG";
    /**
     * grpc传递null值标识
     * 如果不设值没法区分是MessageLite本身没有内容,还是传递的是null,所以设置一个特殊字符串表明null传递
     */
    private static final MS_METHOD_GRPC.PRIMITIVE_PARAM NULL_PARAM = MS_METHOD_GRPC.PRIMITIVE_PARAM.newBuilder()
            .setParamString(NULL_PARAM_TRANSFER_FLAG)
            .build();
    private static final ByteString NULL_PARAM_BYTE_STRING = NULL_PARAM.toByteString();

    /**
     * 初始化
     *
     * @throws Exception
     */
    public static void init() throws Exception {
        RemotingInvokerGenerator.init(IRemotingClass.class.getPackage().getName());
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
        IRemotingInvoker invoker = invokerMap.get(className);
        Preconditions.checkNotNull(invoker, "远程调用未初始化，请调用：RemotingInvokerGenerator#init()");
        return (R) invoker.invokeInternal(method, params);
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
        String methodPositioning = getMethodPositioning(request.getClassName(), request.getMethodName());
        Collection<Class<?>> paramClassTypes = RemotingInvokerGenerator.getClassMethodParamTypes()
                .get(methodPositioning);
        Object[] params = new Object[paramClassTypes.size()];
        int i = 0;
        for (Class<?> paramClassType : paramClassTypes) {
            params[i] = messageLite2Object(paramClassType, request.getParams(i));
            i++;
        }
        return invoke(className, methodName, params);
    }

    /**
     * 获取方法key
     *
     * @param remotingClass
     * @param method
     * @return
     */
    public static String getMethodPositioning(Class<? extends IRemotingClass> remotingClass, String method) {
        return getMethodPositioning(remotingClass.getName(), method);
    }

    /**
     * 获取方法key
     *
     * @param className
     * @param method
     * @return
     */
    public static String getMethodPositioning(String className, String method) {
        return Joiner.on(SPLIT_SHARP).join(className, method);
    }

    /**
     * object转proto类
     *
     * @param param
     * @return
     */
    public static MessageLite object2MessageLite(Object param) {
        // 支持传递null
        if (param == null) {
            return NULL_PARAM;
        }
        Class<?> paramClassType = param.getClass();
        if (Boolean.class.equals(paramClassType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamBool((Boolean) param);
            return primitiveParam.build();
        } else if (Integer.class.equals(paramClassType) || Byte.class.equals(paramClassType) || Short.class.equals(
                paramClassType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamNumber((int) param);
            return primitiveParam.build();
        } else if (Long.class.equals(paramClassType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamLong((Long) param);
            return primitiveParam.build();
        } else if (Float.class.equals(paramClassType) || Double.class.equals(paramClassType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamDouble((double) param);
            return primitiveParam.build();
        } else if (paramClassType == String.class) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamString((String) param);
            return primitiveParam.build();
        } else if (ErrorCode.class.equals(paramClassType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.setParamNumber(((ErrorCode) param).getShortCode());
            return primitiveParam.build();
        } else if (MessageLite.class.isAssignableFrom(paramClassType)) {
            return (MessageLite) param;
        }
        return null;
    }

    /**
     * proto转为对应object
     *
     * @param classType
     * @param result
     * @return
     */
    public static Object messageLite2Object(Class<?> classType, ByteString result) throws Exception {
        // 传递参数是null的情况
        if (NULL_PARAM_BYTE_STRING.equals(result)) {
            return null;
        }
        if (classType.isPrimitive()) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            if (Boolean.TYPE.equals(classType)) {
                return primitiveParam.getParamBool();
            } else if (Byte.TYPE.equals(classType) || Short.TYPE.equals(classType) || Integer.TYPE.equals(classType)) {
                return primitiveParam.getParamNumber();
            } else if (Long.TYPE.equals(classType)) {
                return primitiveParam.getParamLong();
            } else if (Float.TYPE.equals(classType) || Double.TYPE.equals(classType)) {
                return primitiveParam.getParamDouble();
            }
        } else if (Boolean.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            return primitiveParam.getParamBool();
        } else if (Integer.class.equals(classType) || Byte.class.equals(classType) || Short.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            return primitiveParam.getParamNumber();
        } else if (Long.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            return primitiveParam.getParamLong();
        } else if (Float.class.equals(classType) || Double.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            return primitiveParam.getParamDouble();
        } else if (String.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            return primitiveParam.getParamString();
        } else if (ErrorCode.class.equals(classType)) {
            PRIMITIVE_PARAM.Builder primitiveParam = PRIMITIVE_PARAM.newBuilder();
            primitiveParam.mergeFrom(result);
            int errorCode = primitiveParam.getParamNumber();
            return ErrorCode.getErrorCode(String.valueOf(errorCode));
        } else if (MessageLite.class.isAssignableFrom(classType)) {
            Parser<?> parser = protoMessageParser.computeIfAbsent(classType, k -> {
                try {
                    return (Parser<?>) k.getField("PARSER").get(null);
                } catch (Exception e) {
                    return null;
                }
            });
            if (parser == null) {
                log.error("classType[{}] parser not found...", classType.getName());
                return null;
            }
            return parser.parseFrom(result);
        }
        return null;
    }

    /**
     * 检测是否支持该数据类型
     * <p>
     * 一般对象可能会有人在方法内修改对象的信息,这个grpc无法支持修改原有的引用信息,所以目前只之前前面的指定类型
     *
     * @return 是否是支持的数据类型
     */
    public static boolean checkClassTypeSupport(Class<?> classType) {
        // char屏蔽掉
        if (char.class.equals(classType)) {
            return false;
        }
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
    private static Set<Class<?>> firstParamType = Sets.newHashSet(int.class, Integer.class, long.class, Long.class,
            String.class);

    public static boolean checkFirstParamType(Class<?> classType) {
        return firstParamType.contains(classType);
    }

    public static void addInvoker(String className, IRemotingInvoker invoker) {
        IRemotingInvoker old = invokerMap.put(className, invoker);
        if (old != null) {
            log.error("className[{}] replace invoker {} -> {}", className, old.getClass().getName(),
                    invoker.getClass().getName());
        }
    }
}
