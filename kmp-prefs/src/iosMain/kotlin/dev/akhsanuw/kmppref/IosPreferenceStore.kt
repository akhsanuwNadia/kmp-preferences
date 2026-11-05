package dev.akhsanuw.kmppref

import platform.Foundation.NSUserDefaults

import dev.akhsanuw.kmppref.IosPreference.*

class IosPreferenceStore(
    private val delegate: NSUserDefaults = NSUserDefaults.standardUserDefaults()
): PreferenceStore {
    override fun getString(
        key: String,
        defaultValue: String
    ): Preference<String> = StringPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun getLong(
        key: String,
        defaultValue: Long
    ): Preference<Long> = LongPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun getInt(
        key: String,
        defaultValue: Int
    ): Preference<Int> = IntPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun getFloat(
        key: String,
        defaultValue: Float
    ): Preference<Float> = FloatPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun getBoolean(
        key: String,
        defaultValue: Boolean
    ): Preference<Boolean> = BooleanPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun getStringSet(
        key: String,
        defaultValue: Set<String>
    ): Preference<Set<String>> = StringSetPrimitive(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue
    )

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T
    ): Preference<T> = ObjectAsString(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue,
        serializer,
        deserializer
    )

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T
    ): Preference<T> = ObjectAsInt(
        delegate,
        keyFlow(key, delegate),
        key,
        defaultValue,
        serializer,
        deserializer
    )

    @Suppress("UNCHECKED_CAST")
    override fun getAll(): Map<String, *> {
        val keys = delegate.dictionaryRepresentation().keys as Set<String>
        return keys.associateWith { key -> delegate.objectForKey(key) }
    }
}