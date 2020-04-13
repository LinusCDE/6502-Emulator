class UByteArray2D(val width: Int, val height: Int) {

    val bytes = UByteArray(width * height)

    private fun map(x: Int, y: Int): Int = (y * width) + x

    operator fun get(x: Int, y: Int): UByte = bytes[map(x, y)]
    operator fun set(x: Int, y: Int, value: UByte) { bytes[map(x, y)] = value }

}