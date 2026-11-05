package dev.akhsanuw.kmppref

import java.util.prefs.Preferences as JPrefs
import dev.akhsanuw.kmppref.JvmPreference.*

class JvmPreferenceStore(
    nodePath: String,
    private val prefs: JPrefs = JPrefs.userRoot().node(nodePath),
) : PreferenceStore {

    private val keyFlow = prefs.keyFlow

    override fun getString(key: String, defaultValue: String): Preference<String> =
        StringPrimitive(prefs, keyFlow, key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        LongPrimitive(prefs, keyFlow, key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
        IntPrimitive(prefs, keyFlow, key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        FloatPrimitive(prefs, keyFlow, key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        BooleanPrimitive(prefs, keyFlow, key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        StringSetPrimitive(prefs, keyFlow, key, defaultValue)

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = ObjectAsString(
        prefs = prefs,
        keyFlow = keyFlow,
        key = key,
        defaultValue = defaultValue,
        serializer = serializer,
        deserializer = deserializer,
    )

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = ObjectAsInt(
        prefs = prefs,
        keyFlow = keyFlow,
        key = key,
        defaultValue = defaultValue,
        serializer = serializer,
        deserializer = deserializer,
    )

    override fun getAll(): Map<String, *> {
        return prefs.keys().associateWith { key -> prefs.get(key, null) }
    }

    companion object {
        const val DEFAULT_NODE = "node.prefs"
    }
}