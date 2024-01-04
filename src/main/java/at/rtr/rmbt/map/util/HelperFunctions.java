package at.rtr.rmbt.map.util;


import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

@UtilityClass
public class HelperFunctions {

    private static final int DNS_TIMEOUT = 1;
    private static Logger logger = LoggerFactory.getLogger(HelperFunctions.class);

    public String getTimeZoneId() {
        return TimeZone.getDefault().getID();
    }

    public static String getRoamingType(final ResourceBundle messageSource, final int roamingType) {
        switch (roamingType) {
            case 0:
                return messageSource.getString("value_roaming_none");
            case 1:
                return messageSource.getString("value_roaming_national");
            case 2:
                return messageSource.getString("value_roaming_international");
        }
        return "?";
    }

    public static String geoToString(final Double geoLat, final Double geoLong) {

        if (geoLat == null || geoLong == null)
            return null;

        int latd, lond; // latitude degrees and minutes, longitude degrees and
        // minutes
        double latm, lonm; // latitude and longitude seconds.

        // decimal degrees to degrees minutes seconds

        double temp;
        // latitude
        temp = Math.abs(geoLat);
        latd = (int) temp;
        latm = (temp - latd) * 60.0;

        // longitude
        temp = Math.abs(geoLong);
        lond = (int) temp;
        lonm = (temp - lond) * 60.0;

        final String dirLat;
        if (geoLat >= 0)
            dirLat = "N";
        else
            dirLat = "S";
        final String dirLon;
        if (geoLong >= 0)
            dirLon = "E";
        else
            dirLon = "W";

        return String.format("%s %2d°%02.3f'  %s %2d°%02.3f'", dirLat, latd, latm, dirLon, lond, lonm);
    }

    public static String getNetworkTypeName(final Integer type) {
        if (type == null)
            return "UNKNOWN";
        switch (type) {
            case 1:
            case 16:
                return "2G (GSM)";
            case 2:
                return "2G (EDGE)";
            case 3:
                return "3G (UMTS)";
            case 4:
                return "2G (CDMA)";
            case 5:
                return "2G (EVDO_0)";
            case 6:
                return "2G (EVDO_A)";
            case 7:
                return "2G (1xRTT)";
            case 8:
                return "3G (HSDPA)";
            case 9:
                return "3G (HSUPA)";
            case 10:
                return "3G (HSPA)";
            case 11:
                return "2G (IDEN)";
            case 12:
                return "2G (EVDO_B)";
            case 13:
                return "4G (LTE)";
            case 14:
                return "2G (EHRPD)";
            case 15:
                return "3G (HSPA+)";
            case 19:
                return "4G (LTE CA)";
            case 20:
                return "5G (NR)";
            case 40:
                return "4G (+5G)";
            case 41:
                return "5G (NR)";
            case 42:
                return "5G (NR)";
            case 97:
                return "CLI";
            case 98:
                return "BROWSER";
            case 99:
                return "WLAN";
            case 101:
                return "2G/3G";
            case 102:
                return "3G/4G";
            case 103:
                return "2G/4G";
            case 104:
                return "2G/3G/4G";
            case 105:
                return "MOBILE";
            case 106:
                return "Ethernet";
            case 107:
                return "Bluetooth";
            default:
                return "UNKNOWN";
        }
    }

    public static String getNatType(final InetAddress localAdr, final InetAddress publicAdr) {
        try {
            final String ipVersionLocal;
            final String ipVersionPublic;
            if (publicAdr instanceof Inet4Address)
                ipVersionPublic = "ipv4";
            else if (publicAdr instanceof Inet6Address)
                ipVersionPublic = "ipv6";
            else
                ipVersionPublic = "ipv?";

            if (localAdr instanceof Inet4Address)
                ipVersionLocal = "ipv4";
            else if (localAdr instanceof Inet6Address)
                ipVersionLocal = "ipv6";
            else
                ipVersionLocal = "ipv?";

            if (localAdr.equals(publicAdr))
                return "no_nat_" + ipVersionPublic;
            else {
                final String localType = isIPLocal(localAdr) ? "local" : "public";
                final String publicType = isIPLocal(publicAdr) ? "local" : "public";
                if (ipVersionLocal.equals(ipVersionPublic)) {
                    return String.format("nat_%s_to_%s_%s", localType, publicType, ipVersionPublic);
                } else {
                    return String.format("nat_%s_to_%s_%s", ipVersionLocal, publicType, ipVersionPublic);
                }
            }
        } catch (final IllegalArgumentException e) {
            return "illegal_ip";
        }
    }

    public static boolean isIPLocal(final InetAddress adr) {
        return adr.isLinkLocalAddress() || adr.isLoopbackAddress() || adr.isSiteLocalAddress();
    }

    public static String IpType(InetAddress inetAddress) {
        try {
            final String ipVersion;
            if (inetAddress instanceof Inet4Address)
                ipVersion = "ipv4";
            else if (inetAddress instanceof Inet6Address)
                ipVersion = "ipv6";
            else
                ipVersion = "ipv?";

            if (inetAddress.isAnyLocalAddress())
                return "wildcard_" + ipVersion;
            if (inetAddress.isSiteLocalAddress())
                return "site_local_" + ipVersion;
            if (inetAddress.isLinkLocalAddress())
                return "link_local_" + ipVersion;
            if (inetAddress.isLoopbackAddress())
                return "loopback_" + ipVersion;
            return "public_" + ipVersion;

        } catch (final IllegalArgumentException e) {
            return "illegal_ip";
        }
    }
}
