package com.abrenoch.hyperiongrabber.common.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Scans the local network for running Hyperion servers
 * Created by nino on 27-5-18.
 */

public class NetworkScanner {
    public static final int PORT = 19445;
    /** The amount of milliseconds we try to connect to a given ip before giving up */
    private static final int ATTEMPT_TIMEOUT_MS = 50;

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

    /** True if not all ip's have been tried yet
     *
     */
    public boolean hasNextAttempt(){
        return ipsToTry.length > 0 && lastTriedIndex + 1 < ipsToTry.length;
    }

    /**
     * Get IP addresses for non-localhost interfaces
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  a list of found addresses (may be empty)
     *
     * https://stackoverflow.com/a/13007325
     */
    @NonNull
    private static List<String> getIPAddresses(boolean useIPv4) {
        List<String> foundAddresses = new ArrayList<>();
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
                                foundAddresses.add(sAddr);
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                String v6Addr = delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                                foundAddresses.add(v6Addr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // for now eat exceptions
            Log.e("HYPERION SCANNER", "Could not get ip address", e);
        }
        return foundAddresses;
    }

    private String[] getIPsToTry(){
        try {
            List<String> localIpV4Addresses = getIPAddresses(true);
            String[] allIpsToTry = new String[localIpV4Addresses.size() * 254];

            for (int localIpIdx = 0; localIpIdx < localIpV4Addresses.size(); localIpIdx++) {
                String localIpV4Address = localIpV4Addresses.get(localIpIdx);
                String[] ipsToTry = new String[254];

                String[] ipParts = localIpV4Address.split("\\.");

                String ipPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";
                for (int i = 1; i< 255 ; i++){
                    ipsToTry[i-1] = ipPrefix + i;
                }

                int localNumberInSubnet = Integer.parseInt(ipParts[3]);

                // sort in such a way that ips close to the local ip will be tried first
                Arrays.sort(ipsToTry, (lhs, rhs) -> {
                    int lhsNumberInSubnet = Integer.parseInt(lhs.split("\\.")[3]);
                    int rhsNumberInSubnet = Integer.parseInt(rhs.split("\\.")[3]);
                    int lhsDistance = Math.abs(lhsNumberInSubnet - localNumberInSubnet);
                    int rhsDistance = Math.abs(rhsNumberInSubnet - localNumberInSubnet);
                    return lhsDistance - rhsDistance;
                });

                for (int i = 0; i < ipsToTry.length; i++) {
                    // interleave with previously found ip addresses
                    int allIndex = (localIpV4Addresses.size() * i) + localIpIdx;
                    allIpsToTry[allIndex] = ipsToTry[i];
                }


            }


            return allIpsToTry;
        } catch (Exception e){
            // for now eat exceptions
            Log.e("HYPERION SCANNER", "Error while building list of subnet ip's", e);
            return new String[]{};
        }
    }
}
