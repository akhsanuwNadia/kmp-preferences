package dev.akhsanuw.kmppref

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.prefs.PreferenceChangeListener
import java.util.prefs.Preferences

internal sealed class JvmPreference<T>(
    protected val prefs: Preferences,
    private val keyFlow: Flow<String>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract fun read(prefs: Preferences, key: String, defaultValue: T): T
    abstract fun write(prefs: Preferences, key: String, value: T)

    override fun key(): String = key

    override fun get(): T = try {
        read(prefs, key, defaultValue)
    } catch (e: Exception) {
        defaultValue
    }

    override fun set(value: T) = write(prefs, key, value)

    override fun isSet(): Boolean = prefs.keys().contains(key)

    override fun delete() = prefs.remove(key)

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return keyFlow
            .filter { it == key }
            .onStart { emit(key) }
            .map { get() }
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())

    // -------------------------------------------------------------------------
    // Concrete subtypes
    // -------------------------------------------------------------------------

    internal class StringPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: String,
    ) : JvmPreference<String>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: String): String =
            prefs.get(key, defaultValue)

        override fun write(prefs: Preferences, key: String, value: String) =
            prefs.put(key, value)
    }

    internal class LongPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Long,
    ) : JvmPreference<Long>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: Long): Long =
            prefs.getLong(key, defaultValue)

        override fun write(prefs: Preferences, key: String, value: Long) =
            prefs.putLong(key, value)
    }

    internal class IntPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Int,
    ) : JvmPreference<Int>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: Int): Int =
            prefs.getInt(key, defaultValue)

        override fun write(prefs: Preferences, key: String, value: Int) =
            prefs.putInt(key, value)
    }

    internal class FloatPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Float,
    ) : JvmPreference<Float>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: Float): Float =
            prefs.getFloat(key, defaultValue)

        override fun write(prefs: Preferences, key: String, value: Float) =
            prefs.putFloat(key, value)
    }

    internal class BooleanPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Boolean,
    ) : JvmPreference<Boolean>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: Boolean): Boolean =
            prefs.getBoolean(key, defaultValue)

        override fun write(prefs: Preferences, key: String, value: Boolean) =
            prefs.putBoolean(key, value)
    }

    /**
     * Stores a [Set<String>] as a comma-joined string.
     * Commas inside values are not supported; use [ObjectAsString] with a custom serializer
     * if your strings can contain commas.
     */
    internal class StringSetPrimitive(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Set<String>,
    ) : JvmPreference<Set<String>>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: Set<String>): Set<String> {
            val raw = prefs.get(key, null) ?: return defaultValue
            return if (raw.isEmpty()) emptySet() else raw.split(DELIMITER).toSet()
        }

        override fun write(prefs: Preferences, key: String, value: Set<String>) {
            prefs.put(key, value.joinToString(DELIMITER))
        }

        companion object {
            private const val DELIMITER = ","
        }
    }

    internal class ObjectAsString<T>(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : JvmPreference<T>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: T): T {
            return try {
                prefs.get(key, null)?.let(deserializer) ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(prefs: Preferences, key: String, value: T) {
            prefs.put(key, serializer(value))
        }
    }

    internal class ObjectAsInt<T>(
        prefs: Preferences,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T,
    ) : JvmPreference<T>(prefs, keyFlow, key, defaultValue) {
        override fun read(prefs: Preferences, key: String, defaultValue: T): T {
            return try {
                if (prefs.keys().contains(key)) prefs.getInt(key, 0).let(deserializer) else defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(prefs: Preferences, key: String, value: T) {
            prefs.putInt(key, serializer(value))
        }
    }
}

// ---------------------------------------------------------------------------
// Extension: expose java.util.prefs.Preferences changes as a Flow<String>
// ---------------------------------------------------------------------------
internal val Preferences.keyFlow: Flow<String>
    get() = callbackFlow {
        val listener = PreferenceChangeListener { event ->
            trySend(event.key)
        }
        addPreferenceChangeListener(listener)
        awaitClose { removePreferenceChangeListener(listener) }
    }