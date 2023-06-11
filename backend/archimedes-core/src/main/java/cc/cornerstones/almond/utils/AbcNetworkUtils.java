package cc.cornerstones.almond.utils;

import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class AbcNetworkUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbcNetworkUtils.class);

    /**
     *
     */
    public static List<AbcTuple2<String, String>> getServerHostnameAndIpAddress() throws AbcUndefinedException {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            LOGGER.error("fail to retrieve network interfaces", e);
            throw new AbcResourceNotFoundException("server hostname and ip address");
        }

        List<AbcTuple2<String, String>> result = new LinkedList<>();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            try {
                if (!networkInterface.isUp()) {
                    continue;
                }

                if (networkInterface.isLoopback()) {
                    continue;
                }

                if (networkInterface.isPointToPoint()) {
                    continue;
                }

                if (networkInterface.isVirtual()) {
                    continue;
                }
            } catch (IOException e) {
                LOGGER.error("fail to extract from network interface:{}", networkInterface.getName(), e);
                continue;
            }

            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!inetAddress.isLoopbackAddress()
                        && !inetAddress.isAnyLocalAddress()
                        && !inetAddress.isLinkLocalAddress()
                        && !inetAddress.isMulticastAddress()
                        && inetAddress.isSiteLocalAddress()) {
                    if (!ObjectUtils.isEmpty(inetAddress.getHostName())
                            && !ObjectUtils.isEmpty(inetAddress.getHostAddress())) {
                        result.add(new AbcTuple2<>(inetAddress.getHostName(), inetAddress.getHostAddress()));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns server IP address (v4 or v6) bound to local NIC. If multiple NICs are present, choose 'eth0' or 'en0' or
     * any one with name ending with 0. If non found, take first on the list that is not localhost. If no NIC
     * present (relevant only for desktop deployments), return loopback address.
     *
     * copy from com.netflix.discovery.util.SystemUtil
     */
    public static String getServerIPv4() {
        String candidateAddress = null;
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                Enumeration<InetAddress> inetAddresses = nic.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    String address = inetAddresses.nextElement().getHostAddress();
                    String nicName = nic.getName();
                    if (nicName.startsWith("eth0") || nicName.startsWith("en0")) {
                        return address;
                    }
                    if (nicName.endsWith("0") || candidateAddress == null) {
                        candidateAddress = address;
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Cannot resolve local network address", e);
        }
        return candidateAddress == null ? "127.0.0.1" : candidateAddress;
    }

    public static void main(String[] args) {
        List<AbcTuple2<String, String>> result = getServerHostnameAndIpAddress();
        if (CollectionUtils.isEmpty(result)) {
            System.out.println("null");
        } else {
            result.forEach(tuple -> {
                System.out.println(tuple);
            });
        }
    }

}
