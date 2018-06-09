package com.abrenoch.hyperiongrabber.tv.wizard;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepSupportFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Toast;

import com.abrenoch.hyperiongrabber.common.R;
import com.abrenoch.hyperiongrabber.common.network.ServerDiscover;

import java.util.ArrayList;
import java.util.List;

/**
 * The first step (not first run) of the setup wizard. Gives the user the option,
 * to select the server in the automatic found list (searched via Bonjour Service)
 * or to manually enter the connection information (IP and Port).
 */

public class FirstStepFragment extends GuidedStepSupportFragment {

    private ServerDiscover mServerDiscover = null;
    private static final int ACTION_ID_MANUALLY = -10;
    private static final int ACTION_ID_MANUALLY_HOST = 999;
    private static final int ACTION_ID_MANUALLY_PORT = 1000;
    private int index;

    private static List<GuidedAction> makeDropDownActions(Context context){
        List<GuidedAction> manualActions = new ArrayList<>();
        manualActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_MANUALLY_HOST)
                .title(context.getString(R.string.first_step_title_ip))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                .descriptionInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                .description(context.getString(R.string.pref_default_host))
                .build());

        manualActions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_MANUALLY_PORT)
                .title(context.getString(R.string.first_step_title_port))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .descriptionInputType(InputType.TYPE_CLASS_NUMBER)
                .description(context.getString(R.string.pref_default_port))
                .build());

        manualActions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                .editable(false)
                .hasNext(true)
                .build());

        return manualActions;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mServerDiscover != null && !mServerDiscover.isDiscovering()) {
            mServerDiscover.discoverServices();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mServerDiscover != null && mServerDiscover.isDiscovering()) {
            mServerDiscover.stopDiscovery();
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mServerDiscover != null && mServerDiscover.isDiscovering()) {
            mServerDiscover.stopDiscovery();
            mServerDiscover = null;
        }
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(
                getString(R.string.first_step_wizard_title),
                getString(R.string.first_step_wizard_description),
                getString(R.string.first_step_wizard_breadcrumb), null);
    }

    @Override
    public void onCreateActions(@NonNull final List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();
        mServerDiscover = new ServerDiscover(getActivity());

        mServerDiscover.discoverServices(new ServerDiscover.Callback() {
            @Override
            public synchronized void OnServerListChanged (final List<ServerDiscover.Server> serverList) {

                if(getActivity() != null) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actions.clear();
                            setActions(actions);
                            getGuidedActionsStylist().getActionsGridView().getAdapter().notifyDataSetChanged();
                        }
                    });

                    index = 0;
                    for (ServerDiscover.Server server: serverList) {
                        final String serverName = server.getName();
                        final String ipAddress = server.getAddress();
                        final String portNumber = server.getPort();

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                GuidedAction action = new GuidedAction.Builder(getActivity())
                                        .id(index)
                                        .title(serverName)
                                        .description(ipAddress + ":" + portNumber)
                                        .hasNext(true)
                                        .build();
                                actions.add(action);

                                setActions(actions);
                                getGuidedActionsStylist().getActionsGridView().getAdapter().notifyDataSetChanged();
                                index++;
                            }
                        });
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actions.add(new GuidedAction.Builder(context)
                                    .id(ACTION_ID_MANUALLY)
                                    .title(getString(R.string.first_step_title_manually))
                                    .subActions(makeDropDownActions(context))
                                    .build());

                            setActions(actions);
                            getGuidedActionsStylist().getActionsGridView().getAdapter().notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        mServerDiscover.discoverServices();

        actions.add(new GuidedAction.Builder(context)
                .id(ACTION_ID_MANUALLY)
                .title(getString(R.string.first_step_title_manually))
                .subActions(makeDropDownActions(context))
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);

        if (action.getId() != ACTION_ID_MANUALLY) {
            if (mServerDiscover != null && mServerDiscover.isDiscovering() && !mServerDiscover.getServerList().isEmpty()) {
                mServerDiscover.stopDiscovery();
                ServerDiscover.Server server = mServerDiscover.getServerList().get((int) action.getId());

                Bundle args = getArguments();
                args.putString("ARG_HOST", server.getAddress());
                args.putString("ARG_PORT", server.getPort());

                AdditionalFragment additionalFragment = new AdditionalFragment();
                additionalFragment.setArguments(args);
                GuidedStepSupportFragment.add(getFragmentManager(), additionalFragment);
            }
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
            Bundle args = getArguments();

            if (findActionById(ACTION_ID_MANUALLY).hasSubActions()) {
                CharSequence hostnameValue = null, protoPortValue = null;
                List<GuidedAction> subActions = findActionById(ACTION_ID_MANUALLY).getSubActions();

                if (subActions != null) {
                    for (int i = 0; i < subActions.size(); i++) {
                        GuidedAction subAction = subActions.get(i);
                        if (subActions.get(i).getId() == ACTION_ID_MANUALLY_HOST) {
                            hostnameValue = subAction.getDescription();
                        } else if (subActions.get(i).getId() == ACTION_ID_MANUALLY_PORT) {
                            protoPortValue = subAction.getDescription();
                        }
                    }
                } else return false;

                if (hostnameValue == null || getString(R.string.pref_default_host).contentEquals(hostnameValue) || TextUtils.isEmpty(hostnameValue)) {
                    Toast.makeText(getActivity(), R.string.first_step_toast_ip_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (protoPortValue == null || TextUtils.isEmpty(protoPortValue)) {
                    Toast.makeText(getActivity(), R.string.first_step_toast_port_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }

                args.putString("ARG_HOST", hostnameValue.toString());
                args.putString("ARG_PORT", protoPortValue.toString());

                AdditionalFragment additionalFragment = new AdditionalFragment();
                additionalFragment.setArguments(args);
                GuidedStepSupportFragment.add(getFragmentManager(), additionalFragment);
            } else return false;
        } else return false;

        return super.onSubGuidedActionClicked(action);
    }
}