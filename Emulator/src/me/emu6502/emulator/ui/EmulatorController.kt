package me.emu6502.emulator.ui

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import me.emu6502.emulator.Emulator
import me.emu6502.kotlinutils.ubyte
import me.emu6502.kotlinutils.ushort
import me.emu6502.lib6502.AssembleException
import me.emu6502.lib6502.Assembler
import me.emu6502.lib6502.EmulationException
import tornadofx.*
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmulatorController: Controller() {
    val commands = FXCollections.observableArrayList<CommandInfo>()
    val screenImageProperty = SimpleObjectProperty<Image>()
    var screenImage by screenImageProperty

    val console: EmulatorConsoleController by inject()

    val emulator = Emulator(
            clear = {  uiHandleAsync { console.clear() } },
            write = { text ->  uiHandleAsync { console.write(text) } },
            writeLine = { text ->  uiHandleAsync { console.writeLine(text) } },
            defineCommand = { _, displayName, desc -> commands.add(CommandInfo(displayName, desc)) },
            reportError = {
                uiHandleSync { alert(Alert.AlertType.ERROR, "Emulator-Fehler", it, ButtonType.OK) }
            },
            updateScreen = {
                uiHandleAsync {
                    screenImage = SwingFXUtils.toFXImage(it.bitmapScreen.image, null)
                }
            }
    )

    fun uiHandleSync(task: () -> Unit) {
        val lock = ReentrantLock()
        val cond = lock.newCondition()
        uiHandleAsync {
            task()
            lock.withLock { cond.signal() }
        }
        lock.withLock { cond.await() }
    }

    fun uiHandleAsync(task: () -> Unit) = Platform.runLater(task)

    init {
        emulator.printStatus()
    }

}

class EmulatorConsoleController: Controller() {
    val mainController: EmulatorController by inject()

    val outputProperty = SimpleStringProperty("")
    var output by outputProperty

    val inputProperty = SimpleStringProperty("")
    var input by inputProperty

    val inputEnabledProperty = SimpleBooleanProperty(true)
    var inputEnabled by inputEnabledProperty

    fun clear() { output = "" }
    fun write(text: String) { output += text }
    fun writeLine(text: String) { output += "$text\n" }

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

class AssemblerController: Controller() {
    val mainController: EmulatorController by inject()

    var lastMemoryAddress = -1
    var lastProgramSize = 0

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