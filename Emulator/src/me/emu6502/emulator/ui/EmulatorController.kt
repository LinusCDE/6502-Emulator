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
import tornadofx.*
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
        mainController.emulator.executeDebuggerCommand(input)
        val command = input
        input = ""
        inputEnabled = false
        runAsync {
            mainController.emulator.executeDebuggerCommand(command)
        } ui {
            inputEnabled = true
        }
    }
}