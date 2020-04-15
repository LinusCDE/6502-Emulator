package me.emu6502.kotlinutils

import java.lang.RuntimeException

val UInt.ubyte
    get() = this.toUByte()

val UInt.ushort
    get() = this.toUShort()

val UShort.ubyte
    get() = this.toUByte()

val UShort.uint
    get() = this.toUInt()

val UByte.ushort
    get() = this.toUShort()

val UByte.uint
    get() = this.toUInt()

val Int.ubyte
    get() = this.toUByte()

val Int.ushort
    get() = this.toUShort()

val Int.uint
    get() = this.toUInt()

val UInt.int
    get() = this.toInt()

val UShort.int
    get() = this.toInt()

val UByte.int
    get() = this.toInt()

infix fun UByte.shiftLeft(bits: Int): UInt = this.int.shl(bits).uint
infix fun UByte.shiftRight(bits: Int): UInt = this.int.shr(bits).uint

infix fun UShort.shiftLeft(bits: Int): UInt = this.int.shl(bits).uint
infix fun UShort.shiftRight(bits: Int): UInt = this.int.shr(bits).uint

infix fun UInt.shiftLeft(bits: Int): UInt = this.int.shl(bits).uint
infix fun UInt.shiftRight(bits: Int): UInt = this.int.shr(bits).uint

infix fun UInt.plusSigned(other: Int): UInt = this + other.uint
infix fun UShort.plusSigned(other: Int): UShort = (this + other.ushort).ushort
infix fun UByte.plusSigned(other: Int): UByte = (this + other.ubyte).ubyte

infix fun UInt.minusSigned(other: Int): UInt = this - other.uint
infix fun UShort.minusSigned(other: Int): UShort = (this - other.ushort).ushort
infix fun UByte.minusSigned(other: Int): UByte = (this - other.ubyte).ubyte

fun UByte.toString(format: String): String = this.uint.toString(format)
fun UShort.toString(format: String): String = this.uint.toString(format)
fun Int.toString(format: String): String = this.uint.toString(format)

fun ByteArray.toUByteArray() = UByteArray(this.size) { this[it].toUByte() }

fun UInt.toString(format: String): String {
    val radix = when(format[0].toUpperCase()) {
        'X' -> 16
        'O' -> 8
        'B' -> 2
        else -> 10
    }

    val fixedLength = if(radix == 10) format.toInt() else format.substring(1).toInt();

    var str = this.toString(radix)
    if(format[0].isLetter() && format[0].isUpperCase())
        str = str.toUpperCase()
    else
        str = str.toLowerCase()

    if(str.length > fixedLength)
        throw RuntimeException("Number $this is bigger than $fixedLength chars in base $radix")
    while(str.length < fixedLength)
        str = "0$str"

    return str
}