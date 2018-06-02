package com.abrenoch.hyperiongrabber.tv.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.abrenoch.hyperiongrabber.tv.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;

public class ScanResultActivity extends LeanbackActivity {
    public static final String EXTRA_RESULT_HOST_NAME = "EXTRA_RESULT_HOST_NAME";
    public static final String EXTRA_RESULT_PORT = "EXTRA_RESULT_PORT";


    @BindView(R.id.konfetti) KonfettiView konfettiView;
    @BindView(R.id.scanResultDescriptionText) TextView descriptionText;
    @BindView(R.id.scanResultHostName) TextView hostNameText;
    @BindView(R.id.scanResultEmojiText) TextView emojiText;
    private String hostName;
    private String port;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);
        ButterKnife.bind(this);

        emojiText.setText("\uD83D\uDC4F"); // 👏
        descriptionText.setText(getResources().getString(R.string.scan_result_description, "\uD83C\uDF89"));
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            hostName = extras.getString(EXTRA_RESULT_HOST_NAME);
            port = extras.getString(EXTRA_RESULT_PORT);
            hostNameText.setText(hostName);
        } else {
            hostNameText.setText(R.string.error_no_host_name_extra);
        }

        Animator fadeInAnimator = ObjectAnimator.ofFloat(hostNameText, "alpha", 0f, 1f);
        Animator scaleXAnimator = ObjectAnimator
                .ofFloat(hostNameText, "scaleX", .8f, 1f);
        Animator scaleYAnimator = ObjectAnimator
                .ofFloat(hostNameText, "scaleY", .8f, 1f);
        AnimatorSet hostNameSet = new AnimatorSet();
        hostNameSet.playTogether(fadeInAnimator, scaleXAnimator, scaleYAnimator);
        hostNameSet.setInterpolator(new DecelerateInterpolator());
        hostNameSet.setDuration(6000L);
        hostNameSet.start();

        konfettiView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                startKonfetti();
                konfettiView.removeOnLayoutChangeListener(this);
            }
        });



    }



    private void startKonfetti() {
        konfettiView.build()
                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(400L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(new nl.dionsegijn.konfetti.models.Size(12, 5))
                .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                .streamFor(300, Long.MAX_VALUE);
    }

    public void onClick(View v){
        if (v.getId() == R.id.confirmButton){
            saveResult();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // call this to finish the current activity
            return;
        }
    }

    /** Save scan result to SharedPreferences */
    private void saveResult() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("hyperion_host", hostName);
        editor.putString("hyperion_port", port);
        editor.apply();
    }
}
