package dev.akhsanuw.kmppref

actual class PreferenceStoreFactory {
    actual fun create(name: String): PreferenceStore {
        return WebPreferenceStore(name)
    }
}