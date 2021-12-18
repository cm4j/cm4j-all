package com.cm4j.invoke;

import com.cm4j.registry.registered.IRegistered;

/**
 * 远程类的标识
 *
 * <pre>
 *  注意点：
 *  1.方法定义不允许重载(方法名相同,参数不同)
 *  2.方法仅支持：原生类型+proto结构体+Errorcode
 *  3.方法必须是public的
 *  4.getInstance()内部必须返回的是代理类
 *  5.方法上面需要使用@RemotingMethod注解
 *  6.方法第一个参数标明调用服务器id(可以用0表示本地调用)
 *  7.类名约定为XxxRpc(例如,JoanRpc)
 *  8.需要支持grpc异步处理的方法,只支持无返回的方法
 *  </pre>
 *
 * @author yeas.fun
 * @since 2021/8/21
 */
public interface IRemotingClass extends IRegistered {

}
