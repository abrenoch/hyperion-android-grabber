package com.abrenoch.hyperiongrabber.tv.wizard;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.InputType;

import com.abrenoch.hyperiongrabber.common.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The second screen (additional settings) of the setup wizard. Allows the user to configure the
 * Priority of the Grabber in Hyperion, Reconnect at connection loss, Capture Rate and the Grabber type
 */

public class AdditionalFragment extends GuidedStepSupportFragment {

    private String ARG_HOST, ARG_PORT, ARG_PRIORITY, ARG_FRAMERATE, ARG_DELAY;
    private boolean ARG_RECONNECT = false, ARG_OPENGL = false;

    private static final int
            ACTION_ID_PRIORITY = 1005,
            ACTION_ID_RECONNECT = 1006,
            ACTION_ID_RECONNECT_STATE = 1007,
            ACTION_ID_RECONNECT_DELAY = 1008,
            ACTION_ID_FRAMERATE = 1009,
            ACTION_ID_GRABBER = 1010,
            ACTION_ID_GRABBER_MEDIA = 1011,
            ACTION_ID_GRABBER_OGL = 1012,
            ACTION_ID_START_FRAMERATE = 2000; // ID 2000 to length pref_list_framerate reserved for framerate values

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ARG_HOST = getArguments().getString("ARG_HOST", getString(R.string.pref_default_host));
        ARG_PORT = getArguments().getString("ARG_PORT", getString(R.string.pref_default_port));
        ARG_PRIORITY = getString(R.string.pref_default_priority);
        ARG_DELAY = getString(R.string.pref_default_reconnect_delay);
        ARG_FRAMERATE = getString(R.string.pref_default_framerate);
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {

        return new GuidanceStylist.Guidance(
                getString(R.string.additional_step_wizard_title),
                getString(R.string.additional_step_wizard_description),
                getString(R.string.additional_step_wizard_breadcrumb),
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();

        // Message Priority
        actions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_PRIORITY)
                .title(getString(R.string.additional_step_priority_title))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .descriptionInputType(InputType.TYPE_CLASS_NUMBER)
                .description(getString(R.string.pref_default_priority))
                .build());

