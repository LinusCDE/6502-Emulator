package me.emu6502.emulator.ui

import com.kodedu.terminalfx.Terminal
import com.kodedu.terminalfx.TerminalBuilder
import com.kodedu.terminalfx.config.TerminalConfig
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.text.Text
import me.emu6502.emulator.Console
import me.emu6502.kotlinutils.io.PatientPipedReader
import me.emu6502.kotlinutils.io.PatientPipedWriter
import tornadofx.*
import java.io.*
import java.lang.StringBuilder
import java.nio.file.Paths

class EmulatorApp: App(EmulatorWindow::class)

class CommandInfo(name: String = "", description: String = "") {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    val descriptionProperty = SimpleStringProperty(description)
    var description by descriptionProperty
}

class EmulatorWindow: View(title = "6502-Emulator") {
    val controller: EmulatorController by inject()

    override val root: Parent = hbox {
        add(find<AssemblerView>())
        add(find<EmulatorConsoleView>())
        vbox {
            label("Screen:")
            imageview {
                val scale = 2.0
                fitWidth = 140 * scale
                fitHeight = 120 * scale
                isPreserveRatio = true
                imageProperty().bind(controller.screenImageProperty)
            }
            label("Befehle:")
            tableview(controller.commands) {
                column("Name", CommandInfo::nameProperty)
                column("Beschreibung", CommandInfo::descriptionProperty)
            }
        }
    }
}

class EmulatorConsoleView: View() {
    val controller: EmulatorConsoleController by inject()

    val output = Terminal(TerminalConfig().apply {
        windowsTerminalStarter = ""
        //unixTerminalStarter = "/usr/bin/cat"
        unixTerminalStarter = ""
        isCursorBlink = true
    }, Paths.get("")).apply {
        style {
            prefWidth = Dimension(134.0 * 0.7, Dimension.LinearUnits.em)
            prefHeight = Dimension(38.0 * 0.7, Dimension.LinearUnits.em)
        }
        inputReader = BufferedReader(PatientPipedReader(controller.inputWriter))
    }

    override val root: Parent = borderpane {
        center = output
        bottom = borderpane {
            left = hbox {
                label("> ")
                alignment = Pos.CENTER
            }
            center = textfield(controller.inputProperty) {
                editableProperty().bind(controller.inputEnabledProperty)
                action { controller.onInputEnterPressed() }
            }
            right = button {
                disableProperty().bind(controller.inputEnabledProperty.booleanBinding { !it!! })
                textProperty().bind(controller.inputProperty.stringBinding { if(it == "") "Step" else "Send" })
                action { controller.onInputEnterPressed() }
            }
        }
    }

}

class AssemblerView: View() {
    val controller: AssemblerController by inject()

    override val root: Parent = borderpane {
        top = borderpane {
            left = hbox {
                alignment = Pos.CENTER_LEFT
                label("Ziel-Adresse: ")
                textfield(controller.memoryAddressProperty) {
                    style {
                        fontFamily = "monospaced"
                        prefColumnCount = 5
                    }
                }
            }
            right = button("Assemble") {
                action { controller.onAssembleButtonPressed() }
            }
        }
        center = textarea(controller.sourceCodeProperty) {
            style {
                prefColumnCount = 30
                isWrapText = false
                fontFamily = "monospaced"
            }
        }
        bottom = label(controller.statusMessageProperty)
    }
}