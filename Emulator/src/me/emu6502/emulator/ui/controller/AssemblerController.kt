package me.emu6502.emulator.ui.controller

import javafx.beans.property.SimpleStringProperty
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue

class AssemblerController: Controller() {
    val mainController: MainController by inject()

    val sourceCodeProperty = SimpleStringProperty("")
    var sourceCode by sourceCodeProperty

    val memoryAddressProperty = SimpleStringProperty("")
    var memoryAddress by memoryAddressProperty

    val statusMessageProperty = SimpleStringProperty("")
    var statusMessage by statusMessageProperty


    fun onAssembleButtonPressed() {
        val memAddr = memoryAddress.toInt(16)
        runAsync {
            val (_, status) = mainController.emulator.assembleToMemory(sourceCode, memAddr)
            ui { statusMessage = status }
        }
    }

    init {
        var addr = mainController.emulator.cpu.PC.toString(16).toUpperCase()
        while(addr.length < 4)
            addr = "0$addr"
        memoryAddress = addr

        memoryAddressProperty.onChange {
            if(it!!.length > 4)
                mainController.uiHandleAsync { memoryAddress = it.substring(0, 4) }
            val newText = it.filter { it in "0123456789ABCDEFabcdef" }
            if(newText != it)
                mainController.uiHandleAsync { memoryAddress = newText }
        }

        sourceCode = """
            LDA #€50
            STA €00
            LDA #€3C
            STA €01
            LDA #€FF
            STA €02
            JSR €F000
        """.trimIndent().replace("€", "$")
    }
}