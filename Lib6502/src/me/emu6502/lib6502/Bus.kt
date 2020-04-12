package me.emu6502.lib6502

import ubyte
import ushort

class Bus {

    val devices = arrayListOf<Device>()

    fun getData(address: UShort): UByte = devices.firstOrNull { it.request(address) }?.getData(address) ?: 0.ubyte

    fun setData(data: UByte, address: UShort) = devices.firstOrNull { it.request(address) }?.setData(data, address)

    fun setData(data: UByte, address: UInt) = setData(data, address.ushort)

    fun setData(data: UByte, address: UByte) = setData(data, address.ushort)

    fun getData(address: UInt): UByte = getData(address.ushort);

    fun getData(address: UByte): UByte = getData(address.ushort);

    fun performClockActions() = devices.forEach { it.performClockAction() }

}