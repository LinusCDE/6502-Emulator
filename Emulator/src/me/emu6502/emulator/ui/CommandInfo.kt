package me.emu6502.emulator.ui

import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class CommandInfo(name: String = "", description: String = "") {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    val descriptionProperty = SimpleStringProperty(description)
    var description by descriptionProperty
}