package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.app.Activity
import android.os.Bundle
import android.support.v17.leanback.widget.GuidanceStylist
import android.support.v17.leanback.widget.GuidedAction

import com.abrenoch.hyperiongrabber.common.util.Preferences
import com.abrenoch.hyperiongrabber.tv.R

internal class BasicSettingsStepFragment : SettingsStepBaseFragment() {

    private var prefs: Preferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = preferences
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Example_Leanback_GuidedStep_First
    }

    override fun onCreateGuidance(savedInstanceState: Bundle): GuidanceStylist.Guidance {
        val title = getString(R.string.guidedstep_basic_settings_title)
        val description = getString(R.string.guidedstep_basic_settings_description)
        val breadCrumb = getString(R.string.guidedstep_basic_settings_breadcrumb)
        val icon = activity.getDrawable(R.drawable.ic_settings_ethernet_white_128dp)
        return GuidanceStylist.Guidance(title, description, breadCrumb, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val desc = resources.getString(R.string.guidedstep_action_description)
        val stepInfo = GuidedAction.Builder(context)
                .title(resources.getString(R.string.guidedstep_action_title))
                .description(desc)
                .multilineDescription(true)
                .focusable(false)
                .infoOnly(true)
                .enabled(false)
                .build()

        val enterHost = GuidedAction.Builder(context)
                .id(ACTION_HOST_NAME)
                .title(getString(R.string.pref_title_host))
                .description(preferences.getString(R.string.pref_key_hyperion_host, null))
                .descriptionEditable(true)
                .build()

        val enterPort = unSignedNumberAction(
                ACTION_PORT,
                getString(R.string.pref_title_port),
                preferences.getString(R.string.pref_key_hyperion_port, "19445")
        )
        val priority = unSignedNumberAction(
                ACTION_MESSAGE_PRIORITY,
                getString(R.string.pref_title_priority),
                preferences.getString(R.string.pref_key_hyperion_priority, "50")
        )

        val reconnect = GuidedAction.Builder(context)
                .id(ACTION_RECONNECT)
                .title(getString(R.string.pref_title_reconnect))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(false)
                .build()

        val reconnectDelay = unSignedNumberAction(
                ACTION_RECONNECT_DELAY,
                getString(R.string.pref_title_reconnect_delay),
                preferences.getString(R.string.pref_key_reconnect_delay, "5")
        )

        val frameRateLabels = resources.getStringArray(R.array.pref_list_framerate)
        val frameRateValues = resources.getStringArray(R.array.pref_list_framerate_values)

        val captureRate = radioListAction(
                ACTION_CAPTURE_RATE,
                getString(R.string.pref_title_framerate),
                getString(R.string.pref_summary_framerate),
                ACTION_CAPTURE_RATE_SET_ID,
                frameRateLabels,
                frameRateValues,
                preferences.getString(R.string.pref_key_hyperion_framerate, "30")

        )

        actions.add(stepInfo)
        actions.add(enterHost)
        actions.add(enterPort)
        actions.add(priority)
        actions.add(reconnect)
        actions.add(reconnectDelay)
        actions.add(captureRate)

    }

    override fun onCreateButtonActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(continueAction())
        actions.add(backAction())
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SettingsStepBaseFragment.CONTINUE) {

            try {
                val host = assertValue(ACTION_HOST_NAME)
                val port = assertValue(ACTION_PORT)
                val frameRate = assertSubActionValue(ACTION_CAPTURE_RATE, Int::class.java)
                val reconnect = findActionById(ACTION_RECONNECT).isChecked

                prefs!!.putString(R.string.pref_key_hyperion_host, host)
                prefs!!.putString(R.string.pref_key_hyperion_port, port)
                prefs!!.putInt(R.string.pref_key_hyperion_framerate, frameRate)
                prefs!!.putBoolean(R.string.pref_key_reconnect, reconnect)

                val activity = activity
                activity.setResult(Activity.RESULT_OK)
                finishGuidedStepSupportFragments()

            } catch (ignored: AssertionError) {
            }

            return

        }

        super.onGuidedActionClicked(action)
    }

    companion object {
        private const val ACTION_HOST_NAME = 100L
        private const val ACTION_PORT = 110L
        private const val ACTION_RECONNECT = 120L
        private const val ACTION_RECONNECT_DELAY = 130L
        private const val ACTION_MESSAGE_PRIORITY = 140L
        private const val ACTION_CAPTURE_RATE = 150L
        private const val ACTION_CAPTURE_RATE_SET_ID = 1500
    }


}
