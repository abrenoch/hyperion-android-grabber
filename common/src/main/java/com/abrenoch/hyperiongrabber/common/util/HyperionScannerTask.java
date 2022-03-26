package com.abrenoch.hyperiongrabber.common.util;

import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.network.NetworkScanner;

import java.lang.ref.WeakReference;


/**
 * Starts a network scan for a running Hyperion server
 * and posts progress and results
 */
public class HyperionScannerTask extends AsyncTask<Void, Float, String> {
    private WeakReference<Listener> weakListener;

    public HyperionScannerTask(Listener listener){
        weakListener = new WeakReference<>(listener);
    }

    @Override
    protected String doInBackground(Void... voids) {
        Log.d("Hyperion scanner", "starting scan");
        NetworkScanner networkScanner = new NetworkScanner();

        String result;
        while (networkScanner.hasNextAttempt()){
            result = networkScanner.tryNext();

            if (result != null){
                return result;
            }

            publishProgress(networkScanner.getProgress());
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        Log.d("Hyperion scanner", "scan progress: " + values[0]);
        if(weakListener.get() != null){
            weakListener.get().onScannerProgress(values[0]);
        }

    }

    @Override
    protected void onPostExecute(String result) {
        Log.d("Hyperion scanner", "scan result: " + result);
        if(weakListener.get() != null){
            weakListener.get().onScannerCompleted(result);
        }
    }

    public interface Listener {

        void onScannerProgress(float progress);
        void onScannerCompleted(@Nullable String foundIpAddress);

    }
}