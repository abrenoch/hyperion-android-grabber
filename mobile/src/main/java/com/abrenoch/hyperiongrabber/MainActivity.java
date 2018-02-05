package com.abrenoch.hyperiongrabber;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;


import java.io.File;

public class MainActivity extends AppCompatActivity implements Switch.OnCheckedChangeListener {

    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final int REQUEST_ACCESS_STORAGE = 2;
    private static final String TAG = "DEBUG";
    private Switch mSwitch;
    private boolean mRecorderRunning = false;
    private MediaProjectionManager mMediaProjectionManager;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwitch = findViewById(R.id.toggle);
        mSwitch.setOnCheckedChangeListener(this);
        mMediaProjectionManager = (MediaProjectionManager)
                                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                                        REQUEST_MEDIA_PROJECTION);
        } else {
            stopScreenRecorder();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                Toast.makeText(this, "USER CANCELLED", Toast.LENGTH_SHORT).show();

                if (mRecorderRunning) {
                    stopScreenRecorder();
                }

                mSwitch.setChecked(false);
                return;
            }

            Log.i(TAG, "Starting screen capture");

            startScreenRecorder(resultCode, data);
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

    public void makeToast(String text) {
        Toast.makeText(this, text,
                Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startScreenRecorder(int resultCode, Intent data) {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.ACTION_START);
            intent.putExtra(HyperionScreenService.EXTRA_RESULT_CODE, resultCode);
            intent.putExtras(data);
            startService(intent);

            Log.i(TAG, "STARTED THE THING!!!!!");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_ACCESS_STORAGE);
        }
    }

    private void startGrabberService() {
//        String path = Environment.getExternalStorageDirectory().getPath() + "/" + this.getPackageName() + "/RecordingScreen";
//        File saveScreenCaptureDirectory = new File(path);
//        if(!saveScreenCaptureDirectory.exists()){
//            saveScreenCaptureDirectory.mkdirs();
//        }
        Intent i = new Intent(this, HyperionScreenService.class);
        i.setAction(HyperionScreenService.ACTION_STOP);
//        i.putExtra(HyperionScreenService.EXTRA_SET_OUTPUT_PATH, path + "/00.mp4") ;
        this.startService(i);
    }

    public void stopScreenRecorder() {
        if (mRecorderRunning) {
            Intent intent = new Intent(this, HyperionScreenService.class);
            intent.setAction(HyperionScreenService.ACTION_STOP);
            startService(intent);
        }
    }

    private boolean serviceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
