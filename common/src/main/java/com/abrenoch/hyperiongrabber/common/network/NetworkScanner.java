package com.abrenoch.hyperiongrabber.common.network;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Scans the local network for running Hyperion servers
 * Created by nino on 27-5-18.
 */

public class NetworkScanner {
    public static final int PORT = 19445;
    /** The amount of milliseconds we try to connect to a given ip before giving up */
    public static final int ATTEMPT_TIMEOUT_MS = 50;

    private final String[] ipsToTry;
    private int lastTriedIndex = -1;


    public NetworkScanner() {

        ipsToTry = getIPsToTry();
    }


    /** Scan the next ip address in the list of addresses to try
     *
     * @return the hostname (or an ip represented as String) when a Hyperion server was found
     */
    @Nullable
    @WorkerThread
    public String tryNext(){

        if (!hasNextAttempt()){
            throw new IllegalStateException("No more ip addresses to try");
        }

        Socket socket = new Socket();
        String ip = ipsToTry[++lastTriedIndex];
        try {
            // timeout after 100ms, should be enough for local network
            socket.connect(new InetSocketAddress(ip, PORT), ATTEMPT_TIMEOUT_MS);

            if (socket.isConnected()){
                socket.close();
                return ip;
            }

        } catch(Exception e) {
            return null;
        }


        return null;

    }

    /** An indication of how many of the total ip's have been tried
     *
     * @return progress in in the range [0.0 .. 1.0]
     */
    public float getProgress() {
        if (ipsToTry.length == 0){
            return 1f;
        }

        return lastTriedIndex / (float)ipsToTry.length;
    }

    /** True if not all up's have been tried yet
     *
     */
    public boolean hasNextAttempt(){
        return ipsToTry.length > 0 && lastTriedIndex + 1 < ipsToTry.length;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or null
     *
     * https://stackoverflow.com/a/13007325
     */
    @Nullable
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // for now eat exceptions
            Log.e("HYPERION SCANNER", "Could not get ip address", e);
        }
        return null;
    }

    private String[] getIPsToTry(){
        try {
            String localIpV4 = getIPAddress(true);

            String[] ipParts = localIpV4.split("\\.");

            String ipPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";
            String[] ipsToTry = new String[254];
            for (int i = 1; i< 255 ; i++){
                ipsToTry[i-1] = ipPrefix + i;
            }

            int localNumberInSubnet = Integer.parseInt(ipParts[3]);

            // sort in such a way that ips close to the local ip will be tried first
            Arrays.sort(ipsToTry, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    int lhsNumberInSubnet = Integer.parseInt(lhs.split("\\.")[3]);
                    int rhsNumberInSubnet = Integer.parseInt(rhs.split("\\.")[3]);
                    int lhsDistance = Math.abs(lhsNumberInSubnet - localNumberInSubnet);
                    int rhsDistance = Math.abs(rhsNumberInSubnet - localNumberInSubnet);
                    return lhsDistance - rhsDistance;
                }
            });
            return ipsToTry;
        } catch (Exception e){
            // for now eat exceptions
            Log.e("HYPERION SCANNER", "Error while building list of subnet ip", e);
            return new String[]{};
        }
    }
}
