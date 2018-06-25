package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.support.v17.leanback.widget.GuidanceStylist
import com.abrenoch.hyperiongrabber.tv.R

class SettingsStepStylist : GuidanceStylist() {
    override fun onProvideLayoutId(): Int {
        return R.layout.settings_guidance
    }
}