package me.emu6502.emulator.ui.controller

import javafx.beans.property.SimpleStringProperty
import me.emu6502.emulator.Emulator
import me.emu6502.kotlinutils.ushort
import me.emu6502.lib6502.AssembleException
import me.emu6502.lib6502.Disassembler
import me.emu6502.lib6502.enhancedassembler.EnhancedAssembler
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue
import java.io.File
import java.io.IOException
import java.lang.Exception

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

    fun onAssembleMovePcAndRunButtonPressed() {
        val memAddr = memoryAddress.toInt(16)
        runAsync {
            val (_, status) = mainController.emulator.assembleToMemory(sourceCode, memAddr)
            ui {
                statusMessage = "Assemble, move PC and run..."
                mainController.emulator.cpu.PC = memAddr.ushort
                mainController.emulator.executeDebuggerCommand("r")
            }
        }
    }

    fun onLoadSourcePressed(file: File?) {
        if(file == null)
            return

        sourceCode = file.readText()
        statusMessage = "Datei geladen"

        // Remove highligted on data in memory
        if(Emulator.Companion.MemoryTag.LAST_ASSEMBLY.visible) {
            Emulator.Companion.MemoryTag.LAST_ASSEMBLY.visible = false
            mainController.emulator.printStatus()
        }
    }

    fun onSaveSourcePressed(file: File?) {
        if(file == null)
            return
        file.writeText(sourceCode)
        statusMessage = "Datei gespeichert"
    }

    fun onSaveAssemblyToDisk(file: File?) {
        if(file == null)
            return

        try {
            file.writeBytes(EnhancedAssembler.assemble(sourceCode).toByteArray())
            statusMessage = "Assemblierte Datei gespeichert"
        }catch (e: AssembleException) {
            statusMessage = "Assemble-Fehler: ${e.message}"
        }catch (e: IOException) {
            statusMessage = "IO-Fehler: ${e.message}"
        }catch (e: Exception) {
        statusMessage = "Unerwarteter Fehler!"
        }
    }

    fun onLoadBinaryToMemoryPressed(file: File?) {
        if(file == null)
            return

        val bytes = file.readBytes()
        val memAddr = memoryAddress.toInt(16)
        bytes.withIndex().forEach { (index, byte) ->
            mainController.emulator.mainbus.setData(byte.toUByte(), (memAddr + index).ushort)
        }
        statusMessage = "${bytes.size} Bytes in Speicher geladen"

        // Visualize
        Emulator.Companion.MemoryTag.LAST_ASSEMBLY.memoryStart = memAddr.ushort
        Emulator.Companion.MemoryTag.LAST_ASSEMBLY.memoryEnd = (memAddr + bytes.size).ushort
        Emulator.Companion.MemoryTag.LAST_ASSEMBLY.visible = true
        mainController.emulator.printStatus()
    }

    fun onDisassemblePressed(file: File?) {
        if(file == null)
            return

        sourceCode = Disassembler.disassembleFully(file.readBytes().toUByteArray())
                .joinToString(System.lineSeparator())

        statusMessage = "Disassembliert"

        // Remove highligted on data in memory
        if(Emulator.Companion.MemoryTag.LAST_ASSEMBLY.visible) {
            Emulator.Companion.MemoryTag.LAST_ASSEMBLY.visible = false
            mainController.emulator.printStatus()
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