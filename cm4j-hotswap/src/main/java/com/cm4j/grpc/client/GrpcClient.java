package com.cm4j.grpc.client;

import com.cm4j.grpc.config.GrpcConfig;
import com.cm4j.grpc.proto.MsMethodServiceGrpc;
import com.google.common.collect.Maps;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractBlockingStub;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcClient.class);

    /**
     * gRPC blocking deadline time (seconds)
     **/
    private static int GRPC_BLOCKING_DEADLINE_SECONDS = 4;

    /**
     * 同步RPC请求服务关系,K-grpcClass.getName(),V-newBlockingStub
     */
    private static final Map<String, Method> GRPC_SERVICE_BLOCKING_METHODS = Maps.newConcurrentMap();
    /**
     * RPC连接信息,K-mainSid,V-连接信息
     */
    private static final Map<Integer, ManagedChannel> GRPC_CHANNELS = Maps.newConcurrentMap();

    /**
     * 获取channel
     *
     * @param serverId
     * @return
     */
    private static ManagedChannel getChannel(int serverId) {
        ManagedChannel managedChannel = GRPC_CHANNELS.get(serverId);
        if (managedChannel == null) {
            Pair<String, Integer> pair = GrpcConfig.getAddressPort(serverId);
            managedChannel = ManagedChannelBuilder.forAddress(pair.getLeft(), pair.getRight()).usePlaintext().build();
            GRPC_CHANNELS.put(serverId, managedChannel);
        }
        return managedChannel;
    }

    /**
     * 获取阻塞调用的service
     *
     * @param grpcClass *Grpc.class
     * @return
     */
    public static <T extends AbstractBlockingStub> T getBlockingStub0(int serverId, Class<?> grpcClass) {
        try {
            final ManagedChannel channel = getChannel(serverId);

            log.error("{}\r\nchannel isShutdown={} isTerminated={} state={}", channel, channel.isShutdown(),
                    channel.isTerminated(), channel.getState(false));
            Method method = GRPC_SERVICE_BLOCKING_METHODS.get(grpcClass.getName());
            if (method == null) {
                method = grpcClass.getDeclaredMethod("newBlockingStub", Channel.class);
                Method absent = GRPC_SERVICE_BLOCKING_METHODS.putIfAbsent(grpcClass.getName(), method);
                if (absent != null) {
                    method = absent;
                }
            }
            AbstractBlockingStub instance = (AbstractBlockingStub) method.invoke(null, channel);
            // 注意:超时时间是从withDeadline设置后就开始算了，所以不要提前获取Stub，在真正调用的时候再获取
            return (T) instance.withDeadline(Deadline.after(GRPC_BLOCKING_DEADLINE_SECONDS, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("getBlockingStub error, grpcClass={},  deadLineSec={}", grpcClass.getName(),
                    GRPC_BLOCKING_DEADLINE_SECONDS, e);
        }
        return null;
    }

}
