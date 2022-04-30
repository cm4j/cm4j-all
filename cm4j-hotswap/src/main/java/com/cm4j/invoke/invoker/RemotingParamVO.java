package com.cm4j.invoke.invoker;

/**
 * Description:远程grpc传输参数
 *
 * @author yeas.fun
 * @date 2022/4/21
 */
public class RemotingParamVO {

    public Object[] params;

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

}
