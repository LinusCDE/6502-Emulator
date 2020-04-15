package me.emu6502.emulator.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.CacheHint
import javafx.scene.Parent
import javafx.scene.image.Image
import org.jline.reader.Candidate
import tornadofx.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmulatorApp: App(EmulatorWindow::class)

class CommandInfo(name: String = "", description: String = "") {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    val descriptionProperty = SimpleStringProperty(description)
    var description by descriptionProperty
}

class EmulatorWindow: View() {
    val controller: EmulatorController = find<EmulatorController>()
    val screenImage = SimpleObjectProperty<Image>()
    val commands = FXCollections.observableArrayList<CommandInfo>()

    override val root: Parent = hbox {
        add(find(EmulatorConsoleView::class))
        vbox {
            label("Screen:")
            imageview {
                val scale = 2.0
                fitWidth = 140 * scale
                fitHeight = 120 * scale
                isPreserveRatio = true
                imageProperty().bind(screenImage)
            }
            label("Befehle:")
            tableview(commands) {
                column("Name", CommandInfo::nameProperty)
                column("Beschreibung", CommandInfo::descriptionProperty)
            }
        }
    }
}

class EmulatorConsoleView: View() {
    val controller: EmulatorController by inject()

    val output = textarea() {
        isEditable = false
        isWrapText = true
        style {
            prefColumnCount = 134
            prefRowCount = 38
            fontFamily = "monospaced"
        }
    }

    val input = SimpleStringProperty("")
    val inputRequested = SimpleBooleanProperty(false)
    val commandRequested = SimpleBooleanProperty(false)
    val submitLock = ReentrantLock()
    val submitCond = submitLock.newCondition()

    override val root: Parent = borderpane {
        center = output
        bottom = borderpane {
            left = hbox {
                label {
                    textProperty().bind(commandRequested.stringBinding { if (it!!) "> " else "" })
                }
                alignment = Pos.CENTER
            }
            center = textfield(input) {
                editableProperty().bind(inputRequested)

                action {
                    synchronized(inputRequested) {
                        submitLock.withLock {
                            submitCond.signal()
                        }
                    }
                }
            }
            right = button {
                disableProperty().bind(inputRequested.booleanBinding { !it!! })
                textProperty().bind(input.stringBinding { if(it == "") "Step" else "Send" })

                action {
                    synchronized(inputRequested) {
                        submitLock.withLock {
                            submitCond.signal()
                        }
                    }
                }
            }
        }
    }

    fun clear() { output.text = "" }
    fun write(text: String) { output.text += text }
    fun writeLine(text: String) { output.text += "$text\n" }
    fun asyncReqeuestCommand(): String = asyncRequest(true)
    fun asyncRquestRawInput(): String = asyncRequest(false)

    private fun asyncRequest(command: Boolean): String {
        // Unblock textfield and button and wait for that
        controller.uiHandleSync {
            commandRequested.value = command
            synchronized(inputRequested) {
                inputRequested.value = true
            }
        }

        // Wait until enter is pressed
        submitLock.withLock { submitCond.await() }

        // Get text
        val value = input.value

        // Wait until text is cleared and it and button disabled
        controller.uiHandleSync {
            input.value = ""
            synchronized(inputRequested) {
                inputRequested.value = false
            }
        }
        return value
    }
}