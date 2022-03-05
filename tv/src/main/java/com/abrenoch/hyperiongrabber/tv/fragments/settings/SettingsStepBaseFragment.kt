package com.abrenoch.hyperiongrabber.tv.fragments.settings

import android.content.Context
import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import android.text.InputType
import android.widget.Toast
import com.abrenoch.hyperiongrabber.common.util.Preferences
import com.abrenoch.hyperiongrabber.tv.R

/** Base class for Settings Fragments. Defines utilities for creating [GuidedAction]s and
 * assertions on action values
 */
internal abstract class SettingsStepBaseFragment : GuidedStepSupportFragment() {

    lateinit var prefs: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Preferences(super.requireContext())
        super.onCreate(savedInstanceState)
    }

    override fun onCreateGuidanceStylist(): GuidanceStylist {
        return SettingsStepStylist()
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
                .title(getString(R.string.guidedstep_save))
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

    protected fun unSignedNumberAction(id: Long, title: String, description: CharSequence?): GuidedAction {
        return GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(description)
                .descriptionEditable(true)
                .descriptionInputType(InputType.TYPE_CLASS_NUMBER)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build()

    }

    protected fun radioListAction(id: Long, title: String, description: String?, setId: Int, optionLabels: Array<String>, optionValues: Array<out Any>, selected: Any?): GuidedAction {
        val subActions = optionLabels.zip(optionValues).map {
            ValueGuidedAction.Companion.Builder(context!!)
                    .parentId(id)
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
    @Throws(AssertionError::class)
    protected fun assertStringValue(actionId: Long): String {
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
    @Throws(AssertionError::class)
    protected fun assertIntValue(actionId: Long): Int {
        return assertStringValue(actionId).let{
            try {
                Integer.parseInt(it)
            } catch (ignored: Exception){
                showToast(getString(R.string.pref_error_invalid_field, it, getString(R.string.pref_title_reconnect_delay)))
                throw AssertionError("$actionId is not a valid int")
            }
        }
    }

    protected fun findActionByIdRecursive(actionId: Long) =
        findActionById(actionId) ?: findSubActionById(actionId)


    protected fun findSubActionById(subActionId: Long): GuidedAction? {
        actions.forEach { mainAction ->
            mainAction.subActions?.find {
                if (it.id == subActionId) {
                    return it
                } else false
            }
        }

        return null
    }

    /** makes sure a value is filled for the GuidedAction and
     * returns that value. If it is not filled, a toast is shown and this
     * fun @throws an [AssertionError]
     */
    protected fun <T: Any> assertSubActionValue(actionId: Long, type: Class<T>): T {
        with(findActionByIdRecursive(actionId)!!){
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

    protected fun notifyActionIdChanged(id: Long){
        notifyActionChanged(findActionPositionById(id))
    }

    /** Returns empty string if not set */
    private fun stringValueForAction(actionId: Long): String =
        findActionByIdRecursive(actionId)?.description?.toString() ?: ""


    protected fun showToast(message: CharSequence){
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun notifyRequired(actionId: Long){
        showToast(getString(R.string.pref_error_missing_field, findActionById(actionId).title))
    }

    companion object {
        const val CONTINUE = -1303L
        const val BACK = -1304L
    }

    class ValueGuidedAction(
            val value: Any? = null,
            val parentId: Long? = null
    ) : GuidedAction() {

        companion object {
            class Builder(context: Context) : GuidedAction.BuilderBase<Builder>(context){
                var value: Any? = null
                var parentId: Long? = null

                fun value(value: Any?): Builder {
                    this.value = value
                    return this
                }

                fun parentId(parentId: Long?): Builder {
                    this.parentId = parentId
                    return this
                }

                fun build(): ValueGuidedAction{
                    val action = ValueGuidedAction(value, parentId)
                    applyValues(action)

                    return action
                }

            }
        }
    }


}
