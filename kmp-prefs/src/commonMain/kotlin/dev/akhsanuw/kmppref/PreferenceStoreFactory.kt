package dev.akhsanuw.kmppref

expect class PreferenceStoreFactory {
    fun create(name: String = "app_preferences"): PreferenceStore
}