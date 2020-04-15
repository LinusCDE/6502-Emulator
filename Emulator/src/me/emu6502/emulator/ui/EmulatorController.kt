package me.emu6502.emulator.ui

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import me.emu6502.emulator.Emulator
import tornadofx.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmulatorController: Controller() {
    lateinit var emulator: Emulator

    val window: EmulatorWindow by inject()
    val console: EmulatorConsoleView by inject()

    val emulatorThread = Thread {
        emulator = Emulator(
                clear = { uiHandleAsync { console.clear() } },
                write = { text -> uiHandleAsync { console.write(text) } },
                writeLine = { text -> uiHandleAsync { console.writeLine(text) } },
                defineCommand = { _, displayName, desc -> window.commands.add(CommandInfo(displayName, desc)) },
                requestCommand = { console.asyncReqeuestCommand() },
                requestRawInput = { console.asyncRquestRawInput() },
                reportError = {
                    uiHandleSync { alert(Alert.AlertType.ERROR, "Emulator-Fehler", it, ButtonType.OK) }
                },
                updateScreen = {
                    uiHandleAsync {
                        window.screenImage.value = SwingFXUtils.toFXImage(it.bitmapScreen.image, null)
                    }
                }
        )
    }

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
        emulatorThread.start()
    }
}