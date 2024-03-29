package com.github.ksgfk.dawnfoundation.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记实现了 {@link net.minecraftforge.fml.common.network.IGuiHandler} 接口的类实例化后的字段（
 *
 * @author KSGFK create in 2019/11/4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GuiHandler {
    String modId();
}
