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
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

internal sealed class IosPreference<T>(
    protected val delegate: NSUserDefaults,
    private val keyFlow: Flow<String>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract fun read(delegate: NSUserDefaults, key: String, defaultValue: T): T

    abstract fun write(delegate: NSUserDefaults, key: String, value: T)

    override fun key(): String = key

    override fun get(): T {
        return try {
            read(delegate, key, defaultValue)
        } catch (e: ClassCastException) {
            delete()
            defaultValue
        }
    }

    override fun set(value: T) = write(delegate, key, value)

    override fun isSet(): Boolean = delegate.objectForKey(key) != null

    override fun delete() {
        delegate.removeObjectForKey(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return keyFlow
            .filter { it == key }
            .onStart { emit("ignition") }
            .map { get() }
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }

    internal class StringPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: String,
    ) : IosPreference<String>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: String): String =
            delegate.stringForKey(key) ?: defaultValue

        override fun write(delegate: NSUserDefaults, key: String, value: String) =
            delegate.setObject(value, key)
    }

    internal class LongPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Long,
    ) : IosPreference<Long>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: Long): Long =
            if (isSet()) delegate.integerForKey(key) else defaultValue

        override fun write(delegate: NSUserDefaults, key: String, value: Long) =
            delegate.setInteger(value, key)
    }

    internal class IntPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Int,
    ) : IosPreference<Int>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: Int): Int =
            if (isSet()) delegate.integerForKey(key).toInt() else defaultValue

        override fun write(delegate: NSUserDefaults, key: String, value: Int) =
            delegate.setInteger(value.toLong(), key)
    }

    internal class FloatPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Float,
    ) : IosPreference<Float>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: Float): Float =
            if (isSet()) delegate.floatForKey(key) else defaultValue

        override fun write(delegate: NSUserDefaults, key: String, value: Float) =
            delegate.setFloat(value, key)
    }

    internal class BooleanPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Boolean,
    ) : IosPreference<Boolean>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: Boolean): Boolean =
            if (isSet()) delegate.boolForKey(key) else defaultValue

        override fun write(delegate: NSUserDefaults, key: String, value: Boolean) =
            delegate.setBool(value, key)
    }

    internal class StringSetPrimitive(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: Set<String>,
    ) : IosPreference<Set<String>>(delegate, keyFlow, key, defaultValue) {
        override fun read(
            delegate: NSUserDefaults,
            key: String,
            defaultValue: Set<String>
        ): Set<String> {
            val raw = delegate.stringForKey(key) ?: return defaultValue
            return if (raw.isEmpty()) emptySet() else raw.split(DELIMITER).toSet()
        }

        override fun write(delegate: NSUserDefaults, key: String, value: Set<String>) {
            delegate.setObject(value.joinToString(DELIMITER), key)
        }

        companion object {
            private const val DELIMITER = ","
        }
    }

    internal class ObjectAsString<T>(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : IosPreference<T>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: T): T {
            return try {
                delegate.stringForKey(key)?.let(deserializer) ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(delegate: NSUserDefaults, key: String, value: T) {
            delegate.setObject( serializer(value),key,)
        }
    }

    internal class ObjectAsInt<T>(
        delegate: NSUserDefaults,
        keyFlow: Flow<String>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T,
    ) : IosPreference<T>(delegate, keyFlow, key, defaultValue) {
        override fun read(delegate: NSUserDefaults, key: String, defaultValue: T): T {
            return try {
                if (isSet()) delegate.integerForKey(key).toInt()
                    .let(deserializer) else defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(delegate: NSUserDefaults, key: String, value: T) {
            delegate.setInteger(serializer(value).toLong(), key )
        }
    }

}

internal fun keyFlow(key: String, delegate: NSUserDefaults) = callbackFlow {
    var previousValue = delegate.objectForKey(key)
    val block = { _: NSNotification? ->
        val current = delegate.objectForKey(key)
        if (previousValue != current) {
            trySend(key)
            previousValue = current
        }
    }
    val observer = NSNotificationCenter.defaultCenter.addObserverForName(
        name = NSUserDefaultsDidChangeNotification,
        `object` = delegate,
        queue = null,
        usingBlock = block
    )

    awaitClose {
        NSNotificationCenter.defaultCenter.removeObserver(observer)
    }
}