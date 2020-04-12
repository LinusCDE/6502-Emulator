package me.emu6502.lib6502

abstract class Device(val start: UShort, val end: UShort) {

    val memory: UByteArray = UByteArray(end.toInt() - start.toInt() + 1);

    fun request(address: UShort): Boolean = (address >= start) && (address <= end);

    abstract fun setData(data: UByte, address: UShort);

    abstract fun getData(address: UShort): UByte;

    abstract fun performClockAction();

}