package dev.akhsanuw.kmppref

import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

internal class WebPreference<T>(
    private val keyFlow: Flow<String?>,
    private val onChanged: (String) -> Unit,
    private val key: String,
    private val storageKey: String,
    private val defaultValue: T,
    private val encode: (T) -> String,
    private val decode: (String) -> T,
) : Preference<T> {

    override fun key(): String = key

    override fun get(): T {
        val raw = localStorage.getItem(storageKey) ?: return defaultValue
        return try {
            decode(raw)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override fun set(value: T) {
        localStorage.setItem(storageKey, encode(value))
        onChanged(key)
    }

    override fun isSet(): Boolean = localStorage.getItem(storageKey) != null

    override fun delete() {
        localStorage.removeItem(storageKey)
        onChanged(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return keyFlow
            .filter { it == key || it == null }
            .onStart { emit(null) } // initial emission
            .map { get() }
            // The `storage` event doesn't carry our un-prefixed key (we emit null),
            // so skip emissions where our own value didn't actually change.
            .distinctUntilChanged()
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }
}

/**
 * Length-prefixed encoding for `Set<String>` (e.g. `{"ab", "", "c,d"}` -> `2:ab0:3:c,d`).
 * Unambiguous for any content (commas, empty strings, empty set), unlike delimiter-based encodings.
 * Lengths are counted in UTF-16 code units, which is the same on every Kotlin platform.
 */
internal object StringSetCodec {
    fun encode(value: Set<String>): String = buildString {
        for (item in value) {
            append(item.length.toString())
            append(':')
            append(item)
        }
    }

    fun decode(raw: String): Set<String> {
        val result = LinkedHashSet<String>()
        var index = 0
        while (index < raw.length) {
            val colon = raw.indexOf(':', index)
            require(colon > index) { "Malformed string set" }
            val length = raw.substring(index, colon).toInt()
            val start = colon + 1
            val end = start + length
            require(length >= 0 && end <= raw.length) { "Malformed string set" }
            result += raw.substring(start, end)
            index = end
        }
        return result
    }
}