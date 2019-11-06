package com.github.ksgfk.dawnfoundation.api.annotations;

import net.minecraft.world.WorldProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KSGFK create in 2019/11/6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DimensionRegistry {
    /**
     * 维度数字ID
     */
    int id();

    /**
     * 维度名字
     */
    String name();

    /**
     * 用于村庄的后缀
     */
    String suffix();

    /**
     * 该维度使用的 {@link WorldProvider}。要求这个类有零参构造器。
     */
    Class<? extends WorldProvider> provider();

    /**
     * 是否保持该维度的 spawn 区块一直加载
     */
    boolean keepLoaded();
}
