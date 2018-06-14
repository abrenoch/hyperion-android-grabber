package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.app.Activity
import android.os.Bundle
import android.support.v17.leanback.widget.GuidanceStylist
import android.support.v17.leanback.widget.GuidedAction
import com.abrenoch.hyperiongrabber.tv.R

internal class BasicSettingsStepFragment : SettingsStepBaseFragment() {

    override fun onProvideTheme(): Int {
        return R.style.Theme_Example_Leanback_GuidedStep_First
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
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
                .description(prefs.getString(R.string.pref_key_hyperion_host, null))
                .descriptionEditable(true)
                .build()

        val enterPort = unSignedNumberAction(
                ACTION_PORT,
                getString(R.string.pref_title_port),
                prefs.getString(R.string.pref_key_hyperion_port, "19445")
        )
        val priority = unSignedNumberAction(
                ACTION_MESSAGE_PRIORITY,
                getString(R.string.pref_title_priority),
                prefs.getString(R.string.pref_key_hyperion_priority, "50")
        )

        val reconnectEnabled = prefs.getBoolean(R.string.pref_key_reconnect, true)

        val reconnect = GuidedAction.Builder(context)
                .id(ACTION_RECONNECT)
                .title(getString(R.string.pref_title_reconnect))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(reconnectEnabled)
                .build()

        val reconnectDelay = unSignedNumberAction(
                ACTION_RECONNECT_DELAY,
                getString(R.string.pref_title_reconnect_delay),
                prefs.getString(R.string.pref_key_reconnect_delay, "5")
        )

        val reconnectDescription =
                if (prefs.contains(R.string.pref_key_reconnect)){
                    if (reconnectEnabled){
                        R.string.enabled
                    } else {
                        R.string.disabled
                    }
                } else {
                    R.string.pref_summary_reconnect
                }


        val reconnectGroup = GuidedAction.Builder(context)
                .id(ACTION_RECONNECT_GROUP)
                .title(getString(R.string.pref_title_reconnect))
                .description(reconnectDescription)
                .subActions(listOf(reconnect, reconnectDelay))
                .build()

        val frameRateLabels = resources.getStringArray(R.array.pref_list_framerate)
        val frameRateValues = resources.getIntArray(R.array.pref_list_framerate_values).toTypedArray()

        val selectedCaptureRate = prefs.getInt(R.string.pref_key_hyperion_framerate, 30)

        val captureRateDescription =
                if (prefs.contains(R.string.pref_key_hyperion_framerate)){
                    frameRateLabels[frameRateValues.indexOf(selectedCaptureRate)]
                } else {
                    getString(R.string.pref_summary_framerate)
                }

        val captureRate = radioListAction(
                ACTION_CAPTURE_RATE,
                getString(R.string.pref_title_framerate),
                captureRateDescription,
                ACTION_CAPTURE_RATE_SET_ID,
                frameRateLabels,
                frameRateValues,
                selectedCaptureRate

        )

        val useOgl = prefs.getBoolean(R.string.pref_key_ogl_grabber, false)


        val mediaProjection = ValueGuidedAction.Companion.Builder(context)
                .id(ACTION_GRABBER_MEDIA)
                .value(false)
                .title(R.string.pref_title_media_grabber)
                .description(getString(R.string.pref_summary_media_grabber))
                .checkSetId(ACTION_GRABBER_SET_ID)
                .checked(!useOgl)
                .build()

        val ogl = ValueGuidedAction.Companion.Builder(context)
                .id(ACTION_GRABBER_OGL)
                .value(true)
                .title(R.string.pref_title_ogl_grabber)
                .description(R.string.pref_summary_ogl_grabber)
                .checkSetId(ACTION_GRABBER_SET_ID)
                .checked(useOgl)
                .build()

        val grabberDescription =
            if (prefs.contains(R.string.pref_key_ogl_grabber)){
                if (useOgl){
                    R.string.pref_title_ogl_grabber
                } else {
                    R.string.pref_title_media_grabber
                }
            } else {
                R.string.pref_summary_grabber
            }


        val grabberGroup = GuidedAction.Builder(context)
                .id(ACTION_GRABBER_GROUP)
                .title(getString(R.string.pref_group_grabber))
                .description(grabberDescription)
                .subActions(listOf(mediaProjection, ogl))
                .build()

        actions.add(stepInfo)
        actions.add(enterHost)
        actions.add(enterPort)
        actions.add(priority)
        actions.add(reconnectGroup)
        actions.add(captureRate)
        actions.add(grabberGroup)

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
                val useOgl = assertSubActionValue(ACTION_GRABBER_GROUP, Boolean::class.java)
                val reconnect = findSubActionById(ACTION_RECONNECT)!!.isChecked
                val reconnectDelay = assertValue(ACTION_RECONNECT_DELAY).let{
                    try {
                        Integer.parseInt(it)
                    } catch (ignored: Exception){
                        showToast(getString(R.string.pref_error_invalid_field, it, getString(R.string.pref_title_reconnect_delay)))
                        throw AssertionError("invalid reconnectDelay")
                    }
                }

                prefs.putString(R.string.pref_key_hyperion_host, host)
                prefs.putString(R.string.pref_key_hyperion_port, port)
                prefs.putInt(R.string.pref_key_reconnect_delay, reconnectDelay)
                prefs.putInt(R.string.pref_key_hyperion_framerate, frameRate)
                prefs.putBoolean(R.string.pref_key_reconnect, reconnect)
                prefs.putBoolean(R.string.pref_key_ogl_grabber, useOgl)

                val activity = activity
                activity.setResult(Activity.RESULT_OK)
                finishGuidedStepSupportFragments()

            } catch (ignored: AssertionError) {
            }

            return

        }

        super.onGuidedActionClicked(action)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        when {
            action.id == ACTION_RECONNECT -> {
                val newDescription = if(action.isChecked){
                    getString(R.string.enabled)
                } else getString(R.string.disabled)

                findActionById(ACTION_RECONNECT_GROUP)
                        .description = newDescription
                notifyActionIdChanged(ACTION_RECONNECT_GROUP)



                return !action.isChecked
            }
            action.id == ACTION_GRABBER_MEDIA -> {
                findActionById(ACTION_GRABBER_GROUP).description = getString(R.string.pref_title_media_grabber)
                notifyActionIdChanged(ACTION_GRABBER_GROUP)

            }
            action.id == ACTION_GRABBER_OGL -> {
                findActionById(ACTION_GRABBER_GROUP).description = getString(R.string.pref_title_ogl_grabber)
                notifyActionIdChanged(ACTION_GRABBER_GROUP)
            }
            action is ValueGuidedAction && action.parentId != null -> {
                findActionById(action.parentId).description = action.title
                notifyActionIdChanged(action.parentId)
            }
        }


        return super.onSubGuidedActionClicked(action)
    }

    companion object {
        private const val ACTION_HOST_NAME = 100L
        private const val ACTION_PORT = 110L
        private const val ACTION_RECONNECT_GROUP = 200L
        private const val ACTION_RECONNECT = 210L
        private const val ACTION_RECONNECT_DELAY = 220L
        private const val ACTION_MESSAGE_PRIORITY = 300L
        private const val ACTION_CAPTURE_RATE = 400L
        private const val ACTION_CAPTURE_RATE_SET_ID = 1500
        private const val ACTION_GRABBER_GROUP = 500L
        private const val ACTION_GRABBER_SET_ID = 550
        private const val ACTION_GRABBER_MEDIA = 560L
        private const val ACTION_GRABBER_OGL = 570L
    }


}
