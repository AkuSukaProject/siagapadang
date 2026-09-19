package com.akusukaproject.siagapadang.data.repository

import android.content.Context

/** Pengaturan pengguna yang disimpan di HP. */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vibrateBeforeTurn: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_BEFORE_TURN, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATE_BEFORE_TURN, value).apply()
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "siaga_padang_settings"
        const val KEY_VIBRATE_BEFORE_TURN = "vibrate_before_turn"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
