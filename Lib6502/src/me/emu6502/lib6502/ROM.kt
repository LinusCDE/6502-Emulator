package me.emu6502.lib6502

import int
import ushort
import minus
import ubyte
import java.lang.IllegalArgumentException

class ROM(size: UShort, start: UShort): Device(start, (start + size - 1).ushort) {

    fun setMemory(mem: UByteArray) {
        if (mem.size != memory.size)
            throw IllegalArgumentException("Wrong size of mem");

        for(i in mem.indices) {
            memory[i] = mem[i];
        }
    }

    override fun setData(data: UByte, address: UShort) {
        System.err.println("WARN: Attempt to write to ROM at $address!!! Write ignored!")
        // TODO: Add exception (or does the hardware forgive this?)
    }

    override fun getData(address: UShort): UByte = if(request(address)) memory[(address - start).int] else 0.ubyte

    override fun performClockAction() = Unit

}