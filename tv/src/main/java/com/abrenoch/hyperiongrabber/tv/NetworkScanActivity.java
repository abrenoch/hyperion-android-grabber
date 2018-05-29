package com.abrenoch.hyperiongrabber.tv;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abrenoch.hyperiongrabber.common.util.HyperionScannerTask;

import butterknife.BindView;
import butterknife.ButterKnife;


public class NetworkScanActivity extends LeanbackActivity implements HyperionScannerTask.Listener {
    private boolean isScanning = false;

    @BindView(R.id.startScanButton) Button startScanButton;
    @BindView(R.id.manualSetupButton) Button manualSetupButton;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.scannerDescriptionText) TextView descriptionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_scan);
        ButterKnife.bind(this);

    }

    public void onStartScanClick(View v){
        if (!isScanning){
            new HyperionScannerTask(this).execute();
        }

    }


    @SuppressLint("StringFormatInvalid")
    @Override
    public void onScannerProgress(float progress) {
        if (!isScanning){
            isScanning = true;
            startScanButton.setText(R.string.scanner_scan_in_progress_button);
            descriptionText.setGravity(Gravity.CENTER);
            descriptionText.setText(getString(R.string.scanner_scan_in_progress_text, "\uD83D\uDD75️")); // todo: 🕵️
        }

        progressBar.setProgress(Math.round(progress * 100));
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onScannerCompleted(@Nullable String foundIpAddress) {
        isScanning = false;

        if (foundIpAddress == null){
            startScanButton.setText(R.string.scanner_retry_button);
            manualSetupButton.requestFocus();
            descriptionText.setText(getString(R.string.scanner_no_results, "\uD83D\uDE29")); // 😩
        }
    }

}
