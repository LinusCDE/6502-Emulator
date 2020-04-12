class BitConverter  {
    companion object {
        fun IsLittleEndian() = true

        fun GetBytes(value: UShort): UByteArray =
                ubyteArrayOf(
                        (value and 0xFF.ushort).ubyte,
                        ((value shr 8) and 0xFF.ushort).ubyte
                )
    }
}