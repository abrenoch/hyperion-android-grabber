package com.abrenoch.hyperiongrabber;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends LeanbackActivity implements ImageView.OnClickListener,
        ImageView.OnFocusChangeListener {
    public static final int REQUEST_MEDIA_PROJECTION = 1;
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
            String error = intent.getStringExtra(BROADCAST_ERROR);
            if (error != null) {
                Toast.makeText(getBaseContext(), error, Toast.LENGTH_SHORT).show();
            }
            setImageViews(checked);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mMediaProjectionManager = (MediaProjectionManager)
                                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        ImageView iv = findViewById(R.id.imageView_button);
        iv.setOnClickListener(this);
        iv.setOnFocusChangeListener(this);
        iv.setFocusable(true);
        iv.requestFocus();

        ImageButton ib = findViewById(R.id.settingsButton);
        ib.setOnClickListener(this);
        ib.setOnFocusChangeListener(this);
        ib.setFocusable(true);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(BROADCAST_FILTER));
        checkForInstance();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageView_button:
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
            case R.id.imageView_button:
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
        FadingImageView bottomImage = findViewById(R.id.imageView_lights);
        ImageView buttonImage = findViewById(R.id.imageView_button);
        if (running) {
            buttonImage.setAlpha((float) 1);
            bottomImage.setVisibility(View.VISIBLE);
        } else {
            buttonImage.setAlpha((float) 0.25);
            bottomImage.setVisibility(View.INVISIBLE);
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
