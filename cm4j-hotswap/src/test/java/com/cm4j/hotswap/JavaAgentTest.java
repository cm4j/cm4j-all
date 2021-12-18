package com.cm4j.hotswap;

import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;

import org.junit.Test;

import com.cm4j.demo.util.DemoUtil;
import com.cm4j.hotswap.agent.JavaAgent;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

/**
 * @author yeas.fun
 * @since 2021/11/4
 */
public class JavaAgentTest {

    @Test
    public void javaAgentTest()
            throws UnmodifiableClassException, AgentLoadException, IOException, AttachNotSupportedException, ClassNotFoundException, AgentInitializationException {
        JavaAgent.javaAgent(new String[]{DemoUtil.class.getName()});
    }

}