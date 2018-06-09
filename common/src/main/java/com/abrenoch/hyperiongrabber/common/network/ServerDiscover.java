package com.abrenoch.hyperiongrabber.common.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerDiscover {

    private static final String TAG = "ServerDiscover";
    private static final String SERVICE_TYPE = "_hyperiond-proto._tcp.";

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener = null;
    private NsdManager.DiscoveryListener mDiscoveryListener = null;
    private boolean isDiscovering = false;

    private List<Server> serverList;
    private Set<String> serverDiscovered;

    public ServerDiscover(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        serverList = new ArrayList<>();
        serverDiscovered = new HashSet<>();
    }

    public void discoverServices(final ServerDiscover.Callback callback) {

        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getHost() != null) {
                    Server hyperion = new Server(serviceInfo);
                    serverList.add(hyperion);
                }

                if (callback != null) {
                    callback.OnServerListChanged(serverList);
                }
            }
        };

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if (serverDiscovered.contains(service.getServiceName())) {
                    Log.d(TAG, "Service already discovered");
                } else if (service.getServiceType().equals(SERVICE_TYPE)) {
                    serverDiscovered.add(service.getServiceName());
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "service lost" + service);

                for (int i = 0; i < serverList.size(); i++) {
                    if (service.getServiceName().contains(serverList.get(i).getName())) {
                        serverList.remove(i);
                        break;
                    }
                }

                serverDiscovered.remove(service.getServiceName());

                if (callback != null) {
                    callback.OnServerListChanged(serverList);
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Service discovery stopped");
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    public void discoverServices() {
        serverList.clear();
        serverDiscovered.clear();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        isDiscovering = true;
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        isDiscovering = false;
    }

    public boolean isDiscovering() {
        return isDiscovering;
    }

    public List<Server> getServerList() {
        return serverList;
    }

    public interface Callback {
        void OnServerListChanged(List<Server> serverList);
    }

    public static class Server {
        private String port;
        private String name;
        private String address;

        private Server(NsdServiceInfo serviceInfo) {

            this.port = Integer.toString(serviceInfo.getPort());
            this.name = serviceInfo.getServiceName();
            this.address = serviceInfo.getHost().getHostAddress();
        }

        public String getPort() {
            return port;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }
    }
}