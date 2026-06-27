package com.energiaxxi.conversor.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "lang_prefs"
    private const val KEY_LANG = "language"
    private val supportedLanguages = listOf("es", "ca", "gl", "eu", "ast", "oc")

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "es") ?: "es"
    }

    fun saveLanguage(context: Context, langCode: String) {
        if (langCode !in supportedLanguages) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, langCode)
            .apply()
    }

    fun wrapContext(context: Context, langCode: String): Context {
        Locale.setDefault(Locale(langCode))
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(langCode))
        return context.createConfigurationContext(config)
    }
}
