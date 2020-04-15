package me.emu6502.emulator.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.image.Image
import tornadofx.*

class EmulatorApp: App(EmulatorWindow::class)

class CommandInfo(name: String = "", description: String = "") {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    val descriptionProperty = SimpleStringProperty(description)
    var description by descriptionProperty
}

class EmulatorWindow: View() {
    val controller: EmulatorController by inject()

    override val root: Parent = hbox {
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

    val output = textarea(controller.outputProperty) {
        isEditable = false
        isWrapText = true
        style {
            prefColumnCount = 134
            prefRowCount = 38
            fontFamily = "monospaced"
        }
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