package com.abrenoch.hyperiongrabber.tv.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v4.app.FragmentActivity;

import com.abrenoch.hyperiongrabber.common.R;

/**
 * An FragmentActivity that displays a step by step wizard to configure the
 * network connection between the Hyperion server and the android grabber.
 */
public class WizardActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Set background picture
        //getWindow().setBackgroundDrawableResource(R.drawable.wizard_background);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean firstRun = prefs.getBoolean(getString(R.string.wizard_previously_started), false);

        GuidedStepSupportFragment fragment;
        if(!firstRun) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.wizard_previously_started), true);
            edit.apply();
            fragment = new FirstRunFragment();
        } else fragment = new FirstStepFragment();

        fragment.setArguments(getIntent().getExtras());
        GuidedStepSupportFragment.addAsRoot(this, fragment, android.R.id.content);
    }

    @Override
    public void onBackPressed() {
        if (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(getSupportFragmentManager()) instanceof FinishFragment) {
            finish();
        } else if (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(getSupportFragmentManager()) instanceof FirstStepFragment) {
            finish();
        } else super.onBackPressed();
    }
}