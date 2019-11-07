package com.github.ksgfk.dawnfoundation.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记需要注册的Entity类
 *
 * @author KSGFK create in 2019/10/31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntityRegistry {
    String modId();

    boolean hasCustomFunction();

    /**
     * 字符串ID
     */
    String name();

    /**
     * 用于网络的数字ID
     */
    int id();

    int updateRange() default 64;

    int updateFrequency() default 3;

    boolean isSendVelocityUpdates() default true;

    /**
     * 若使用默认参数，则不会生成怪物蛋
     */
    int eggColor1() default -1;

    /**
     * 若使用默认参数，则不会生成怪物蛋
     */
    int eggColor2() default -1;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Custom {
    }
}
