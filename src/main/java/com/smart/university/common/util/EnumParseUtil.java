package com.smart.university.common.util;

import cn.hutool.core.util.StrUtil;

import java.util.function.Function;

/**
 * 枚举解析工具，用于把前端传入的字符串安全转换为枚举
 */
public final class EnumParseUtil {

    private EnumParseUtil() {
    }

    /**
     * 按枚举名解析枚举，解析失败返回 null
     *
     * @param enumClass 枚举类型
     * @param value     枚举名
     * @param <E>       枚举泛型
     * @return 枚举实例，未匹配时返回 null
     */
    public static <E extends Enum<E>> E parseOrNull(Class<E> enumClass, String value) {
        if (enumClass == null || StrUtil.isBlank(value)) {
            return null;
        }
        for (E each : enumClass.getEnumConstants()) {
            if (each.name().equals(value)) {
                return each;
            }
        }
        return null;
    }

    /**
     * 按枚举名解析枚举，解析失败返回默认值
     *
     * @param enumClass   枚举类型
     * @param value       枚举名
     * @param defaultValue 解析失败时的默认值
     * @param <E>         枚举泛型
     * @return 枚举实例
     */
    public static <E extends Enum<E>> E parseOrDefault(Class<E> enumClass, String value, E defaultValue) {
        E result = parseOrNull(enumClass, value);
        return result == null ? defaultValue : result;
    }

    /**
     * 按枚举名解析枚举，解析失败抛业务异常前的取值函数
     *
     * @param enumClass 枚举类型
     * @param value     枚举名
     * @param mapper    解析成功后的转换函数
     * @param <E>       枚举泛型
     * @param <R>       返回值泛型
     * @return 转换结果，未匹配时返回 null
     */
    public static <E extends Enum<E>, R> R parseAndMap(Class<E> enumClass, String value,
                                                       Function<? super E, ? extends R> mapper) {
        E result = parseOrNull(enumClass, value);
        return result == null ? null : mapper.apply(result);
    }
}
