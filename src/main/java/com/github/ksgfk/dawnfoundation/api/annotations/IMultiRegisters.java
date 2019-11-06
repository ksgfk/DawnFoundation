package com.github.ksgfk.dawnfoundation.api.annotations;

import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 一个Item或Block类内有多个需注册的元素时使用该接口
 *
 * @author KSGFK create in 2019/11/3
 */
@Deprecated
public interface IMultiRegisters<T extends IForgeRegistryEntry<T>> {
    @Nonnull
    List<T> getRegisters();
}
