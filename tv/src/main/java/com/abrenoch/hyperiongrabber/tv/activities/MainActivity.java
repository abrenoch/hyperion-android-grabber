package com.abrenoch.hyperiongrabber.tv.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.abrenoch.hyperiongrabber.common.HyperionScreenService;
import com.abrenoch.hyperiongrabber.common.util.Preferences;
import com.abrenoch.hyperiongrabber.tv.R;
import com.abrenoch.hyperiongrabber.tv.SettingsActivity;

public class MainActivity extends LeanbackActivity implements ImageView.OnClickListener,
        ImageView.OnFocusChangeListener {
    public static final int REQUEST_MEDIA_PROJECTION = 1;
    public static final int REQUEST_INITIAL_SETUP = 2;
    public static final String BROADCAST_ERROR = "SERVICE_ERROR";
    public static final String BROADCAST_TAG = "SERVICE_STATUS";
    public static final String BROADCAST_FILTER = "SERVICE_FILTER";
    private static final String TAG = "DEBUG";
    private boolean mRecorderRunning = false;
    private static MediaProjectionManager mMediaProjectionManager;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean checked = intent.getBooleanExtra(BROADCAST_TAG, false);
            mRecorderRunning = checked;
            String error = intent.getStringExtra(BROADCAST_ERROR);
            if (error != null) {
                Toast.makeText(getBaseContext(), error, Toast.LENGTH_SHORT).show();
            } else if (checked){
                Toast.makeText(getBaseContext(), getResources().getText(R.string.toast_service_started), Toast.LENGTH_LONG).show();
            }
            setImageViews(checked);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!initIfConfigured()){
            startSetup();
        }


    }

    /** @return whether the activity was initialized */
    private boolean initIfConfigured() {
        // Do we have a valid server config?
        Preferences preferences = new Preferences(getApplicationContext());
        String host = preferences.getString(R.string.pref_key_hyperion_host, null);
        String port = preferences.getString(R.string.pref_key_hyperion_port, null);

        if (host == null || port == null){
            return false;
        }

        initActivity();
        return true;
    }

    private void startSetup() {
        // Start onboarding (setup)
        Intent intent = new Intent(this, NetworkScanActivity.class);
        startActivityForResult(intent, REQUEST_INITIAL_SETUP);
    }

    // Prepare activity for display
    private void initActivity() {
        // assume the recorder is not running until we are notified otherwise
        mRecorderRunning = false;

        setContentView(R.layout.activity_main);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mMediaProjectionManager = (MediaProjectionManager)
                                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        ImageView iv = findViewById(R.id.power_toggle);
        iv.setOnClickListener(this);
        iv.setOnFocusChangeListener(this);
        iv.setFocusable(true);
        iv.requestFocus();

        ImageButton ib = findViewById(R.id.settingsButton);
        ib.setOnClickListener(this);
        ib.setOnFocusChangeListener(this);
        ib.setFocusable(true);

        setImageViews(mRecorderRunning);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(BROADCAST_FILTER));

        // request an update on the running status
        checkForInstance();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.power_toggle:
                if (!mRecorderRunning) {
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                            REQUEST_MEDIA_PROJECTION);
                } else {
                    stopScreenRecorder();
                }
                mRecorderRunning = !mRecorderRunning;
                break;
            case R.id.settingsButton:
                startSettings();
                break;
        }
    }

    @Override
    public void onFocusChange(View view, boolean focused) {
        int clr = Color.argb(255, 0, 0, 150);
        if (!focused) {
            clr = Color.argb(255, 0, 0, 0);
        }
        switch (view.getId()) {
            case R.id.power_toggle:
                ((ImageView) view).setColorFilter(clr);
                break;
            case R.id.settingsButton:
                ((ImageButton) view).setColorFilter(clr);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INITIAL_SETUP){
            if (resultCode == RESULT_OK){
                if (!initIfConfigured()){
                    startSetup();
                }
            } else {
                finish();
            }

            return;
        }
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, R.string.toast_must_give_permission, Toast.LENGTH_SHORT).show();
                if (mRecorderRunning) {
                    stopScreenRecorder();
                }
                setImageViews(false);
                return;
            }
            Log.i(TAG, "Starting screen capture");
            startScreenRecorder(resultCode, (Intent) data.clone());
            mRecorderRunning = true;
        }
    }

    private void startSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        Bundle bundle =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                        .toBundle();
        startActivity(intent, bundle);
    }

    private void checkForInstance() {
        if (isServiceRunning()) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.GET_STATUS);
            startService(intent);
        }
    }

    private void setImageViews(boolean running) {
        View rainbow = findViewById(R.id.sweepGradientView);
        if (running) {
            rainbow.setVisibility(View.VISIBLE);
        } else {
            rainbow.setVisibility(View.INVISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startScreenRecorder(int resultCode, Intent data) {
        Intent intent = new Intent(this, HyperionScreenService.class);
        intent.setAction(HyperionScreenService.ACTION_START);
        intent.putExtra(HyperionScreenService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    public void stopScreenRecorder() {
        if (mRecorderRunning) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.ACTION_EXIT);
            startService(intent);
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
