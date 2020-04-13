package me.emu6502.lib6502

import int
import minusSigned
import ubyte
import ushort

class RAM(size: UShort, start: UShort): Device(start, (start + size minusSigned 1).ushort) {

    override fun setData(data: UByte, address: UShort) {
        if (request(address))
            memory[(address - start).toInt()] = data;
    }

    override fun getData(address: UShort): UByte = if(request(address)) memory[(address - start).int] else 0.ubyte

    override fun performClockAction() = Unit

}