package me.vickychijwani.spectre.util

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import java.io.Serializable
import java.util.*

/**
 * Fluent API for [android.os.Bundle]
 * `Bundle bundle = new BundleBuilder().put(....).put(....).get();`
 */
class BundleBuilder {

    private val bundle: Bundle = Bundle()

    fun put(key: String, value: Boolean): BundleBuilder {
        bundle.putBoolean(key, value)
        return this
    }

    fun put(key: String, value: BooleanArray): BundleBuilder {
        bundle.putBooleanArray(key, value)
        return this
    }

    fun put(key: String, value: Int): BundleBuilder {
        bundle.putInt(key, value)
        return this
    }

    fun put(key: String, value: IntArray): BundleBuilder {
        bundle.putIntArray(key, value)
        return this
    }

    fun putIntegerArrayList(key: String, value: ArrayList<Int>): BundleBuilder {
        bundle.putIntegerArrayList(key, value)
        return this
    }

    fun put(key: String, value: Bundle): BundleBuilder {
        bundle.putBundle(key, value)
        return this
    }

    fun put(key: String, value: Byte): BundleBuilder {
        bundle.putByte(key, value)
        return this
    }

    fun put(key: String, value: ByteArray): BundleBuilder {
        bundle.putByteArray(key, value)
        return this
    }

    fun put(key: String, value: String): BundleBuilder {
        bundle.putString(key, value)
        return this
    }

    fun put(key: String, value: Array<String>): BundleBuilder {
        bundle.putStringArray(key, value)
        return this
    }

    fun putStringArrayList(key: String, value: ArrayList<String>): BundleBuilder {
        bundle.putStringArrayList(key, value)
        return this
    }

    fun put(key: String, value: Long): BundleBuilder {
        bundle.putLong(key, value)
        return this
    }

    fun put(key: String, value: LongArray): BundleBuilder {
        bundle.putLongArray(key, value)
        return this
    }

    fun put(key: String, value: Float): BundleBuilder {
        bundle.putFloat(key, value)
        return this
    }

    fun put(key: String, value: FloatArray): BundleBuilder {
        bundle.putFloatArray(key, value)
        return this
    }

    fun put(key: String, value: Char): BundleBuilder {
        bundle.putChar(key, value)
        return this
    }

    fun put(key: String, value: CharArray): BundleBuilder {
        bundle.putCharArray(key, value)
        return this
    }

    fun put(key: String, value: CharSequence): BundleBuilder {
        bundle.putCharSequence(key, value)
        return this
    }

    fun put(key: String, value: Array<CharSequence>): BundleBuilder {
        bundle.putCharSequenceArray(key, value)
        return this
    }

    fun putCharSequenceArrayList(key: String, value: ArrayList<CharSequence>): BundleBuilder {
        bundle.putCharSequenceArrayList(key, value)
        return this
    }

    fun put(key: String, value: Double): BundleBuilder {
        bundle.putDouble(key, value)
        return this
    }

    fun put(key: String, value: DoubleArray): BundleBuilder {
        bundle.putDoubleArray(key, value)
        return this
    }

    fun put(key: String, value: Parcelable): BundleBuilder {
        bundle.putParcelable(key, value)
        return this
    }

    fun put(key: String, value: Array<Parcelable>): BundleBuilder {
        bundle.putParcelableArray(key, value)
        return this
    }

    fun putParcelableArrayList(key: String, value: ArrayList<out Parcelable>): BundleBuilder {
        bundle.putParcelableArrayList(key, value)
        return this
    }

    fun putSparseParcelableArray(key: String, value: SparseArray<out Parcelable>): BundleBuilder {
        bundle.putSparseParcelableArray(key, value)
        return this
    }

    fun put(key: String, value: Short): BundleBuilder {
        bundle.putShort(key, value)
        return this
    }

    fun put(key: String, value: ShortArray): BundleBuilder {
        bundle.putShortArray(key, value)
        return this
    }

    fun put(key: String, value: Serializable): BundleBuilder {
        bundle.putSerializable(key, value)
        return this
    }

    fun putAll(map: Bundle): BundleBuilder {
        bundle.putAll(map)
        return this
    }

    /**
     * Get the underlying bundle.
     */
    fun build(): Bundle {
        return bundle
    }

    companion object {
        /**
         * Initialize a BundleBuilder that is copied form the given bundle. The bundle that is passed will not be modified.
         */
        fun copyFrom(bundle: Bundle): BundleBuilder {
            return BundleBuilder().putAll(bundle)
        }
    }

}
