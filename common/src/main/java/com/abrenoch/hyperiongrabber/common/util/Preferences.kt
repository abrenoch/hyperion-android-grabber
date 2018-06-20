package com.abrenoch.hyperiongrabber.common.util

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.StringRes

class Preferences(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources

    fun contains(@StringRes keyResourceId: Int): Boolean = preferences.contains(key(keyResourceId))

    fun getString(@StringRes keyResourceId: Int, default: String? = null): String? {
        return preferences.getString(key(keyResourceId), default)
    }

    fun putString(@StringRes keyResourceId: Int, value: String){
        val edit = preferences.edit()
        edit.putString(key(keyResourceId), value)
        edit.apply()
    }

    fun getInt(@StringRes keyResourceId: Int, default: Int = 0): Int {
        return getString(keyResourceId)?.let { Integer.parseInt(it) } ?: default
    }

    fun putInt(@StringRes keyResourceId: Int, value: Int){
        putString(keyResourceId, value.toString())
    }

    fun getBoolean(@StringRes keyResourceId: Int, default: Boolean): Boolean =
        preferences.getBoolean(key(keyResourceId), default)

    fun putBoolean(@StringRes keyResourceId: Int, value: Boolean){
        val edit = preferences.edit()
        edit.putBoolean(key(keyResourceId), value)
        edit.apply()
    }

    private fun key(keyResourceId: Int) = resources.getString(keyResourceId)

}