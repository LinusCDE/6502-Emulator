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
    var _porta = 0x00.ubyte
    var porta: UByte
        get() {
            if(!outa) return 0x00.ubyte
            _rdya = false
            return _porta
        }
        set(value) {
            if(outa) return
            _porta = value
            _rdya = true
        }

    var _portb = 0x00.ubyte
    var portb: UByte
        get() {
            if(!outb) return 0x00.ubyte
            _rdyb = false
            return _portb
        }
        set(value) {
            if(outb) return
            _portb = value
            _rdyb = true
        }

    var _rdya = false
    var rdya: Boolean
        get() = _rdya
        set(value) {
            if(outa) return
            _rdya = value
            if(irq) cpu.IRQ = true
        }
    var _rdyb = false
    var rdyb: Boolean
        get() = _rdyb
        set(value) {
            if(outb) return
            _rdyb = value
            if(irq) cpu.IRQ = true
        }

    fun inspectPorta() = _porta
    fun inspectPortb() = _portb


    override fun setData(data: UByte, address: UShort) {
        if(!request(address)) return
        when((address - start).int) {
            0 -> { // PORTA
                if(outa) {
                    _porta = data
                    _rdya = true
                }
            }
            1 -> { // PORTB
                if(outb) {
                    _portb = data
                    _rdyb = true
                }
            }
            2 -> { //DDR (- - - - - - OUTB OUTA)
                outa = CPU.checkBit(data, 0)
                outb = CPU.checkBit(data, 1)
            }
            3 -> { //RDYR (- - - - - - RDYB RDYA)
                if(outa)
                    _rdya = CPU.checkBit(data, 0)
                if(outb)
                    _rdyb = CPU.checkBit(data, 1)
            }
        }
    }

    override fun getData(address: UShort): UByte {
        if(request(address)) return 0x00.ubyte
        return when((address - start).int) {
            0 -> { // PORTA
                if(!outa) _rdya = false
                _porta
            }
            1 -> { // PORTB
                if(!outb) _rdyb = false
                _portb
            }
            2 -> //DDR (- - - - - - OUTB OUTA)
                (((if(outb) 1 else 0) shl 1) + (if(outa) 1 else 0)).ubyte
            3 -> //RDYR (- - - - - - RDYB RDYA)
                (((if(_rdyb) 1 else 0) shl 1) + (if(_rdya) 1 else 0)).ubyte
            else -> 0x00.ubyte // Should never be reached! TODO: Error out
        }
    }

    override fun performClockAction() { }
}