package me.emu6502.emulator.ui.controller

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
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
import java.awt.image.BufferedImage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainController: Controller() {
    val commands = FXCollections.observableArrayList<CommandInfo>()
    val screenImageProperty = SimpleObjectProperty<Image>()
    var screenImage by screenImageProperty
    val textScreenImageProperty = SimpleObjectProperty<Image>()
    var textScreenImage by textScreenImageProperty

    val console: ConsoleController by inject()

    val porta = Array(8) { SimpleBooleanProperty(false) }
    val outa = SimpleBooleanProperty(false)
    val portb = Array(8) { SimpleBooleanProperty(false) }
    val outb = SimpleBooleanProperty(false)

    private var lastScreenUpdateCounter = -1L
    private var lastTextScreenUpdateCounter = -1L

    val isAssemblerViewInOwnWindowProperty = SimpleBooleanProperty(false)
    var isAssemblerViewInOwnWindow by isAssemblerViewInOwnWindowProperty


    val emulator = Emulator(
            clear = {  console.clear() },
            write = { text -> console.write(text) },
            writeLine = { text -> console.writeLine(text) },
            defineCommand = { _, displayName, desc -> commands.add(CommandInfo(displayName, desc)) },
            reportError = {
                uiHandleSync { alert(Alert.AlertType.ERROR, "Emulator-Fehler", it, ButtonType.OK) }
            },
            updateScreen = {
                if(lastScreenUpdateCounter != it.bitmapScreen.updateCounter) {
                    uiHandleAsync {
                        lastScreenUpdateCounter = it.bitmapScreen.updateCounter
                        val scaled = it.bitmapScreen.createScaled(it.bitmapScreen.width * 2, it.bitmapScreen.height * 2, BufferedImage.SCALE_FAST)
                        screenImage = SwingFXUtils.toFXImage(scaled, null)
                    }
                }
            },
            updateTextScreen = {
                if(lastTextScreenUpdateCounter != it.bitmapScreen.updateCounter) {
                    uiHandleAsync {
                        lastTextScreenUpdateCounter = it.bitmapScreen.updateCounter
                        textScreenImage = SwingFXUtils.toFXImage(it.bitmapScreen.image, null)
                    }
                }
            },
            updatePia = { pia ->
                uiHandleAsync {
                    for(bit in 0 until 8) {
                        porta[7 - bit].set(pia.inspectPorta().int and (1 shl bit) > 0)
                        portb[7 - bit].set(pia.inspectPortb().int and (1 shl bit) > 0)
                    }
                    outa.set(pia.outa)
                    outb.set(pia.outb)
                }
            }
    )

    val emulationSyncsPerSecondProperty = SimpleIntegerProperty(emulator.syncsPerSecond).apply {
        onChange { emulator.syncsPerSecond = it }
    }
    var emulationSyncsPerSecond by emulationSyncsPerSecondProperty


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
        emulationSyncsPerSecond = 10
    }

}