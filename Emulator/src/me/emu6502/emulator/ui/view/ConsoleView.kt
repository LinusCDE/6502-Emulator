package me.emu6502.emulator.ui.view

import javafx.geometry.Pos
import javafx.scene.Parent
import me.emu6502.emulator.ui.controller.ConsoleController
import tornadofx.*

class ConsoleView: View() {

    val controller: ConsoleController by inject()

    override val root: Parent = borderpane {
        center = BasicVT100View(135, 38).apply {
            controller.writeFunc = { write(it) }
            controller.writeFunc!!(controller.outputPreConnectCache.toString())
            controller.outputPreConnectCache = null
        }

        bottom = borderpane {
            left = hbox {
                label("> ")
                alignment = Pos.CENTER
            }
            center = textfield(controller.inputProperty) {
                editableProperty().bind(controller.inputEnabledProperty)
                action { controller.onInputEnterPressed() }
            }
            right = button {
                disableProperty().bind(controller.inputEnabledProperty.booleanBinding { !it!! })
                textProperty().bind(controller.inputProperty.stringBinding { if(it == "") "Step" else "Send" })
                action { controller.onInputEnterPressed() }
            }
        }
    }

}