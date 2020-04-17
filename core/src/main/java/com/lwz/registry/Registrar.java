package com.lwz.registry;

/**
 * @author liweizhou 2020/4/17
 */
public interface Registrar {

    ServerInfoObserver addObserver();

    default void signIn(){};

    default void signOut(){};

    default void ping(){};

}
