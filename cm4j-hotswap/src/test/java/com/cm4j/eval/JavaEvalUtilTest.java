package com.cm4j.eval;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class JavaEvalUtilTest {

    /**
     * 直接执行代码
     *
     * @throws Exception
     */
    @Test
    public void evalTest1() throws Exception {
        String src = "public class JavaEvalDemo {\n" + "\n" + "    public static Object calc() {\n"
                + "        return 1 + 2;\n" + "    }\n" + "}";

        System.out.println("执行结果为：" + JavaEvalUtil.eval(src));
    }

    /**
     * 读取本地的类文件，然后执行代码
     *
     * @throws Exception
     */
    @Test
    public void evalTest2() throws Exception {
        String absolutePath = new File("").getAbsolutePath();
        absolutePath = Joiner.on(File.separator).join(absolutePath, "src", "test", "java");
        absolutePath +=
                File.separator + StringUtils.replace(JavaEvalDemo.class.getName(), ".", File.separator) + ".java";

        String content = Files.toString(new File(absolutePath), Charsets.UTF_8);

        System.out.println("执行结果为：" + JavaEvalUtil.eval(content));
    }
}