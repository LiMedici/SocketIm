package com.mrmedici.clink.utils

object ByteUtils {
    /**
     * Does this byte array begin with match array content?
     *
     * @param source Byte array to examine
     * @param match  Byte array to locate in `source`
     * @return true If the starting bytes are equal
     */
    fun startsWith(source: ByteArray, match: ByteArray): Boolean {
        return startsWith(source, 0, match)
    }

    /**
     * Does this byte array begin with match array content?
     *
     * @param source Byte array to examine
     * @param offset An offset into the `source` array
     * @param match  Byte array to locate in `source`
     * @return true If the starting bytes are equal
     */
    fun startsWith(source: ByteArray, offset: Int, match: ByteArray): Boolean {

        if (match.size > source.size - offset) {
            return false
        }

        for (i in match.indices) {
            if (source[offset + i] != match[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Does the source array equal the match array?
     *
     * @param source Byte array to examine
     * @param match  Byte array to locate in `source`
     * @return true If the two arrays are equal
     */
    fun equals(source: ByteArray, match: ByteArray): Boolean {

        return if (match.size != source.size) {
            false
        } else startsWith(source, 0, match)
    }

    /**
     * Copies bytes from the source byte array to the destination array
     *
     * @param source      The source array
     * @param srcBegin    Index of the first source byte to copy
     * @param srcEnd      Index after the last source byte to copy
     * @param destination The destination array
     * @param dstBegin    The starting offset in the destination array
     */
    fun getBytes(source: ByteArray, srcBegin: Int, srcEnd: Int, destination: ByteArray,
                 dstBegin: Int) {
        System.arraycopy(source, srcBegin, destination, dstBegin, srcEnd - srcBegin)
    }

    /**
     * Return a new byte array containing a sub-portion of the source array
     *
     * @param srcBegin The beginning index (inclusive)
     * @param srcEnd   The ending index (exclusive)
     * @return The new, populated byte array
     */
    fun subbytes(source: ByteArray, srcBegin: Int, srcEnd: Int = source.size): ByteArray {
        val destination = ByteArray(srcEnd - srcBegin)
        getBytes(source, srcBegin, srcEnd, destination, 0)

        return destination
    }

}