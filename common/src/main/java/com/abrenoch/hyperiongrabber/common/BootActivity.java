package com.abrenoch.hyperiongrabber.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import com.abrenoch.hyperiongrabber.common.audio.HyperionAudioService;

public class BootActivity extends AppCompatActivity {
    public static final int REQUEST_MEDIA_PROJECTION = 1;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager manager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager != null) {
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        } else {
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                startScreenRecorder(this, resultCode, data);
            }
            finish();
        }
    }

    public static void startScreenRecorder(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, HyperionAudioService.class);
        intent.setAction(HyperionAudioService.ACTION_START);
        intent.putExtra(HyperionAudioService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
