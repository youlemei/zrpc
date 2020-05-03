package com.lwz.registry;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
public interface Registrar {

    void setListener(Consumer<List<ServerInfo>> listener);

    default void signIn(ServerInfo serverInfo){}

    default void signOut(){}

}
