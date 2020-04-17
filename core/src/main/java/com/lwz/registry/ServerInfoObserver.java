package com.lwz.registry;


import java.util.List;

/**
 * @author liweizhou 2020/4/17
 */
public class ServerInfoObserver implements Observer<List<ServerInfo>> {

    private boolean init;

    private List<ServerInfo> serverInfos;

    @Override
    public void update(List<ServerInfo> arg) {
        this.init = true;
        this.serverInfos = serverInfos;
    }



}
