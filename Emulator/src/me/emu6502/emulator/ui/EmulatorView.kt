package me.emu6502.emulator.ui

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import tornadofx.*
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmulatorApp: App(EmulatorWindow::class)

class EmulatorWindow: View() {
    val controller: EmulatorController = find<EmulatorController>()

    override val root: Parent = vbox {
        apply { add(find(EmulatorConsoleView::class)) }
    }
}

class EmulatorConsoleView: View() {
    val controller: EmulatorController by inject()

    val output = textarea {
        isEditable = false
        isWrapText = true
        style {
            prefColumnCount = 140
            prefRowCount = 38
            fontFamily = "monospaced"
        }
    }

    val input = SimpleStringProperty("")
    val inputRequested = SimpleBooleanProperty(false)
    val submitLock = ReentrantLock()
    val submitCond = submitLock.newCondition()

    override val root: Parent = borderpane {
        center = output
        bottom = borderpane {
            center = textfield(input) {
                editableProperty().bind(inputRequested)

                action {
                    submitLock.withLock {
                        submitCond.signal()
                    }
                }
            }
            right = button {
                disableProperty().bind(inputRequested.booleanBinding { !it!! })
                textProperty().bind(input.stringBinding { if(it == "") "Step" else "Send" })

                action {
                    submitLock.withLock {
                        submitCond.signal()
                    }
                }
            }
        }
    }

    fun clear() { output.text = "" }
    fun write(text: String) { output.text += text }
    fun writeLine(text: String) { output.text += "$text\n" }
    fun asyncReqeuestCommand(): String {
        submitLock.withLock {
            controller.asyncBlockingUi { inputRequested.value = true }
            submitCond.await()
        }
        val value = input.value
        controller.asyncBlockingUi {
            input.value = ""
            inputRequested.value = false
        }
        return value
    }
    fun asyncRquestRawInput(): String = asyncReqeuestCommand()
}