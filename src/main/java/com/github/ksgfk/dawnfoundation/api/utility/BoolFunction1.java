package com.github.ksgfk.dawnfoundation.api.utility;

/**
 * @author KSGFK create in 2019/11/10
 */
@FunctionalInterface
public interface BoolFunction1<T> {
    boolean invoke(T t);
}
