package dev.akhsanuw.kmppref

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InMemoryPreferenceStore(
    initialPreferences: Sequence<InMemoryPreference<*>> = sequenceOf(),
) : PreferenceStore {

    private val preferences: Map<String, Preference<*>> =
        initialPreferences.toList().associateBy { it.key() }

    override fun getString(key: String, defaultValue: String): Preference<String> {
        val data = preferences[key]?.get() as? String
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        val data = preferences[key]?.get() as? Long
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        val data = preferences[key]?.get() as? Int
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        val data = preferences[key]?.get() as? Float
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        val data = preferences[key]?.get() as? Boolean
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        @Suppress("UNCHECKED_CAST")
        val data = preferences[key]?.get() as? Set<String>
        return InMemoryPreference(key, data, defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        val data = preferences[key]?.get() as? T
        return InMemoryPreference(key, data, defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> {
        val data = preferences[key]?.get() as? T
        return InMemoryPreference(key, data, defaultValue)
    }

    override fun getAll(): Map<String, *> = preferences

    /**
     * An in-memory [Preference] backed by [MutableStateFlow] for proper reactive [changes].
     *
     * Note: [set] updates the in-memory state only; nothing is persisted across sessions.
     */
    class InMemoryPreference<T>(
        private val key: String,
        initialData: T?,
        private val defaultValue: T,
    ) : Preference<T> {

        // Sentinel to distinguish "not set" from a null value
        private val _flow = MutableStateFlow<T?>(initialData)

        override fun key(): String = key

        override fun get(): T = _flow.value ?: defaultValue

        override fun isSet(): Boolean = _flow.value != null

        override fun delete() {
            _flow.value = null
        }

        override fun defaultValue(): T = defaultValue

        override fun changes(): Flow<T> {
            // Map null (deleted/unset) back to defaultValue for consumers
            return object : Flow<T> {
                override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<T>) {
                    _flow.collect { value ->
                        collector.emit(value ?: defaultValue)
                    }
                }
            }
        }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
            return changes().stateIn(scope, SharingStarted.Eagerly, get())
        }

        override fun set(value: T) {
            _flow.value = value
        }
    }
}