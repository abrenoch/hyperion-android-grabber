package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.content.Context
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepSupportFragment
import android.support.v17.leanback.widget.GuidedAction
import android.text.InputType
import android.widget.Toast
import com.abrenoch.hyperiongrabber.common.util.Preferences
import com.abrenoch.hyperiongrabber.tv.R

internal abstract class SettingsStepBaseFragment : GuidedStepSupportFragment() {

    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = Preferences(context)
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

    protected fun unSignedNumberAction(id: Long, title: String, description: String?): GuidedAction {
        return GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(description)
                .descriptionEditable(true)
                .descriptionInputType(InputType.TYPE_CLASS_NUMBER)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build()

    }

    protected fun radioListAction(id: Long, title: String, description: String?, setId: Int, optionLabels: Array<String>, optionValues: Array<String>, selected: String?): GuidedAction {
        val subActions = optionLabels.zip(optionValues).map {
            ValueGuidedAction.Companion.Builder(context)
                    .checkSetId(setId)
                    .title(it.first)
                    .value(it.second)
                    .checked(it.second == selected)
                    .build()
        }


        return GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(description)
                .subActions(subActions)
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

    /** makes sure a value is filled for the GuidedAction and
     * returns that value. If it is not filled, a toast is shown and this
     * fun @throws a [AssertionError]
     */
    protected fun <T: Any> assertSubActionValue(actionId: Long, type: Class<T>): T {
        with(findAction(actionId)){
            val selected = subActions.find { it.isChecked }

            if (selected is ValueGuidedAction){
                return selected.value as T
            }

            if (selected == null) {
                notifyRequired(actionId)
                throw AssertionError("$actionId has no value")
            }
            return selected.title.toString() as T
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

    companion object {
        protected const val CONTINUE = -1303L
        protected const val BACK = -1304L
    }

    class ValueGuidedAction : GuidedAction() {
        var value: Any? = null

        companion object {
            class Builder(context: Context) : GuidedAction.BuilderBase<Builder>(context){
                var value: Any? = null

                fun value(value: Any?): Builder {
                    this.value = value
                    return this
                }

                fun build(): ValueGuidedAction{
                    val action = ValueGuidedAction()
                    applyValues(action)

                    action.value = value
                    return action
                }

            }
        }
    }


}
