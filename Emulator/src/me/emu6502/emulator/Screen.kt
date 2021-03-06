package me.emu6502.emulator

import me.emu6502.lib6502.Device
import me.emu6502.kotlinutils.*
import java.awt.Color

open class Screen(width: Int, height: Int, startAddress: UShort): Device(startAddress, startAddress plusSigned 3) {

    init {
        memory[3] = 0x01.ubyte
    }
    val bitmapScreen = DirectBitmap(width, height)

    override fun getData(address: UShort) = if(request(address) && (address == end)) memory[3] else 0x00.ubyte

    override fun setData(data: UByte, address: UShort) {
        if (request(address))
            memory[(address - start).toInt()] = data
    }

    override fun performClockAction() {
        if(memory[3] == 0x02.ubyte) {
            // Also setting red value in case alpha is not displayed
            val v = memory[2].toInt()
            bitmapScreen[memory[0].toInt(), memory[1].toInt()] = Color(v, v, v)
            memory[3] = 0x01.ubyte
        }
    }

    open fun screenshot() = bitmapScreen.save("screen.bmp")

    open fun reset() {
        for (y in 0 until bitmapScreen.height)
            for (x in 0 until bitmapScreen.width)
                bitmapScreen[x, y] = Color.BLACK
    }

}