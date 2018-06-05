package com.abrenoch.hyperiongrabber.common.util

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.StringRes

class Preferences(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources

    fun getString(@StringRes keyResourceId: Int, default: String? = null): String? {
        return preferences.getString(resources.getString(keyResourceId), default)
    }

    fun putString(@StringRes keyResourceId: Int, value: String){
        val edit = preferences.edit()
        edit.putString(resources.getString(keyResourceId), value)
        edit.apply()
    }

    fun getBoolean(@StringRes keyResourceId: Int, default: Boolean): Boolean =
        preferences.getBoolean(resources.getString(keyResourceId), default)

    fun putBoolean(@StringRes keyResourceId: Int, value: Boolean){
        val edit = preferences.edit()
        edit.putBoolean(resources.getString(keyResourceId), value)
        edit.apply()
    }

}