package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import android.widget.Toast
import com.abrenoch.hyperiongrabber.common.network.Hyperion
import com.abrenoch.hyperiongrabber.tv.R
import java.lang.ref.WeakReference
import java.net.UnknownHostException

internal class BasicSettingsStepFragment : SettingsStepBaseFragment() {

    /** The amount of times the connection was tested */
    private var testCounter = 0

    override fun onProvideTheme(): Int {
        return R.style.Theme_HyperionGrabber_GuidedStep_First
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val title = getString(R.string.guidedstep_basic_settings_title)
        val description = getString(R.string.guidedstep_basic_settings_description)
        val icon = activity?.getDrawable(R.drawable.ic_qr_android_tv_remote_short)
        return GuidanceStylist.Guidance(title, description, null, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {

        val enterHost = GuidedAction.Builder(context)
                .id(ACTION_HOST_NAME)
                .title(getString(R.string.pref_title_host))
                .description(prefs.getString(R.string.pref_key_host, null))
                .descriptionEditable(true)
                .build()

        val enterPort = unSignedNumberAction(
                ACTION_PORT,
                getString(R.string.pref_title_port),
                prefs.getInt(R.string.pref_key_port).toString()
        )

        val enterHorizontalLEDCount = unSignedNumberAction(
                ACTION_X_LED_COUNT,
                getString(R.string.pref_title_x_led),
                prefs.getInt(R.string.pref_key_x_led).toString()
        )

        val enterVerticalLEDCount = unSignedNumberAction(
                ACTION_Y_LED_COUNT,
                getString(R.string.pref_title_y_led),
                prefs.getInt(R.string.pref_key_y_led).toString()
        )

        val startOnBootEnabled = prefs.getBoolean(R.string.pref_key_boot)

        val startOnBoot = GuidedAction.Builder(context)
                .id(ACTION_START_ON_BOOT)
                .title(getString(R.string.pref_title_boot))
                .description(R.string.pref_summary_boot)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(startOnBootEnabled)
                .build()

        val advancedInfo = GuidedAction.Builder(context)
                .title(R.string.guidedstep_section_advanced_title)
                .description(R.string.guidedstep_section_advanced_description)
                .multilineDescription(true)
                .focusable(false)
                .infoOnly(true)
                .enabled(false)
                .build()

        val priority = unSignedNumberAction(
                ACTION_MESSAGE_PRIORITY,
                getString(R.string.pref_title_priority),
                prefs.getString(R.string.pref_key_priority, "50")
        )

        val reconnectEnabled = prefs.getBoolean(R.string.pref_key_reconnect)

        val reconnect = GuidedAction.Builder(context)
                .id(ACTION_RECONNECT)
                .title(getString(R.string.pref_title_reconnect))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(reconnectEnabled)
                .build()

        val reconnectDelay = unSignedNumberAction(
                ACTION_RECONNECT_DELAY,
                getString(R.string.pref_title_reconnect_delay),
                prefs.getInt(R.string.pref_key_reconnect_delay).toString()
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
        val frameRateValues = resources.getStringArray(R.array.pref_list_framerate_values)

        val selectedCaptureRate = prefs.getString(R.string.pref_key_framerate, "30")

        val captureRateDescription =
                if (prefs.contains(R.string.pref_key_framerate)){
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

        val averageColor = GuidedAction.Builder(context)
                .id(ACTION_AVERAGE_COLOR)
                .title(getString(R.string.pref_title_use_avg_color))
                .description(R.string.pref_summary_use_avg_color)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(prefs.getBoolean(R.string.pref_key_use_avg_color))
                .build()

        actions.add(enterHost)
        actions.add(enterPort)
        actions.add(enterHorizontalLEDCount)
        actions.add(enterVerticalLEDCount)
        actions.add(startOnBoot)
        actions.add(advancedInfo)
        actions.add(priority)
        actions.add(reconnectGroup)
        actions.add(captureRate)
        actions.add(averageColor)

    }

    override fun onCreateButtonActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(continueAction())
        actions.add(GuidedAction.Builder(context)
                .id(ACTION_TEST)
                .title(getString(R.string.guidedstep_test))
                .description(R.string.guidedstep_test_description)
                .build()

        )
        actions.add(backAction())
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SettingsStepBaseFragment.CONTINUE) {

            try {
                val host = assertStringValue(ACTION_HOST_NAME)
                val port = assertIntValue(ACTION_PORT)
                val xLED = assertIntValue(ACTION_X_LED_COUNT)
                val yLED = assertIntValue(ACTION_Y_LED_COUNT)
                val startOnBootEnabled = findActionById(ACTION_START_ON_BOOT).isChecked
                val priority = assertIntValue(ACTION_MESSAGE_PRIORITY)
                val frameRate = assertSubActionValue(ACTION_CAPTURE_RATE, String::class.java)
                val reconnect = findSubActionById(ACTION_RECONNECT)!!.isChecked
                val reconnectDelay = assertIntValue(ACTION_RECONNECT_DELAY)
                val useAverageColor = findActionById(ACTION_AVERAGE_COLOR)!!.isChecked

                prefs.putString(R.string.pref_key_host, host)
                prefs.putInt(R.string.pref_key_port, port)
                prefs.putInt(R.string.pref_key_x_led, xLED)
                prefs.putInt(R.string.pref_key_y_led, yLED)
                prefs.putBoolean(R.string.pref_key_boot, startOnBootEnabled)
                prefs.putInt(R.string.pref_key_priority, priority)
                prefs.putInt(R.string.pref_key_reconnect_delay, reconnectDelay)
                prefs.putString(R.string.pref_key_framerate, frameRate)
                prefs.putBoolean(R.string.pref_key_reconnect, reconnect)
                prefs.putBoolean(R.string.pref_key_use_avg_color, useAverageColor)

                val activity = activity
                activity?.setResult(Activity.RESULT_OK)
                finishGuidedStepSupportFragments()

            } catch (ignored: AssertionError) {
            }

            return

        } else if (action.id == ACTION_TEST){
            val colorIdx = testCounter % TEST_COLORS.size
            val color = TEST_COLORS[colorIdx]
            testCounter++

            try {
                val host = assertStringValue(ACTION_HOST_NAME)
                val port = assertIntValue(ACTION_PORT)
                val priority = assertIntValue(ACTION_MESSAGE_PRIORITY)
                testHyperionColor(host, port, priority, color)
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
            action is ValueGuidedAction && action.parentId != null -> {
                findActionById(action.parentId).description = action.title
                notifyActionIdChanged(action.parentId)
            }
        }


        return super.onSubGuidedActionClicked(action)
    }

    /** tries to connect to Hyperion and sets the given color for 5 seconds  */
    private fun testHyperionColor(hostName: String, port: Int, priority: Int, color: Int) {
        TestTask(context!!).execute(TestSpec(hostName, port, priority, color))
    }

    companion object {
        private const val ACTION_HOST_NAME = 100L
        private const val ACTION_PORT = 110L
        private const val ACTION_START_ON_BOOT = 120L
        private const val ACTION_X_LED_COUNT = 130L
        private const val ACTION_Y_LED_COUNT = 140L
        private const val ACTION_RECONNECT_GROUP = 200L
        private const val ACTION_RECONNECT = 210L
        private const val ACTION_RECONNECT_DELAY = 220L
        private const val ACTION_MESSAGE_PRIORITY = 300L
        private const val ACTION_CAPTURE_RATE = 400L
        private const val ACTION_CAPTURE_RATE_SET_ID = 1500
        private const val ACTION_AVERAGE_COLOR = 600L


        private const val ACTION_TEST = 700L

        private val TEST_COLORS = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)

        /**
         * Tries to connect to Hyperion and set all LEDs to a single color
         */
        class TestTask(context: Context): AsyncTask<TestSpec, Int, Int>(){


            private val contextRef = WeakReference(context)

            override fun doInBackground(vararg params: TestSpec?): Int {
                try {
                    val (host, port, priority, color) = params[0]!!
                    val hyperion = Hyperion(host, port)
                    if (hyperion.isConnected) {
                        hyperion.setColor(color, priority, 3000)
                    } else {
                        return RESULT_UNREACHABLE
                    }
                    hyperion.disconnect()
                    return RESULT_OK
                } catch (e: UnknownHostException) {
                    return RESULT_UNREACHABLE
                } catch (e: Exception){
                    return RESULT_UNKNOWN_ERROR
                }
            }

            override fun onPostExecute(result: Int) {
                contextRef.get()?.run {
                    val messageRes = when(result){
                        RESULT_OK -> R.string.guidedstep_test_success
                        RESULT_UNREACHABLE -> R.string.guidedstep_test_host_unreachable
                        else -> R.string.guidedstep_test_unknown_error
                    }

                    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
                }



            }

            companion object {
                const val RESULT_OK = 1
                const val RESULT_UNREACHABLE = -1
                const val RESULT_UNKNOWN_ERROR = -100
            }

        }

        data class TestSpec(val host: String, val port: Int, val priority: Int, val color: Int)

    }


}
