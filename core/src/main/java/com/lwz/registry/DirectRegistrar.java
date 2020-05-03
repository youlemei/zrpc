package com.lwz.registry;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author liweizhou 2020/4/17
 */
public class DirectRegistrar implements Registrar {

    private Map<String, List<ServerInfo>> serverInfoMap = new HashMap<>();

    public DirectRegistrar(Map<String, List<String>> directMap) {
        directMap.forEach((server, nodes) -> {
            Assert.isTrue(!CollectionUtils.isEmpty(nodes), String.format("server %s nodes must not empty", server));
            List<ServerInfo> serverInfos = new ArrayList<>();
            for (String node : nodes) {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, String.format("server %s node must define as 'host:port'", server));
                serverInfos.add(new ServerInfo(parts[0], Integer.valueOf(parts[1])));
            }
            serverInfoMap.put(server, serverInfos);
        });
    }

    @Override
    public void addListener(String serverName, Consumer<List<ServerInfo>> listener) {
        listener.accept(serverInfoMap.getOrDefault(serverName, Collections.emptyList()));
    }
}