        // Reconnect
        List<GuidedAction> reconnectActions = new ArrayList<>();
        reconnectActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_RECONNECT_STATE)
                .title(getString(R.string.additional_step_reconnect_enable))
                .build());
        reconnectActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_RECONNECT_DELAY)
                .title(getString(R.string.additional_step_reconnect_delay_title))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .descriptionInputType(InputType.TYPE_CLASS_NUMBER)
                .description(getString(R.string.pref_default_reconnect_delay))
                .build());
        actions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_RECONNECT)
                .title(getString(R.string.additional_step_reconnect_title))
                .description(getString(R.string.additional_step_reconnect_description_disabled))
                .subActions(reconnectActions)
                .build());

        // Capture Rate
        List<GuidedAction> framerateActions = new ArrayList<>();
        final String[] framerate = getResources().getStringArray(R.array.pref_list_framerate);
        final String[] framerateValues = getResources().getStringArray(R.array.pref_list_framerate_values);
        for (int i = 0; i < framerate.length; i++) {
            framerateActions.add(new GuidedAction.Builder(context)
                .title(framerate[i])
                .id(ACTION_ID_START_FRAMERATE + i)
                .build());
        }
        for (int i = 0; i < framerateValues.length; i++) {
            if (framerateValues[i].equals(getString(R.string.pref_default_framerate))) {
                actions.add(new GuidedAction.Builder(context)
                        .id(ACTION_ID_FRAMERATE)
                        .title(getString(R.string.additional_step_framerate_title))
                        .description(framerate[i])
                        .subActions(framerateActions)
                        .build());
                break;
            }
        }

        // Grabber
        List<GuidedAction> grabberActions = new ArrayList<>();
        grabberActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_GRABBER_MEDIA)
                .title(getString(R.string.additional_step_grabber_media))
                .build());
        grabberActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_GRABBER_OGL)
                .title(getString(R.string.additional_step_grabber_ogl))
                .build());
        actions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_GRABBER)
                .title(getString(R.string.additional_step_grabber_title))
                .description(getString(R.string.additional_step_grabber_media))
                .subActions(grabberActions)
                .build());
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (action.getId() >= ACTION_ID_START_FRAMERATE && action.getId() <= (ACTION_ID_START_FRAMERATE + getResources().getStringArray(R.array.pref_list_framerate).length)) {
            //TODO: save "pref_list_framerate_values" in ARG_FRAMERATE
            final String[] framerate = getResources().getStringArray(R.array.pref_list_framerate);
            final String[] framerateValues = getResources().getStringArray(R.array.pref_list_framerate_values);

            for (int i = 0; i < framerate.length; i++) {
                if (framerate[i].equals(action.getTitle().toString())) {
                    ARG_FRAMERATE = framerateValues[i];
                    break;
                }
            }

            findActionById(ACTION_ID_FRAMERATE).setDescription(action.getTitle().toString());
            notifyActionChanged(findActionPositionById(ACTION_ID_FRAMERATE));
        } else if (action.getId() == ACTION_ID_GRABBER_MEDIA) {
            ARG_OPENGL = false;
            findActionById(ACTION_ID_GRABBER).setDescription(getString(R.string.additional_step_grabber_media));
            notifyActionChanged(findActionPositionById(ACTION_ID_GRABBER));
        } else if (action.getId() == ACTION_ID_GRABBER_OGL) {
            ARG_OPENGL = true ;
            findActionById(ACTION_ID_GRABBER).setDescription(getString(R.string.additional_step_grabber_ogl));
            notifyActionChanged(findActionPositionById(ACTION_ID_GRABBER));
        } else if (action.getId() == ACTION_ID_RECONNECT_STATE) {
                if (action.getTitle() == getString(R.string.additional_step_reconnect_enable)) {
                    ARG_RECONNECT = true;
                    action.setTitle(getString(R.string.additional_step_reconnect_disable));
                    findActionById(ACTION_ID_RECONNECT).setDescription(getString(R.string.additional_step_reconnect_description_enabled));
                    notifyActionChanged(findActionPositionById(ACTION_ID_RECONNECT));
                } else {
                    ARG_RECONNECT = false;
                    action.setTitle(getString(R.string.additional_step_reconnect_enable));
                    findActionById(ACTION_ID_RECONNECT).setDescription(getString(R.string.additional_step_reconnect_description_disabled));
                    notifyActionChanged(findActionPositionById(ACTION_ID_RECONNECT));
                }
        } else if (action.getId() == ACTION_ID_RECONNECT_DELAY) {
            ARG_DELAY = action.getDescription().toString();
            action.setDescription(ARG_DELAY);
        }

        return super.onSubGuidedActionClicked(action);
    }

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        super.onCreateButtonActions(actions,savedInstanceState);
        actions.add(new GuidedAction.Builder(getContext())
                .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                .editable(false)
                .hasNext(true)
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ID_PRIORITY) {
            ARG_PRIORITY = action.getDescription().toString();
            findActionById(ACTION_ID_PRIORITY).setDescription(ARG_PRIORITY);
            notifyActionChanged(findActionPositionById(ACTION_ID_PRIORITY));
        } else if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {

             Bundle args = getArguments();

            //TODO: Check the arguments to see if they are valid

            args.putString("ARG_HOST", ARG_HOST);
            args.putString("ARG_PORT", ARG_PORT);
            args.putString("ARG_PRIORITY", ARG_PRIORITY);

            if (ARG_RECONNECT)
                args.putString("ARG_DELAY", ARG_DELAY);

            args.putString("ARG_FRAMERATE", ARG_FRAMERATE);

            if (ARG_OPENGL)
                args.putBoolean("ARG_OPENGL", true);

            FinishFragment finishFragment = new FinishFragment();
            finishFragment.setArguments(args);
            GuidedStepSupportFragment.add(getFragmentManager(), finishFragment);
        }
    }
}