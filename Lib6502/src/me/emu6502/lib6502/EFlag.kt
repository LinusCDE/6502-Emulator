package me.emu6502.lib6502

import me.emu6502.kotlinutils.*

class EFlag {
    companion object {

        val NEG = 0b10000000.ubyte
        val OVR = 0b01000000.ubyte
        val BRK = 0b00010000.ubyte
        val DEC = 0b00001000.ubyte
        val IRQ = 0b00000100.ubyte
        val ZER = 0b00000010.ubyte
        val CAR = 0b00000001.ubyte

    }
}