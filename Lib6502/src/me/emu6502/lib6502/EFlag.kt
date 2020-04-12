package me.emu6502.lib6502

class EFlag {
    companion object {
        val NEG = 0b10000000.toUByte()
        val OVR = 0b01000000.toUByte()
        val BRK = 0b00010000.toUByte()
        val DEC = 0b00001000.toUByte()
        val IRQ = 0b00000100.toUByte()
        val ZER = 0b00000010.toUByte()
        val CAR = 0b00000001.toUByte()
    }
}