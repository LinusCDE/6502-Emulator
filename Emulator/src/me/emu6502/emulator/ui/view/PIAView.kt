package me.emu6502.emulator.ui.view

import javafx.scene.Parent
import me.emu6502.emulator.ui.controller.MainController
import me.emu6502.kotlinutils.int
import me.emu6502.kotlinutils.ubyte
import tornadofx.*

class PIAView: View() {
    val controller: MainController by inject()

    val PORTA = Array(8) {
        checkbox("", controller.porta[it]) {
            disableProperty().bind(controller.outa)
            action {
                val pia = controller.emulator.pia
                val mask = (128 shr it)
                val newPorta = if(isSelected)
                    (pia.porta.int or mask).ubyte
                else
                    (pia.porta.int and mask.inv()).ubyte

                controller.emulator.pia.updatePorta(newPorta)
            }
        }
    }

    val PORTB = Array(8) {
        checkbox("", controller.portb[it]) {
            disableProperty().bind(controller.outb)
            action {
                val pia = controller.emulator.pia
                val mask = (128 shr it)
                val newPortb = if(isSelected)
                    (pia.portb.int or mask).ubyte
                else
                    (pia.portb.int and mask.inv()).ubyte

                controller.emulator.pia.updatePortb(newPortb)
            }
        }
    }

    override val root = vbox {
            label(controller.outa.stringBinding { if(it!!) "PORTA (Output)" else "PORTA (Input)" })
            hbox {
                for(cb in PORTA)
                    add(cb)
            }

            label(controller.outb.stringBinding { if(it!!) "PORTB (Output)" else "PORTB (Input)" })
            hbox {
                for(cb in PORTB)
                    add(cb)
            }
        }
}