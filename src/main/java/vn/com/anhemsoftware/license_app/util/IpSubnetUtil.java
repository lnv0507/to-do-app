package vn.com.anhemsoftware.license_app.util;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

public class IpSubnetUtil {

    private IpSubnetUtil() {
    }

    public static boolean isSameSubnet(String ip1, String ip2) {
        if (ip1 == null || ip2 == null)
            return false;
        try {
            IPAddress addr1 = new IPAddressString(ip1).getAddress();
            IPAddress addr2 = new IPAddressString(ip2).getAddress();
            if (addr1 == null || addr2 == null)
                return false;

            // IPv4: prefix /24, IPv6: prefix /48
            int prefixLen = addr1.isIPv4() ? 24 : 48;
            IPAddress subnet1 = addr1.toPrefixBlock(prefixLen);
            IPAddress subnet2 = addr2.toPrefixBlock(prefixLen);
            return subnet1.equals(subnet2);
        } catch (Exception e) {
            return false;
        }
    }
}
