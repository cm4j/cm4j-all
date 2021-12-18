package com.cm4j.grpc.config;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

/**
 * GRPC测试类
 * 主要是为了配置grpc的信息
 */
public class GrpcConfig {

    private static final Map<Integer, Pair<String, Integer>> config = Maps.newHashMap();

    public static Pair<String, Integer> getAddressPort(int serverId) {
        return config.get(serverId);
    }

    public static void addAddressPort(int serverId, String address, int port) {
        config.put(serverId, Pair.of(address, port));
    }
}
