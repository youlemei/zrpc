package com.lwz.registry;

import java.util.List;

/**
 * @author liweizhou 2020/4/17
 */
public interface Registrar {

    List<ServerInfo> getServerInfos();

    default void signIn(){}

    default void signOut(){}

}
