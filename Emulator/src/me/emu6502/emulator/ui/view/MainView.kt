package me.emu6502.emulator.ui.view

import javafx.scene.Parent
import me.emu6502.emulator.ui.CommandInfo
import me.emu6502.emulator.ui.controller.MainController
import tornadofx.*

class MainView: View(title = "6502-Emulator") {
    val controller: MainController by inject()

    override val root: Parent = hbox {
        add(AssemblerView())
        add(ConsoleView())
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
