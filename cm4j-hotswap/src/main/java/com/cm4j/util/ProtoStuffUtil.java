package com.cm4j.util;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * protostuff缓存类
 *
 * @author yeas.fun
 * @since 2022/4/21
 */
public class ProtoStuffUtil {

    static final DefaultIdStrategy STRATEGY = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS
            | IdStrategy.PRESERVE_NULL_ELEMENTS
            | IdStrategy.MORPH_COLLECTION_INTERFACES
            | IdStrategy.MORPH_MAP_INTERFACES
            | IdStrategy.MORPH_NON_FINAL_POJOS);

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    public static <T> byte[] encode(T obj) {
        Class<?> clazz = obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            @SuppressWarnings("unchecked") Schema<T> schema = (Schema<T>) getSchema(clazz);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    public static <T> T decode(byte[] bytes, Class<T> clazz) {
        try {
            T t = clazz.newInstance();
            @SuppressWarnings("unchecked") Schema<T> schema = (Schema<T>) getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes, t, schema);
            return t;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static Schema<?> getSchema(Class<?> clazz) {
        return cachedSchema.computeIfAbsent(clazz, aClass -> RuntimeSchema.createFrom(aClass, STRATEGY));
    }
}