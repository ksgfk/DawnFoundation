package com.github.ksgfk.dawnfoundation.api.annotations;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * 会被 {@link com.github.ksgfk.dawnfoundation.api.annotations.Registry} 注解获取的Item或Block必须实现该接口，否则
 * 注册时会抛出异常且注册失败
 *
 * @author KSGFK create in 2019/11/1
 */
public interface IDomainName {
    /**
     * 获取资源完整名称，
     * 例：
     * private String name = "example";
     * public ResourceLocation getDomainName() { return new ResourceLocation(Example.MOD_ID, name); }
     */
    @Nonnull
    ResourceLocation getDomainName();
}
