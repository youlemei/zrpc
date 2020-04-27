package com.lwz.util;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class IPUtils {

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
        List<String> ipList = getHostAddress();
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

    private static List<String> getHostAddress() {
        Enumeration<NetworkInterface> nis;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
        List<String> ipList = new ArrayList<>();
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if ("eth0".equalsIgnoreCase(ni.getName())) {
                Enumeration<NetworkInterface> subNis = ni.getSubInterfaces();
                while (subNis.hasMoreElements()) {
                    getHostAddress(ipList, subNis.nextElement());
                }
            }
            getHostAddress(ipList, ni);
        }
        return ipList;
    }

    private static void getHostAddress(List<String> ipList, NetworkInterface ni) {
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
