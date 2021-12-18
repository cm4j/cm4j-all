package com.cm4j.grpc.server;

import com.cm4j.grpc.service.MsMethodServiceImpl;
import io.grpc.ServerBuilder;
import io.grpc.util.MutableHandlerRegistry;

import java.io.IOException;

/**
 * 这里为了演示，只实现最简答的grpc用法
 */
public class GrpcServer {

    public GrpcServer(int port) throws IOException {
        MutableHandlerRegistry registry = new MutableHandlerRegistry();
        // 硬编码注册处理类，可替换使用扫描注册
        registry.addService(new MsMethodServiceImpl());

        // 服务器启动
        ServerBuilder.forPort(port).fallbackHandlerRegistry(registry).build().start();
    }

    // 当前服务器ID
    private static int serverId;

    /**
     * 设置当前服务器ID
     *
     * @param serverId
     */
    public static void setServerId(int serverId) {
        GrpcServer.serverId = serverId;
    }

    /**
     * 判断是否是当前服务器ID
     *
     * @param sid
     * @return
     */
    public static boolean isSameServer(int sid) {
        return serverId == sid;
    }
}
