package com.abrenoch.hyperiongrabber.tv.fragments.settings

import androidx.leanback.widget.GuidanceStylist
import com.abrenoch.hyperiongrabber.tv.R

class SettingsStepStylist : GuidanceStylist() {
    override fun onProvideLayoutId(): Int {
        return R.layout.settings_guidance
    }
}