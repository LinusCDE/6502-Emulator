package me.emu6502.emulator

import me.emu6502.kotlinutils.*
import java.awt.Color
import java.io.DataInputStream
import java.io.FileInputStream

class TextScreen(width: Int, height: Int, startAddress: UShort): Screen(width*8, height*8, startAddress) {

    val charmap = UByteArray2D(128, 8)
    val foregroundColor = Color(0x7b71d5)
    val backgroundColor = Color(0x4130a4)

    init {
        reset()
        val din = DataInputStream(FileInputStream("apple1.vid"))
        for(i in 0 until 128)
            for(j in 0 until 8)
                charmap[i, j] = din.readByte().toUByte()
        din.close()
    }

    override fun performClockAction() {
        if (memory[3] == 0x02.ubyte) {

            try {
                for (j in 0 until 8) {
                    for (k in 1 until 8) {
                        val isLit = (charmap[memory[2].int, j].int and (1 shl k)) == (1 shl k)
                        bitmapScreen[memory[0].int * 8 + k, memory[1].int * 8 + j] = if (isLit) foregroundColor else backgroundColor
                    }
                }
            }catch (e: ArrayIndexOutOfBoundsException) { } // Ignore invalid characters

            memory[3] = 0x01.ubyte;
        }
    }

    override fun screenshot() = bitmapScreen.save("textscreen.bmp")

    override fun reset() {
        for (y in 0 until bitmapScreen.height)
            for (x in 0 until bitmapScreen.width)
                bitmapScreen[x, y] = backgroundColor
    }
}