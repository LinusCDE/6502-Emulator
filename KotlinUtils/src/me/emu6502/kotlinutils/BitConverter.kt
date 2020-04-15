package me.emu6502.kotlinutils

class BitConverter  {
    companion object {
        fun IsLittleEndian() = true

        fun GetBytes(value: UShort): UByteArray =
                ubyteArrayOf(
                        (value and 0xFF.ushort).ubyte,
                        ((value shiftRight 8) and 0xFF.uint).ubyte
                )
    }
}