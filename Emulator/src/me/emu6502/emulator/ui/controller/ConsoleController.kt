package me.emu6502.emulator.ui.controller

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import me.emu6502.kotlinutils.vt100.VT100Sequence
import me.emu6502.lib6502.EmulationException
import tornadofx.Controller
import tornadofx.alert
import tornadofx.getValue
import tornadofx.setValue
import java.lang.StringBuilder

class ConsoleController: Controller() {
    val mainController: MainController by inject()

    val inputEnabledProperty = SimpleBooleanProperty(true)
    var inputEnabled by inputEnabledProperty

    val inputProperty = SimpleStringProperty("")
    var input by inputProperty

    var writeFunc: ((text: String) -> Unit)? = null

    fun clear() {
        write("${VT100Sequence.CURSOR_HOME}${VT100Sequence.ERASE_DOWN}")
    }

    var outputPreConnectCache: StringBuilder? = StringBuilder()

    fun write(text: String) {
        if(writeFunc != null)
            writeFunc!!(text)
        else if(outputPreConnectCache != null) {
            // The view will write this as soon as it sets the writerFunc
            outputPreConnectCache?.append(text)
        }
    }

    fun writeLine(text: String) = write("$text\n")

    fun onInputEnterPressed() {
        val command = input
        input = ""
        inputEnabled = false
        runAsync {
            try {
                mainController.emulator.executeDebuggerCommand(command)
            }catch (e: EmulationException) {
                ui {
                    alert(Alert.AlertType.ERROR, "CPU Emulation Error!", e.message, ButtonType.OK)
                }
            }
        } ui {
            inputEnabled = true
        }
    }
}
