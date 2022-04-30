package com.cm4j.grpc.service;

import com.cm4j.grpc.proto.MS_METHOD_GRPC;
import com.cm4j.grpc.proto.MsMethodServiceGrpc;
import com.cm4j.util.RemotingInvokerUtil;
import com.google.protobuf.MessageLite;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:远程方法调用
 *
 * @author yeas.fun
 * @since 2021/8/27
 */
public class MsMethodServiceImpl extends MsMethodServiceGrpc.MsMethodServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(MsMethodServiceImpl.class);

    @Override
    public void invoker(MS_METHOD_GRPC.MS_METHOD_REQ request, StreamObserver<MS_METHOD_GRPC.MS_METHOD_RESP> responseObserver) {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        MS_METHOD_GRPC.MS_METHOD_RESP.Builder resp = MS_METHOD_GRPC.MS_METHOD_RESP.newBuilder();
        try {
            Object invoke = RemotingInvokerUtil.remoteInvoke(className, methodName, request);
            if (invoke != null) {
                resp.setReback(RemotingInvokerUtil.encodeParams(new Object[]{invoke}));
            }
            responseObserver.onNext(resp.build());
        } catch (StatusRuntimeException e) {
            String message = "异常经过服>>>" + e.getStatus().getDescription();
            Status status = e.getStatus().withDescription(message);
            responseObserver.onError(status.asRuntimeException());
        } catch (Exception e) {
            String description = "异常发生服 grpc 执行异常：" + e.getMessage();
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription(description).withCause(e).asRuntimeException());
            log.error("invoker[{}.{}] error", className, methodName, e);
        } finally {
            responseObserver.onCompleted();
        }
    }

}
