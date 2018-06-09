package com.abrenoch.hyperiongrabber.tv.wizard;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import com.abrenoch.hyperiongrabber.common.R;

import java.util.List;

/**
 * The first run of the connection wizard explains the meaning of the wizard.
 * Only displayed if Hyperion Grabber is running for the first time.
 */

public class FirstRunFragment extends GuidedStepSupportFragment {

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(
                getString(R.string.first_run_wizard_title),
                getString(R.string.first_run_wizard_description),
                getString(R.string.first_run_wizard_breadcrumb), null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                .editable(false)
                .hasNext(true)
                .build());
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_CANCEL)
                .editable(false)
                .hasNext(false)
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
            GuidedStepSupportFragment.add(getFragmentManager(), new FirstStepFragment());
        } else getActivity().finish();
    }
}