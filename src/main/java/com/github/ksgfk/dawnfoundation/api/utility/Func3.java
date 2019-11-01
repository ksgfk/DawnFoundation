package com.github.ksgfk.dawnfoundation.api.utility;

/**
 * @author KSGFK create in 2019/11/1
 */
@FunctionalInterface
public interface Func3<A, B, C> {
    void invoke(A a, B b, C c);
}
