package me.emu6502.emulator.ui

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import me.emu6502.emulator.Console
import me.emu6502.emulator.Emulator
import tornadofx.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmulatorController: Controller() {
    lateinit var emulator: Emulator

    val console: EmulatorConsoleView by inject()
    val emulatorThread = Thread {
        emulator = Emulator(
                clear = { Platform.runLater { console.clear() } },
                write = { text -> Platform.runLater { console.write(text) } },
                writeLine = { text -> Platform.runLater { console.writeLine(text) } },
                defineCommand = { name, displayName, desc -> Console.completer.addCommand(name, displayName, desc) },
                requestCommand = { console.asyncReqeuestCommand() },
                requestRawInput = { console.asyncRquestRawInput() },
                reportError = {
                    asyncBlockingUi { alert(Alert.AlertType.ERROR, "Emulator-Fehler", it, ButtonType.OK) }
                },
                updateScreen = { it.screenshot() }
        )
    }

    fun asyncBlockingUi(task: () -> Unit) {
        val lock = ReentrantLock()
        val cond = lock.newCondition()
        Platform.runLater {
            task()
            lock.withLock { cond.signal() }
        }
        lock.withLock { cond.await() }
    }

    init {
        emulatorThread.start()
    }
}