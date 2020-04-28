package com.lwz.registry;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
public class DirectRegistrar implements Registrar {

    private List<ServerInfo> serverInfos;

    public DirectRegistrar(List<String> nodes) {
        Assert.isTrue(!CollectionUtils.isEmpty(nodes), "nodes must not empty");
        serverInfos = new ArrayList<>(nodes.size());
        for (String node : nodes) {
            String[] parts = StringUtils.split(node, ":");
            Assert.state(parts.length == 2, "node must define as 'host:port'");
            serverInfos.add(new ServerInfo(parts[0], Integer.valueOf(parts[1])));
        }
    }

    @Override
    public void setListener(Consumer<List<ServerInfo>> listener) {
        listener.accept(serverInfos);
    }
}
