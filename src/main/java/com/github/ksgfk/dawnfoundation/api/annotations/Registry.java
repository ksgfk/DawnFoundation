package com.github.ksgfk.dawnfoundation.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注册拥有该注解的类下的字段，目前支持
 * Item
 * Block
 * Enchantment
 * Potion
 * PotionType
 * VillagerRegistry.VillagerProfession
 * Biome
 * SoundEvent
 * KeyBinding(Client only)
 *
 * @author KSGFK create in 2019/10/21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registry {
    String modId();
}
