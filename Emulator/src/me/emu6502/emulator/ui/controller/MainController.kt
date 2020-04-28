package me.emu6502.emulator.ui.controller

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import me.emu6502.emulator.Emulator
import me.emu6502.emulator.ui.CommandInfo
import me.emu6502.kotlinutils.int
import tornadofx.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainController: Controller() {
    val commands = FXCollections.observableArrayList<CommandInfo>()
    val screenImageProperty = SimpleObjectProperty<Image>()
    var screenImage by screenImageProperty

    val console: ConsoleController by inject()

    val porta = Array(8) { SimpleBooleanProperty(false) }
    val outa = SimpleBooleanProperty(false)
    val portb = Array(8) { SimpleBooleanProperty(false) }
    val outb = SimpleBooleanProperty(false)

    val emulator = Emulator(
            clear = {  console.clear() },
            write = { text -> console.write(text) },
            writeLine = { text -> console.writeLine(text) },
            defineCommand = { _, displayName, desc -> commands.add(CommandInfo(displayName, desc)) },
            reportError = {
                uiHandleSync { alert(Alert.AlertType.ERROR, "Emulator-Fehler", it, ButtonType.OK) }
            },
            updateScreen = {
                uiHandleAsync {
                    screenImage = SwingFXUtils.toFXImage(it.bitmapScreen.image, null)
                }
            },
            updatePia = { pia ->
                uiHandleAsync {
                    for(bit in 0 until 8) {
                        porta[7 - bit].set(pia.porta.int and (1 shl bit) > 0)
                        portb[7 - bit].set(pia.portb.int and (1 shl bit) > 0)
                    }
                    outa.set(pia.outa)
                    outb.set(pia.outb)
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