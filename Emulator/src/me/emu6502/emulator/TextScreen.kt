package me.emu6502.emulator

import me.emu6502.kotlinutils.*
import java.io.DataInputStream
import java.io.FileInputStream

class TextScreen(width: Int, height: Int, startAddress: UShort): Screen(width, height, startAddress) {

    val charmap = UByteArray2D(128, 8)

    init {
        val din = DataInputStream(FileInputStream("apple1.vid"))
        for(i in 0 until 128)
            for(j in 0 until 8)
                charmap[i, j] = din.readByte().toUByte()
        din.close()
    }
}