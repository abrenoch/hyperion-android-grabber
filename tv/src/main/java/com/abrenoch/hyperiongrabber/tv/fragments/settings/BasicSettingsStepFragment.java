package com.abrenoch.hyperiongrabber.tv.fragments.settings;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;

import com.abrenoch.hyperiongrabber.common.util.Preferences;
import com.abrenoch.hyperiongrabber.tv.R;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicSettingsStepFragment extends SettingsStepBaseFragment {
    private static final long ACTION_HOST_NAME = 100L;
    private static final long ACTION_PORT = 110L;
    private static final long ACTION_RECONNECT = 120L;
    private static final long ACTION_RECONNECT_DELAY = 130L;
    private static final long ACTION_MESSAGE_PRIORITY = 140L;
    private static final long ACTION_CAPTURE_RATE = 150L;
    private static final int ACTION_CAPTURE_RATE_SET_ID = 1500;

    private Preferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getPreferences();
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Example_Leanback_GuidedStep_First;
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.guidedstep_basic_settings_title);
        String description = getString(R.string.guidedstep_basic_settings_description);
        String breadCrumb = getString(R.string.guidedstep_basic_settings_breadcrumb);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_settings_ethernet_white_128dp);
        return new GuidanceStylist.Guidance(title, description, breadCrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String desc = getResources().getString(R.string.guidedstep_action_description);
        GuidedAction stepInfo = new GuidedAction.Builder(getContext())
                .title(getResources().getString(R.string.guidedstep_action_title))
                .description(desc)
                .multilineDescription(true)
                .focusable(false)
                .infoOnly(true)
                .enabled(false)
                .build();

        GuidedAction enterHost = unSignedNumberAction(
                ACTION_HOST_NAME,
                getString(R.string.pref_title_host),
                getPreferences().getString(R.string.pref_key_hyperion_host, null)
        );
        GuidedAction enterPort = unSignedNumberAction(
                ACTION_PORT,
                getString(R.string.pref_title_port),
                getPreferences().getString(R.string.pref_key_hyperion_port, "19445")
        );
        GuidedAction priority = unSignedNumberAction(
                ACTION_MESSAGE_PRIORITY,
                getString(R.string.pref_title_priority),
                getPreferences().getString(R.string.pref_key_hyperion_priority, "50")
        );

        GuidedAction reconnect = new GuidedAction.Builder(getContext())
                .id(ACTION_RECONNECT)
                .title(getString(R.string.pref_title_reconnect))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(false)
                .build();

        GuidedAction reconnectDelay = unSignedNumberAction(
                ACTION_RECONNECT_DELAY,
                getString(R.string.pref_title_reconnect_delay),
                getPreferences().getString(R.string.pref_key_reconnect_delay, "5")
        );

        String[] frameRateOptions = getResources().getStringArray(R.array.pref_list_framerate_values);

        GuidedAction captureRate = radioListAction(
                ACTION_CAPTURE_RATE,
                getString(R.string.pref_title_framerate),
                getString(R.string.pref_summary_framerate),
                ACTION_CAPTURE_RATE_SET_ID,
                frameRateOptions,
                getPreferences().getString(R.string.pref_key_hyperion_framerate, "30")

        );

        actions.add(stepInfo);
        actions.add(enterHost);
        actions.add(enterPort);
        actions.add(priority);
        actions.add(reconnect);
        actions.add(reconnectDelay);
        actions.add(captureRate);



    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if ((action.getDescriptionEditInputType() & InputType.TYPE_CLASS_NUMBER) != 0){
            CharSequence description = action.getDescription();
            if (description != null){
                action.setDescription(description.toString().replaceAll("[^\\d]", ""));
            }
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(continueAction());
        actions.add(backAction());
    }

    @Override
    public void onGuidedActionClicked(@NonNull GuidedAction action) {
        if (action.getId() == SettingsStepBaseFragment.CONTINUE) {

            try {
                String host = assertValue(ACTION_HOST_NAME);
                String port = assertValue(ACTION_PORT);
                String frameRate = assertSubActionValue(ACTION_CAPTURE_RATE);

                prefs.putString(R.string.pref_key_hyperion_host, host);
                prefs.putString(R.string.pref_key_hyperion_port, port);
                prefs.putString(R.string.pref_key_hyperion_framerate, frameRate);

                FragmentActivity activity = getActivity();
                activity.setResult(Activity.RESULT_OK);
                finishGuidedStepSupportFragments();

            } catch (AssertionError ignored) {}

            return;

        }

        super.onGuidedActionClicked(action);
    }


}
