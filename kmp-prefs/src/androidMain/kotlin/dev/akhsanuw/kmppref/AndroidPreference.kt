package dev.akhsanuw.kmppref

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

internal sealed class AndroidPreference<T>(
    private val preferences: SharedPreferences,
    private val keyFlow: Flow<String?>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract fun read(preferences: SharedPreferences, key: String, defaultValue: T): T

    abstract fun write(key: String, value: T): SharedPreferences.Editor.() -> Unit

    override fun key(): String = key

    override fun get(): T {
        return try {
            read(preferences, key, defaultValue)
        } catch (e: ClassCastException) {
            delete()
            defaultValue
        }
    }

    override fun set(value: T) {
        preferences.edit(action = write(key, value))
    }

    override fun isSet(): Boolean = preferences.contains(key)

    override fun delete() {
        preferences.edit { remove(key) }
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return keyFlow
            .filter { it == key || it == null }
            .onStart { emit("ignition") }
            .map { get() }
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }

    // -------------------------------------------------------------------------
    // Concrete subtypes
    // -------------------------------------------------------------------------

    internal class StringPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: String,
    ) : AndroidPreference<String>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: String): String =
            preferences.getString(key, defaultValue) ?: defaultValue

        override fun write(key: String, value: String): SharedPreferences.Editor.() -> Unit = { putString(key, value) }
    }

    internal class LongPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Long,
    ) : AndroidPreference<Long>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Long): Long =
            preferences.getLong(key, defaultValue)

        override fun write(key: String, value: Long): SharedPreferences.Editor.() -> Unit = { putLong(key, value) }
    }

    internal class IntPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Int,
    ) : AndroidPreference<Int>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Int): Int =
            preferences.getInt(key, defaultValue)

        override fun write(key: String, value: Int): SharedPreferences.Editor.() -> Unit = { putInt(key, value) }
    }

    internal class FloatPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Float,
    ) : AndroidPreference<Float>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Float): Float =
            preferences.getFloat(key, defaultValue)

        override fun write(key: String, value: Float): SharedPreferences.Editor.() -> Unit = { putFloat(key, value) }
    }

    internal class BooleanPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Boolean,
    ) : AndroidPreference<Boolean>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Boolean): Boolean =
            preferences.getBoolean(key, defaultValue)

        override fun write(key: String, value: Boolean): SharedPreferences.Editor.() -> Unit = { putBoolean(key, value) }
    }

    internal class StringSetPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Set<String>,
    ) : AndroidPreference<Set<String>>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Set<String>): Set<String> =
            preferences.getStringSet(key, defaultValue) ?: defaultValue

        override fun write(key: String, value: Set<String>): SharedPreferences.Editor.() -> Unit = { putStringSet(key, value) }
    }

    internal class ObjectAsString<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : AndroidPreference<T>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: T): T {
            return try {
                preferences.getString(key, null)?.let(deserializer) ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(key: String, value: T): SharedPreferences.Editor.() -> Unit = { putString(key, serializer(value)) }
    }

    internal class ObjectAsInt<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T,
    ) : AndroidPreference<T>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: T): T {
            return try {
                if (preferences.contains(key)) preferences.getInt(key, 0).let(deserializer) else defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(key: String, value: T): SharedPreferences.Editor.() -> Unit = { putInt(key, serializer(value)) }
    }
}