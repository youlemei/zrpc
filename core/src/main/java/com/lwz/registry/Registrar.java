package com.lwz.registry;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
public interface Registrar {

    void addListener(String serverName, Consumer<List<ServerInfo>> listener);

    default void register(String serverName, ServerInfo serverInfo){}

    default void unRegister(){}

}
