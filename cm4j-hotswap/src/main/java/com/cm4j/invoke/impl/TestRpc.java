package com.cm4j.invoke.impl;

import com.cm4j.invoke.IRemotingClass;
import com.cm4j.invoke.RemotingMethod;
import com.cm4j.invoke.proxy.LocalProxyGenerator;

public class TestRpc implements IRemotingClass {

    @RemotingMethod
    public String rpcTest(int sid, String data) {
        System.out.println("执行线程：" + Thread.currentThread());
        return "sid:" + sid + ",data:" + data;
    }

    public static TestRpc getInstance() {
        return LocalProxyGenerator.getProxy(TestRpc.class);
    }
}
