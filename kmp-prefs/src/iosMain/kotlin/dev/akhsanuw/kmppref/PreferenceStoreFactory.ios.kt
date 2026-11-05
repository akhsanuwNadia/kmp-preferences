package dev.akhsanuw.kmppref

actual class PreferenceStoreFactory {
    /**
     * for target ios only:
     * @param name      not used
     */
    actual fun create(name: String): PreferenceStore {
        return IosPreferenceStore()
    }
}