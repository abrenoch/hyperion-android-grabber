package com.abrenoch.hyperiongrabber.tv.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.abrenoch.hyperiongrabber.common.network.Hyperion;
import com.abrenoch.hyperiongrabber.common.util.Preferences;
import com.abrenoch.hyperiongrabber.tv.R;

import java.io.IOException;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class ScanResultActivity extends LeanbackActivity {
    public static final String EXTRA_RESULT_HOST_NAME = "EXTRA_RESULT_HOST_NAME";
    public static final String EXTRA_RESULT_PORT = "EXTRA_RESULT_PORT";


    @BindView(R.id.konfetti) KonfettiView konfettiView;
    @BindView(R.id.scanResultDescriptionText) TextView descriptionText;
    @BindView(R.id.scanResultHostName) TextView hostNameText;
    @BindView(R.id.scanResultEmojiText) TextView emojiText;
    private String hostName;
    private int port;

    private final int ORANGE = Color.rgb(253, 104, 40);
    private final int[] COLORS = {Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, ORANGE};
    /** the amount of variation in R, G, B components of a confetti burst */
    private final int BURST_COLOR_SPREAD = 200;


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
            port = Integer.parseInt(extras.getString(EXTRA_RESULT_PORT));
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

    @Override
    protected void onPause() {
        super.onPause();
        setHyperionColor(hostName, port, -1);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setHyperionColor(hostName, port, Color.MAGENTA);
    }

    public void onClick(View v){
        if (v.getId() == R.id.confirmButton){
            saveResult();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // call this to finish the current activity
        } else if (v.getId() == R.id.changeColorButton){
            int color = COLORS[new Random().nextInt(COLORS.length)];
            setHyperionColor(hostName, port, color);
            burstKonfetti(color);
        }
    }

    /** Save scan result to Preferences */
    private void saveResult() {
        Preferences prefs = new Preferences(getApplicationContext());
        prefs.putString(R.string.pref_key_host, hostName);
        prefs.putInt(R.string.pref_key_port, port);
    }

    private void startKonfetti() {
        konfettiView.build()
                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(400L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(new Size(12, 5))
                .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                .streamFor(300, Long.MAX_VALUE);
    }

    /** Start a one-shot burst of confetti from a random location of a single color
     *
      * @param color a color int such as Color.RED, NOT a color resource
     */
    private void burstKonfetti(int color){
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        Random random = new Random();
        int[] variations = new int[10];
        int halfSpread = BURST_COLOR_SPREAD / 2;
        for (int i = 0; i < variations.length; i++) {
            variations[i] = Color.rgb(
                    colorComponentClamp(r - halfSpread + random.nextInt(BURST_COLOR_SPREAD)),
                    colorComponentClamp(g - halfSpread + random.nextInt(BURST_COLOR_SPREAD)),
                    colorComponentClamp(b - halfSpread + random.nextInt(BURST_COLOR_SPREAD))
            );
        }

        float posX = (float) (Math.random() * konfettiView.getWidth());
        float posY = (float) (Math.random() * konfettiView.getHeight());

        konfettiView.build()
                .addColors(variations)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(400L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(new Size(12, 5))
                .setPosition(posX, posY)
                .burst(100);
    }

    /** A color int , or -1 to clear */
    private void setHyperionColor(String hostName, int port, int color){
        new Thread(() -> {
            try {
                Hyperion hyperion = new Hyperion(hostName, port);
                if (hyperion.isConnected()){
                    if (color == -1){
                        hyperion.clear(50);
                    } else {
                        hyperion.setColor(color, 50);
                    }
                }
                hyperion.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** [0 -255] */
    private static int colorComponentClamp(int value){
        return Math.min(255, Math.max(0, value));
    }

}
