package com.cm4j.invoke.proxy;

import com.cm4j.grpc.client.GrpcClient;
import com.cm4j.grpc.proto.MS_METHOD_GRPC;
import com.cm4j.grpc.proto.MsMethodServiceGrpc;
import com.cm4j.grpc.server.GrpcServer;
import com.cm4j.invoke.IRemotingClass;
import com.cm4j.invoke.RemotingMethod;
import com.cm4j.util.RemotingInvokerUtil;
import com.google.common.collect.Maps;
import io.grpc.StatusRuntimeException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 本服调用的代理类
 * 主要是判断是本地调用还是远程RPC
 *
 * @author yeas.fun
 * @since 2021/8/25
 */
@SuppressWarnings("unchecked")
public class LocalProxyGenerator {

    private static final Logger log = LoggerFactory.getLogger(LocalProxyGenerator.class);

    /**
     * 映射关系
     */
    private static final Map<Class<IRemotingClass>, IRemotingClass> proxyMap = Maps.newHashMap();

    /**
     * 获取代理类
     *
     * @param clazz
     * @return
     */
    public static <T extends IRemotingClass> T getProxy(Class<T> clazz) {
        return (T) proxyMap.get(clazz);
    }

    /**
     * 生成代理类
     *
     * @param remotingClass
     */
    public static void proxy(Class<IRemotingClass> remotingClass) {
        proxyMap.put(remotingClass, generateProxy(remotingClass));
    }

    /**
     * 生成代理类
     *
     * @param remotingClass
     * @param <T>
     * @return
     */
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
            // 非本服，直接远程RPC调用
            int sid = Integer.parseInt(String.valueOf(params[0]));
            if (sid > 0 && !GrpcServer.isSameServer(sid)) {
                return grpc(remotingClass, methodName, params);
            }

            // 本服直调，调用热更对象【非调用代理对象】
            return RemotingInvokerUtil.invoke(remotingClass, methodName, params);
        });

        return (T) enhancer.create();
    }

    /**
     * 发送远程rpc请求
     *
     * @param clazz
     * @param method
     * @param params
     * @return
     */
    public static Object grpc(Class<? extends IRemotingClass> clazz, String method, Object[] params)
            throws Exception {
        try {
            final MS_METHOD_GRPC.MS_METHOD_REQ.Builder req = MS_METHOD_GRPC.MS_METHOD_REQ.newBuilder();

            req.setClassName(clazz.getName());
            req.setMethodName(method);
            req.setParams(RemotingInvokerUtil.encodeParams(params));

            int sid = Integer.parseInt(String.valueOf(params[0]));
            MsMethodServiceGrpc.MsMethodServiceBlockingStub blockingStub = GrpcClient.getBlockingStub0(sid, MsMethodServiceGrpc.class);
            MS_METHOD_GRPC.MS_METHOD_RESP resp = blockingStub.invoker(req.build());


            if (resp.hasReback()) {
                Object[] reback = RemotingInvokerUtil.decodeParams(resp.getReback());
                return reback[0];
            }
        } catch (StatusRuntimeException e) {
            // 远端异常
            log.error("调用远端异常 grpc[{}].[{}] error...", clazz.getName(), method, e);
            throw e;
        } catch (Exception e) {
            log.error("本服异常 grpc[{}].[{}] error...", clazz.getName(), method, e);
            // 异常上抛,外层逻辑处理
            throw e;
        }
        return null;
    }

}
