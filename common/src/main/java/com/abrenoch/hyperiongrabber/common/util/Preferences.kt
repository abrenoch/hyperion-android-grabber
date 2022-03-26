package com.abrenoch.hyperiongrabber.common.util

import android.content.Context
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.annotation.StringRes

/** Wrapper around SharedPreferences which allows for default values defined in Resources
 * Main purpose is that defaults are defined in a centralized location and that preferences are
 * accessed through a unified interface */
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

    fun getInt(@StringRes keyResourceId: Int): Int {
        val default = defaultKey(keyResourceId, "integer").let {
            if (it == 0){
                0
            } else {
                try {
                    resources.getInteger(it)
                } catch (e: Resources.NotFoundException) {
                    0
                }
            }
        }

        return getInt(keyResourceId, default)
    }

    fun getInt(@StringRes keyResourceId: Int, default: Int = 0): Int {
        return getString(keyResourceId)?.let { Integer.parseInt(it) } ?: default
    }

    fun putInt(@StringRes keyResourceId: Int, value: Int){
        putString(keyResourceId, value.toString())
    }

    fun getBoolean(@StringRes keyResourceId: Int): Boolean {
        val default = defaultKey(keyResourceId, "bool").let {
            if (it == 0){
                false
            } else {
                try {
                    resources.getBoolean(it)
                } catch (e: Resources.NotFoundException) {
                    false
                }
            }
        }

        return preferences.getBoolean(key(keyResourceId), default)
    }

    fun getBoolean(@StringRes keyResourceId: Int, default: Boolean): Boolean =
        preferences.getBoolean(key(keyResourceId), default)

    fun putBoolean(@StringRes keyResourceId: Int, value: Boolean){
        val edit = preferences.edit()
        edit.putBoolean(key(keyResourceId), value)
        edit.apply()
    }

    private fun key(keyResourceId: Int) = resources.getString(keyResourceId)

    /** @return 0 if not found, resource id otherwise */
    private fun defaultKey(keyResourceId: Int, type: String): Int {
        val defaultKeyName = resources.getResourceEntryName(keyResourceId).replace("pref_key_", "pref_default_")
        return resources.getIdentifier(defaultKeyName, type, resources.getResourcePackageName(keyResourceId))
    }

}