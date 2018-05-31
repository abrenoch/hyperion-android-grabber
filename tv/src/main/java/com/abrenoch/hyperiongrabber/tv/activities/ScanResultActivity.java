package com.abrenoch.hyperiongrabber.tv.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.abrenoch.hyperiongrabber.tv.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;

public class ScanResultActivity extends LeanbackActivity {
    @BindView(R.id.konfetti) KonfettiView konfettiView;
    @BindView(R.id.scanResultDescriptionText) TextView descriptionText;
    @BindView(R.id.scanResultHostName) TextView hostNameText;
    @BindView(R.id.scanResultEmojiText) TextView emojiText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);
        ButterKnife.bind(this);

        emojiText.setText("\uD83D\uDC4F"); // 👏
        descriptionText.setText(getResources().getString(R.string.scan_result_description, "\uD83C\uDF89"));
        hostNameText.setText("192.168.100.104");

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

    }
}
