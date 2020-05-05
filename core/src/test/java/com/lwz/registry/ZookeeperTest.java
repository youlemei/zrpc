package com.lwz.registry;

import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.util.List;

/**
 * @author liweizhou 2020/5/3
 */
public class ZookeeperTest {

    @Test
    public void testZookeeper() throws Exception{
        ZooKeeper zooKeeper = new ZooKeeper("localhost:2181", 10000, event -> System.out.println(event));
        zooKeeper.addWatch("/lwz", event -> {
            System.out.println("mode1");
        }, AddWatchMode.PERSISTENT_RECURSIVE);

        zooKeeper.addWatch("/lwz", event -> {
            System.out.println("mode2");
        }, AddWatchMode.PERSISTENT_RECURSIVE);

        //Thread.sleep(888888888);
    }

    @Test
    public void testZookeeperChildren() throws Exception{
        ZooKeeper zooKeeper = new ZooKeeper("localhost:2181", 10000, event -> System.out.println(event));

        zooKeeper.getChildren("/lwz", event -> {
                    System.out.println("mode1");
                },
                (int rc, String p, Object ctx, List<String> children, Stat stat) -> {
                    System.out.println("rc1");
                }, this);

        zooKeeper.getChildren("/lwz", event -> {
                    System.out.println("mode2");
                },
                (int rc, String p, Object ctx, List<String> children, Stat stat) -> {
                    System.out.println("rc2");
                }, this);

        //Thread.sleep(888888888);
    }

}
