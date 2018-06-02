package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v17.leanback.app.GuidedStepSupportFragment
import android.support.v17.leanback.widget.GuidedAction
import android.widget.Toast

import com.abrenoch.hyperiongrabber.tv.R

internal abstract class SettingsStepBaseFragment : GuidedStepSupportFragment() {

    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        super.onCreate(savedInstanceState)

    }

    /** Default implementation handles backAction only*/
    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == BACK) {
            finishGuidedStepSupportFragments()
        }
    }

    protected fun continueAction(): GuidedAction {
        return GuidedAction.Builder(context)
                .id(CONTINUE)
                .title(getString(R.string.guidedstep_continue))
                .description(R.string.guidedstep_letsdoit)
                .build()
    }

    protected fun backAction(): GuidedAction {
        return GuidedAction.Builder(context)
                .id(BACK)
                .title(getString(R.string.guidedstep_cancel))
                .description(R.string.guidedstep_nevermind)
                .build()
    }

    /** makes sure a value is filled for the GuidedAction and
     * returns that value. If it is not filled, a toast is shown and this
     * fun @throws a [AssertionError]
     */
    protected fun assertValue(actionId: Long): String {
        with(stringValueForAction(actionId)){
            if (isEmpty()){
                notifyRequired(actionId)
                throw AssertionError("$actionId has no value")
            }
            return this
        }
    }

    /** Returns empty string if not set */
    private fun stringValueForAction(actionId: Long): String =
        findAction(actionId).description?.toString() ?: ""


    private fun findAction(actionId: Long): GuidedAction =
        actions.find { it.id == actionId }!!

    private fun showToast(message: CharSequence){
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun notifyRequired(actionId: Long){
        showToast("Please enter ${findAction(actionId).title}")
    }

    /** Creates an editor, calls a lambda on it and persists the edits */
    protected fun save(operation: (SharedPreferences.Editor) -> Unit){
        val edit = preferences.edit()
        operation.invoke(edit)
        edit.apply()
    }

    companion object {
        protected const val CONTINUE = -1303L
        protected const val BACK = -1304L
    }
}
