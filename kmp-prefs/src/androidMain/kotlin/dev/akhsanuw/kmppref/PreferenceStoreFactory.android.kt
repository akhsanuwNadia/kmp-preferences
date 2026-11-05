package dev.akhsanuw.kmppref

import android.content.Context

actual class PreferenceStoreFactory(private val context: Context) {
    /**
     * for android target only:
     *  @param name     not used
     */
    actual fun create(name: String): PreferenceStore {
        return AndroidPreferenceStore(context = context)
    }
}