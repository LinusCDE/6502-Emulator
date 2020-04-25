package me.emu6502.emulator.ui.view

import javafx.css.converter.PaintConverter
import javafx.scene.Parent
import javafx.scene.paint.Color
import me.emu6502.emulator.ui.CommandInfo
import me.emu6502.emulator.ui.controller.MainController
import org.aerofx.AeroFX
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

    init {
        //AeroFX.style()
        //Material
        importStylesheet(MainView::class.java.getResource("style.css").toExternalForm())
    }
}
