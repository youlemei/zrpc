package com.lwz.registry;

import com.lwz.client.pool.ClientPool;
import lombok.Data;

import java.util.Objects;

/**
 * @author liweizhou 2020/4/17
 */
@Data
public class ServerInfo {

    private String host;

    private int port;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ClientPool) {
            o = ((ClientPool) o).getServerInfo();
        }
        if (!(o instanceof ServerInfo)) return false;
        ServerInfo that = (ServerInfo) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
