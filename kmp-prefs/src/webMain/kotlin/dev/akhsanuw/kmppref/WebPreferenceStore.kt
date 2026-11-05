package dev.akhsanuw.kmppref

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge

class WebPreferenceStore(
    namespace: String = DEFAULT_NAMESPACE,
) : PreferenceStore {

    private val prefix = "$namespace."

    // Changes made through this store (same tab).
    private val localChanges = MutableSharedFlow<String?>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // Local changes + changes from other tabs/windows.
    private val keyFlow: Flow<String?> = merge(localChanges, StorageEventBus.events)

    private fun <T> create(
        key: String,
        defaultValue: T,
        encode: (T) -> String,
        decode: (String) -> T,
    ): Preference<T> = WebPreference(
        keyFlow = keyFlow,
        onChanged = { localChanges.tryEmit(it) },
        key = key,
        storageKey = prefix + key,
        defaultValue = defaultValue,
        encode = encode,
        decode = decode,
    )

    override fun getString(key: String, defaultValue: String): Preference<String> =
        create(key, defaultValue, encode = { it }, decode = { it })

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        create(key, defaultValue, encode = { it.toString() }, decode = { it.toLong() })

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
        create(key, defaultValue, encode = { it.toString() }, decode = { it.toInt() })

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        create(key, defaultValue, encode = { it.toString() }, decode = { it.toFloat() })

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        create(key, defaultValue, encode = { it.toString() }, decode = { it.toBooleanStrict() })

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        create(key, defaultValue, encode = StringSetCodec::encode, decode = StringSetCodec::decode)

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = create(key, defaultValue, encode = serializer, decode = deserializer)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = create(
        key = key,
        defaultValue = defaultValue,
        encode = { serializer(it).toString() },
        decode = { deserializer(it.toInt()) },
    )

    override fun getAll(): Map<String, *> = buildMap<String, Any?> {
        for (index in 0 until localStorage.length) {
            val storageKey = localStorage.key(index) ?: continue
            if (storageKey.startsWith(prefix)) {
                put(storageKey.removePrefix(prefix), localStorage.getItem(storageKey))
            }
        }
    }

    companion object {
        const val DEFAULT_NAMESPACE = "sidago"
    }
}


internal object StorageEventBus {
    val events = MutableSharedFlow<String?>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        window.addEventListener("storage") {
            events.tryEmit(null)
        }
    }
}