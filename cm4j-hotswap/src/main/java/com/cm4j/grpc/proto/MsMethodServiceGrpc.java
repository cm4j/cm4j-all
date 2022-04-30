package com.cm4j.grpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 *跨服调用方法
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.34.1)",
    comments = "Source: grpc/MsMethod.proto")
public final class MsMethodServiceGrpc {

  private MsMethodServiceGrpc() {}

  public static final String SERVICE_NAME = "MsMethodService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ,
      com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> getInvokerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "invoker",
      requestType = com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ.class,
      responseType = com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ,
      com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> getInvokerMethod() {
    io.grpc.MethodDescriptor<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ, com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> getInvokerMethod;
    if ((getInvokerMethod = MsMethodServiceGrpc.getInvokerMethod) == null) {
      synchronized (MsMethodServiceGrpc.class) {
        if ((getInvokerMethod = MsMethodServiceGrpc.getInvokerMethod) == null) {
          MsMethodServiceGrpc.getInvokerMethod = getInvokerMethod =
              io.grpc.MethodDescriptor.<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ, com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "invoker"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP.getDefaultInstance()))
              .setSchemaDescriptor(new MsMethodServiceMethodDescriptorSupplier("invoker"))
              .build();
        }
      }
    }
    return getInvokerMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MsMethodServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceStub>() {
        @java.lang.Override
        public MsMethodServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MsMethodServiceStub(channel, callOptions);
        }
      };
    return MsMethodServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MsMethodServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceBlockingStub>() {
        @java.lang.Override
        public MsMethodServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MsMethodServiceBlockingStub(channel, callOptions);
        }
      };
    return MsMethodServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MsMethodServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MsMethodServiceFutureStub>() {
        @java.lang.Override
        public MsMethodServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MsMethodServiceFutureStub(channel, callOptions);
        }
      };
    return MsMethodServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   *跨服调用方法
   * </pre>
   */
  public static abstract class MsMethodServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * 调用方法
     * </pre>
     */
    public void invoker(com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ request,
        io.grpc.stub.StreamObserver<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> responseObserver) {
      asyncUnimplementedUnaryCall(getInvokerMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getInvokerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ,
                com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP>(
                  this, METHODID_INVOKER)))
          .build();
    }
  }

  /**
   * <pre>
   *跨服调用方法
   * </pre>
   */
  public static final class MsMethodServiceStub extends io.grpc.stub.AbstractAsyncStub<MsMethodServiceStub> {
    private MsMethodServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MsMethodServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MsMethodServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 调用方法
     * </pre>
     */
    public void invoker(com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ request,
        io.grpc.stub.StreamObserver<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInvokerMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   *跨服调用方法
   * </pre>
   */
  public static final class MsMethodServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<MsMethodServiceBlockingStub> {
    private MsMethodServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MsMethodServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MsMethodServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 调用方法
     * </pre>
     */
    public com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP invoker(com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ request) {
      return blockingUnaryCall(
          getChannel(), getInvokerMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   *跨服调用方法
   * </pre>
   */
  public static final class MsMethodServiceFutureStub extends io.grpc.stub.AbstractFutureStub<MsMethodServiceFutureStub> {
    private MsMethodServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MsMethodServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MsMethodServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 调用方法
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP> invoker(
        com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ request) {
      return futureUnaryCall(
          getChannel().newCall(getInvokerMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_INVOKER = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MsMethodServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MsMethodServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_INVOKER:
          serviceImpl.invoker((com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_REQ) request,
              (io.grpc.stub.StreamObserver<com.cm4j.grpc.proto.MS_METHOD_GRPC.MS_METHOD_RESP>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class MsMethodServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MsMethodServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.cm4j.grpc.proto.MS_METHOD_GRPC.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MsMethodService");
    }
  }

  private static final class MsMethodServiceFileDescriptorSupplier
      extends MsMethodServiceBaseDescriptorSupplier {
    MsMethodServiceFileDescriptorSupplier() {}
  }

  private static final class MsMethodServiceMethodDescriptorSupplier
      extends MsMethodServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MsMethodServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (MsMethodServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MsMethodServiceFileDescriptorSupplier())
              .addMethod(getInvokerMethod())
              .build();
        }
      }
    }
    return result;
  }
}
