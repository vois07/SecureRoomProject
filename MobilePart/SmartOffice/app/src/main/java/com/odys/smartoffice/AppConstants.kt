package com.odys.smartoffice

import kotlin.properties.Delegates

abstract class AppConstants {
    companion object {
        var login: String = ""
        const val splashTime: Long = 2000
        var dataUpdate: String by Delegates.observable("dataupdate") {_, old, new -> onDataChanged?.invoke(old, new)}
        var onDataChanged: ((String, String) -> Unit)? = null
        const val refreshTime: Long = 60*1000
        var work: Boolean by Delegates.observable(false) {_, old, new -> onWorkChange?.invoke(old, new)}
        var onWorkChange: ((Boolean, Boolean) -> Unit)? = null
    }
}