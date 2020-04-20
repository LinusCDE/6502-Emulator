package me.emu6502.lib6502

import me.emu6502.kotlinutils.toString
import me.emu6502.kotlinutils.ubyte
import java.lang.NumberFormatException

enum class AddressMode(val addressBytes: Int, val prefix: String, val suffix: String, val fixedValueLength: Int) {

    ABSOLUTE(2, "$", "", 4),
    ABSOLUTE_X(2,"$", ",X", 4),
    ABSOLUTE_Y(2,"$", ",Y", 4),
    ACCUMULATOR(0, "A", "", 0),
    IMMEDIATE(1,"#$", "", 2),
    INDIRECT(2,"($", ")", 4),
    INDEX_X(1,"($", ",X)", 2),
    INDEX_Y(1,"($", ",Y)", 4),
    ZEROPAGE(1,"$", "", 2),
    ZEROPAGE_X(1,"$", ",X", 2),
    ZEROPAGE_Y(1,"$", ",Y", 2);

    fun parse(operator: String): UByteArray? {
        if(!operator.startsWith(prefix, true) || !operator.endsWith(suffix, true))
            return null
        val rawValue = operator.substring(prefix.length, operator.length - suffix.length)
        if(rawValue.length != fixedValueLength)
            return null
        if(fixedValueLength % 2 != 0) // To have chunking work properly
            return null

        return try {
            // Get byte array of any size in reverse order
            rawValue.chunked(2).map { it.toInt(16).ubyte }.reversed().toUByteArray()
        }catch (e: NumberFormatException) { null }
    }

    fun toString(data: UByteArray): String? {
        if(data.size != addressBytes)
            return null
        return prefix + data.map { it.toString("X2") }.reversed().joinToString("") + suffix
    }

}