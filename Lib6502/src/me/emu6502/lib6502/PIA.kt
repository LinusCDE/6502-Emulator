package me.emu6502.lib6502

import me.emu6502.kotlinutils.int
import me.emu6502.kotlinutils.plusSigned
import me.emu6502.kotlinutils.ubyte

class PIA(private val cpu: CPU, position: UShort): Device(position, position plusSigned 4) {

    var irq = false
    var outa = false
        private set
    var outb = false
        private set

    var porta: UByte = 0x00.ubyte
        get() =
            if(outa) {
                rdya = false
                field
            }else
                0x00.ubyte
        private set
    fun updatePorta(newPorta: UByte) {
        if(!outa) return
        porta = newPorta
        irq = true
    }

    var portb: UByte = 0x00.ubyte
        get() =
            if(outb) {
                rdyb = false
                field
            }else
                0x00.ubyte
        private set
    fun updatePortb(newPortb: UByte) {
        if(!outb) return
        portb = newPortb
        irq = true
    }

    var rdya = false
        private set
    fun updateRdya(newRdya: Boolean) {
        if(outa) return
        rdya = newRdya
        if(irq) cpu.IRQ = true
    }

    var rdyb = false
        private set
    fun updateRdyb(newRdyb: Boolean) {
        if(outb) return
        rdyb = newRdyb
        if(irq) cpu.IRQ = true
    }

    override fun setData(data: UByte, address: UShort) {
        if(!request(address)) return
        when((address - start).int) {
            0 -> { // PORTA
                if(outa) {
                    updatePorta(data)
                    updateRdya(true)
                }
            }
            1 -> { // PORTB
                if(outb) {
                    updatePortb(data)
                    updateRdyb(true)
                }
            }
            2 -> { //DDR (- - - - - - OUTB OUTA)
                outa = CPU.checkBit(data, 0)
                outb = CPU.checkBit(data, 1)
            }
            3 -> { //RDYR (- - - - - - RDYB RDYA)
                if(outa)
                    updateRdya(CPU.checkBit(data, 0))
                if(outb)
                    updateRdyb(CPU.checkBit(data, 1))
            }
        }
    }

    override fun getData(address: UShort): UByte {
        if(request(address)) return 0x00.ubyte
        return when((address - start).int) {
            0 -> { // PORTA
                if(!outa) rdya = false
                porta
            }
            1 -> { // PORTB
                if(!outb) rdyb = false
                portb
            }
            2 -> //DDR (- - - - - - OUTB OUTA)
                (((if(outb) 1 else 0) shl 1) + (if(outa) 1 else 0)).ubyte
            3 -> //RDYR (- - - - - - RDYB RDYA)
                (((if(rdyb) 1 else 0) shl 1) + (if(rdya) 1 else 0)).ubyte
            else -> 0x00.ubyte // Should never be reached! TODO: Error out
        }
    }

    override fun performClockAction() { }
}