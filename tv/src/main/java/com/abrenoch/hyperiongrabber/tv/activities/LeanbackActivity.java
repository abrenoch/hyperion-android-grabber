package com.abrenoch.hyperiongrabber.tv.activities;

import android.support.v4.app.FragmentActivity;

public abstract class LeanbackActivity extends FragmentActivity {
    @Override
    public boolean onSearchRequested() {
        return false;
    }
}
