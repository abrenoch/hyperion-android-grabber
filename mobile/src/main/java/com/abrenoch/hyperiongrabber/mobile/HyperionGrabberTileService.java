package com.abrenoch.hyperiongrabber.mobile;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.abrenoch.hyperiongrabber.common.BootActivity;
import com.abrenoch.hyperiongrabber.common.HyperionScreenService;
import com.abrenoch.hyperiongrabber.common.util.Preferences;

@RequiresApi(api = Build.VERSION_CODES.N)
public class HyperionGrabberTileService extends TileService {
    private final int REMOVE_LISTENER_DELAY = 1000 * 10; // 10 second delay to remove listener
    private static boolean mIsListening = false;
    private Handler mHandle = new Handler();

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Tile tile = getQsTile();
            boolean running = intent.getBooleanExtra(HyperionScreenService.BROADCAST_TAG, false);
            String error = intent.getStringExtra(HyperionScreenService.BROADCAST_ERROR);
            tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
            if (error != null) {
                Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        mHandle.removeCallbacksAndMessages(null);
        if (!mIsListening) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mMessageReceiver, new IntentFilter(HyperionScreenService.BROADCAST_FILTER));
            mIsListening = true;
        }
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
        mHandle.postDelayed(unregisterReceiverRunner, REMOVE_LISTENER_DELAY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsListening = false;
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

                boolean setupStarted = startSetupIfNeeded();

                if (!setupStarted){
                    final Intent i = new Intent(this, BootActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                            |Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            |Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivityAndCollapse(i);
                }

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

    private Runnable unregisterReceiverRunner = () -> {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        mIsListening = false;
    };

    public static boolean isListening() { return mIsListening; }

    /** Starts the Settings Activity if connection settings are missing
     *
     * @return true if setup was started
     */
    private boolean startSetupIfNeeded(){
        Preferences preferences = new Preferences(getApplicationContext());
        if (TextUtils.isEmpty(preferences.getString(R.string.pref_key_host, null)) || preferences.getInt(R.string.pref_key_port, -1) == -1){
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.putExtra(SettingsActivity.EXTRA_SHOW_TOAST_KEY, SettingsActivity.EXTRA_SHOW_TOAST_SETUP_REQUIRED_FOR_QUICK_TILE);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Use TaskStackBuilder to make sure the MainActivity opens when the SettingsActivity is closed
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(settingsIntent)
                    .startActivities();

            Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeIntent);

            return true;
        }

        return false;
    }
}
