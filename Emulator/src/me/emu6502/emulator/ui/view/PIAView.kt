package me.emu6502.emulator.ui.view

import javafx.scene.Parent
import me.emu6502.emulator.ui.controller.MainController
import me.emu6502.kotlinutils.int
import me.emu6502.kotlinutils.toString
import me.emu6502.kotlinutils.ubyte
import tornadofx.*

class PIAView: View() {
    val controller: MainController by inject()

    val PORTA = Array(8) {
        checkbox("", controller.porta[it]) {
            disableProperty().bind(controller.outa)
        }
    }

    val PORTB = Array(8) {
        checkbox("", controller.portb[it]) {
            disableProperty().bind(controller.outb)
        }
    }

    override val root = vbox {
            label(controller.outa.stringBinding { if(it!!) "PORTA (Output)" else "PORTA (Input)" })
            hbox {
                for(cb in PORTA)
                    add(cb)
                button("Set Port A") {
                    disableProperty().bind(controller.outa)
                    action {
                        var newPortA = 0.ubyte
                        for(i in PORTA.indices) {
                            if(PORTA[i].isSelected)
                                newPortA = (newPortA or (1 shl (7 - i)).ubyte)
                        }
                        controller.emulator.pia.porta = newPortA
                        controller.uiHandleAsync { controller.emulator.updatePia(controller.emulator.pia) }
                    }
                }
            }

            label(controller.outb.stringBinding { if(it!!) "PORTB (Output)" else "PORTB (Input)" })
            hbox {
                for(cb in PORTB)
                    add(cb)
                button("Set Port B") {
                    disableProperty().bind(controller.outb)
                    action {
                        var newPortB = 0.ubyte
                        for(i in PORTB.indices) {
                            if(PORTB[i].isSelected)
                                newPortB = (newPortB or (1 shl (7 - i)).ubyte)
                        }
                        controller.emulator.pia.portb = newPortB
                        controller.uiHandleAsync { controller.emulator.updatePia(controller.emulator.pia) }
                    }
                }
            }
        }
}