package com.github.ksgfk.dawnfoundation.api.utility;

/**
 * @author KSGFK create in 2019/11/7
 */
@FunctionalInterface
public interface Action1<T> {
    void invoke(T type);
}
