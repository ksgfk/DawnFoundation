package com.github.ksgfk.dawnfoundation.api.annotations;

import net.minecraft.tileentity.TileEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KSGFK create in 2019/11/4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TESRRegistry {
    Class<? extends TileEntity> tileEntity();
}
