package com.abrenoch.hyperiongrabber.mobile;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.abrenoch.hyperiongrabber.common.BootActivity;
import com.abrenoch.hyperiongrabber.common.HyperionScreenService;
import com.abrenoch.hyperiongrabber.common.audio.AudioGradientView;
import com.abrenoch.hyperiongrabber.common.audio.HyperionAudioEncoder;
import com.abrenoch.hyperiongrabber.common.audio.HyperionAudioService;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ImageView.OnClickListener,
        ImageView.OnFocusChangeListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "MainActivity";
    public static final int REQUEST_MEDIA_PROJECTION = 1;
    private boolean mRecorderRunning = false;
    private static MediaProjectionManager mMediaProjectionManager;

    private static final int MY_PERMISSIONS_RECORD_AUDIO = 200;
    static boolean hasPermission = false;
    public int[] mColors;
    AudioGradientView mAudioGradientView;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean checked = intent.getBooleanExtra(HyperionAudioService.BROADCAST_TAG, false);
            mRecorderRunning = checked;
            String error = intent.getStringExtra(HyperionAudioService.BROADCAST_ERROR);
            if (error != null &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                            !HyperionGrabberTileService.isListening())) {
                Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
            }
            setImageViews(checked, checked);
        }
    };

    private BroadcastReceiver mColorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (DEBUG) Log.d(TAG, HyperionAudioService.BROADCAST_COLORS);
            mColors = intent.getIntArrayExtra(HyperionAudioService.BROADCAST_COLORS);
            if (DEBUG) Log.d(TAG, "Colors received: " + Arrays.toString(mColors));

            mAudioGradientView.setColors(mColors);

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // permission for Android 6+
        requestAudioPermissions();

        setContentView(R.layout.activity_main);
        mMediaProjectionManager = (MediaProjectionManager)
                                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        ImageView iv = findViewById(R.id.power_toggle);
        iv.setOnClickListener(this);
        iv.setOnFocusChangeListener(this);
        iv.setFocusable(true);
        iv.requestFocus();

        ImageView next = findViewById(R.id.next_effect);
        next.setOnClickListener(this);
        next.setOnFocusChangeListener(this);
        next.setFocusable(true);
        next.requestFocus();

        ImageView prev = findViewById(R.id.previous_effect);
        prev.setOnClickListener(this);
        prev.setOnFocusChangeListener(this);
        prev.setFocusable(true);
        prev.requestFocus();

        setImageViews(mRecorderRunning, false);
        mAudioGradientView = findViewById(R.id.audioGradientView);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(HyperionAudioService.BROADCAST_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mColorReceiver, new IntentFilter(HyperionAudioService.BROADCAST_X));

        checkForInstance();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.power_toggle){
            if (!mRecorderRunning) {
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                        REQUEST_MEDIA_PROJECTION);
            } else {
                stopScreenRecorder();
            }
            mRecorderRunning = !mRecorderRunning;
        }
        else if(view.getId()==R.id.next_effect){

            if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RAINBOW_SWIRL) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RAINBOW_MOD;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RAINBOW_MOD) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.ICEBLUE;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.ICEBLUE) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RGBWHITE;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RGBWHITE) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.PLASMA;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.PLASMA) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RAINBOW_SWIRL;


        }
        else if(view.getId()==R.id.previous_effect){

            if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.PLASMA) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RGBWHITE;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RGBWHITE) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.ICEBLUE;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.ICEBLUE) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RAINBOW_MOD;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RAINBOW_MOD) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.RAINBOW_SWIRL;
            else if(HyperionAudioEncoder.VISUALIZATION_METHOD==HyperionAudioEncoder.VisualisationMethod.RAINBOW_SWIRL) HyperionAudioEncoder.VISUALIZATION_METHOD=HyperionAudioEncoder.VisualisationMethod.PLASMA;


        }

    }

    @Override
    public void onFocusChange(View view, boolean focused) {
        if (focused) {
            ((ImageView) view).setColorFilter(Color.argb(255, 0, 0, 150));
        } else {
            ((ImageView) view).setColorFilter(Color.argb(255, 0, 0, 0));
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
                setImageViews(false, true);
                return;
            }
            Log.i(TAG, "Starting screen capture");
            startScreenRecorder(resultCode, (Intent) data.clone());

            mRecorderRunning = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent=new Intent(this,SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkForInstance() {
        if (isServiceRunning()) {
            Intent intent = new Intent(this, HyperionAudioService.class);
            intent.setAction(HyperionAudioService.GET_STATUS);
            startService(intent);
        }
    }

    public void startScreenRecorder(int resultCode, Intent data) {
        BootActivity.startScreenRecorder(this, resultCode, data);
    }

    public void stopScreenRecorder() {
        if (mRecorderRunning) {
            Intent intent = new Intent(this, HyperionAudioService.class);
            intent.setAction(HyperionAudioService.ACTION_EXIT);
            startService(intent);
        }
    }

    private void setImageViews(boolean running, boolean animated) {
        //View rainbow = findViewById(R.id.sweepGradientView);
        View audioGradientView = findViewById(R.id.audioGradientView);
        View message = findViewById(R.id.grabberStartedText);
        View buttonImage = findViewById(R.id.power_toggle);
        View buttonNextEffect = findViewById(R.id.next_effect);
        View buttonPrevEffect = findViewById(R.id.previous_effect);

        if (running) {
//            if (animated){
//                fadeView(rainbow, true);
//                fadeView(message, true);
//            } else {
//                rainbow.setVisibility(View.VISIBLE);
//                message.setVisibility(View.VISIBLE);
//            }
            message.setVisibility(View.VISIBLE);
            audioGradientView.setVisibility(View.VISIBLE);
            buttonImage.setAlpha((float) 1);
            buttonNextEffect.setVisibility(View.VISIBLE);
            buttonPrevEffect.setVisibility(View.VISIBLE);
        } else {
//            if (animated){
//                fadeView(rainbow, false);
//                fadeView(message, false);
//            } else {
//                rainbow.setVisibility(View.INVISIBLE);
//                message.setVisibility(View.INVISIBLE);
//            }
            message.setVisibility(View.INVISIBLE);
            audioGradientView.setVisibility(View.INVISIBLE);
            buttonImage.setAlpha((float) 0.25);
            buttonNextEffect.setVisibility(View.INVISIBLE);
            buttonPrevEffect.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (HyperionAudioService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void fadeView(View view, boolean visible){
        float alpha = visible ? 1f : 0f;
        int endVisibility = visible ? View.VISIBLE : View.INVISIBLE;
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(alpha)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(endVisibility);
                    }
                })
                .start();
    }

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true;
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String [] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                hasPermission = true;
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(MainActivity.this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
            }
        }
    }
}
