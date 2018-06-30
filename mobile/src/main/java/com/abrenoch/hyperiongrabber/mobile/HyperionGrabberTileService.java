package com.abrenoch.hyperiongrabber.mobile;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;

import com.abrenoch.hyperiongrabber.common.BootActivity;
import com.abrenoch.hyperiongrabber.common.HyperionScreenService;

@RequiresApi(api = Build.VERSION_CODES.N)
public class HyperionGrabberTileService extends TileService {

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Tile tile = getQsTile();
            boolean running = intent.getBooleanExtra(HyperionScreenService.BROADCAST_TAG, false);
            tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(HyperionScreenService.BROADCAST_FILTER));
        if (isServiceRunning()) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.GET_STATUS);
            startService(intent);
        } else {
            Tile tile = getQsTile();
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onStopListening() {
        super.onTileRemoved();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        tile.updateTile();
        int tileState = tile.getState();
        if (tileState == Tile.STATE_ACTIVE) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.ACTION_EXIT);
            startService(intent);
        } else {
            Runnable runner = () -> {
                final Intent i = new Intent(this, BootActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                        |Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        |Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            };
            if (isLocked()) {
                unlockAndRun(runner);
            } else {
                runner.run();
            }
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (HyperionScreenService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
