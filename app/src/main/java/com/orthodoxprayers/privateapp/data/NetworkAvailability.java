package com.orthodoxprayers.privateapp.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Lightweight connectivity hint. The signed HTTPS request remains the source of truth. */
public final class NetworkAvailability {
    private NetworkAvailability() {}

    public static boolean hasConnectedNetwork(Context context) {
        if (context == null) return true;
        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return true;
            Network network = manager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (SecurityException ignored) {
            // ACCESS_NETWORK_STATE is declared, but never block a signed download if an OEM
            // denies the hint unexpectedly.
            return true;
        }
    }
}
