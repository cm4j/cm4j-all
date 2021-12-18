package com.cm4j.util;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author yeas.fun
 * @since 2021/11/5
 */
public class PackageUtilTest {

    @Test
    public void findPackageClass() {
        Set<Class<?>> packageClass = PackageUtil.findPackageClass("com.cm4j");
        assertFalse(packageClass.isEmpty());
    }
}