package com.lwz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author liweizhou 2020/2/17
 */
public class IPUtils {

    private static final Logger log = LoggerFactory.getLogger(IPUtils.class);

    private static String ip;

    /**
     * 获取本机ip
     *
     * @return
     */
    public static String getIp() {
        if (ip == null) {
            getLocalIp();
        }
        return ip;
    }

    private static void getLocalIp() {
        List<String> ipList = new ArrayList<>();
        try {
            getIpList(NetworkInterface.getNetworkInterfaces(), ipList);
        } catch (SocketException e) {
        }
        log.info("this machine all ip are: {}", ipList);
        if (!CollectionUtils.isEmpty(ipList)) {
            for (String tip : ipList) {
                if (tip.startsWith("127.0.0."))
                    continue;
                if (tip.startsWith("10."))
                    continue;
                ip = tip;
                return;
            }
            ip = ipList.get(0);
        }
    }

    private static void getIpList(Enumeration<NetworkInterface> nis, List<String> ipList) {
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if ("eth0".equalsIgnoreCase(ni.getName())) {
                getIpList(ni.getSubInterfaces(), ipList);
            }
            Enumeration<InetAddress> ips = ni.getInetAddresses();
            while (ips.hasMoreElements()) {
                InetAddress inet = ips.nextElement();
                String ip = inet.getHostAddress();
                if (ip.indexOf(":") == -1) {
                    // 不使用IPv6
                    ipList.add(ip);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getIp());
    }

}
