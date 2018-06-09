package com.abrenoch.hyperiongrabber.tv.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import com.abrenoch.hyperiongrabber.common.R;

import java.util.List;

public class FinishFragment extends GuidedStepSupportFragment {

    private String ARG_HOST, ARG_PORT, ARG_PRIORITY, ARG_FRAMERATE, ARG_DELAY;
    private boolean ARG_OPENGL = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ARG_HOST = getArguments().getString("ARG_HOST", getString(R.string.pref_default_host));
        ARG_PORT = getArguments().getString("ARG_PORT", getString(R.string.pref_default_port));
        ARG_PRIORITY = getArguments().getString("ARG_PRIORITY", getString(R.string.pref_default_priority));
        ARG_DELAY = getArguments().getString("ARG_DELAY", "-1");
        ARG_FRAMERATE = getArguments().getString("ARG_FRAMERATE", getString(R.string.pref_default_framerate));
        ARG_OPENGL = getArguments().getBoolean("ARG_OPENGL", false);

        super.onCreate(savedInstanceState);
    }
    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {

        return new GuidanceStylist.Guidance(
                getString(R.string.last_step_title),
                getString(R.string.last_step_description),
                getString(R.string.last_step_breadcrumb),
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getActivity())
                .clickAction(GuidedAction.ACTION_ID_FINISH)
                .editable(false)
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("hyperion_host", ARG_HOST);
        editor.putString("hyperion_port", ARG_PORT);
        editor.putString("hyperion_priority", ARG_PRIORITY);

        if (!ARG_DELAY.equals("-1")) {
            editor.putBoolean("reconnect", true);
            editor.putString("delay", ARG_DELAY);
        } else {
            editor.putBoolean("reconnect", false);
            editor.putString("delay", getString(R.string.pref_default_reconnect_delay));
        }

        editor.putString("hyperion_framerate", ARG_FRAMERATE);
        editor.putBoolean("ogl_grabber", ARG_OPENGL);
        editor.apply();
        getActivity().finish();
    }
}