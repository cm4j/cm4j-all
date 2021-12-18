package com.cm4j.invoke.impl;

import com.cm4j.grpc.config.GrpcConfig;
import com.cm4j.grpc.server.GrpcServer;
import com.cm4j.registry.AbstractRegistry;
import com.cm4j.registry.registry.InvokerRegistry;
import com.cm4j.util.RemotingInvokerUtil;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;

public class TestRpcTest {

    @Before
    public void init() throws Exception {
        // 手动配置grpc参数
        GrpcConfig.addAddressPort(1, "127.0.0.1", 6666);
        GrpcConfig.addAddressPort(2, "127.0.0.1", 8888);

        // 启动InvokerRegistry注册，这里是反射调用
        // 正常业务：应该用spring注册，或者包扫描进行注册
        startRegistry(InvokerRegistry.class);

        // 启动2服的服务器
        GrpcServer grpcServer = new GrpcServer(8888);

        // 远程方法调用初始化
        RemotingInvokerUtil.init();
    }

    /**
     * 启动注册类，用于测试
     *
     * @param clazz
     */
    public static void startRegistry(Class<? extends AbstractRegistry> clazz) {
        try {
            Constructor<? extends AbstractRegistry> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void rpcTest() {
        // 假设当前是1服
        GrpcServer.setServerId(1);

        System.out.println("启动线程：" + Thread.currentThread());

        // 本服：直接执行逻辑。打印中方法执行线程就是启动线程
        TestRpc.getInstance().rpcTest(1, "1234");

        // 跨服：请求2服的数据。打印方法中执行线程是另一个线程
        String result = TestRpc.getInstance().rpcTest(2, "1234");
        System.out.println(result);
    }
}