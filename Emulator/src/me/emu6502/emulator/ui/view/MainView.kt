package me.emu6502.emulator.ui.view

import javafx.scene.Parent
import javafx.util.Duration
import me.emu6502.emulator.darkModeEnabled
import me.emu6502.emulator.ui.CommandInfo
import me.emu6502.emulator.ui.controller.MainController
import me.emu6502.kotlinutils.toString
import tornadofx.*
import java.lang.Exception

class MainView: View(title = "6502-Emulator") {
    val controller: MainController by inject()

    override val root: Parent = borderpane {
        center = splitpane {
            add(AssemblerView())
            add(ConsoleView())
        }
        right = vbox {
            titledpane("Screen (\$${controller.emulator.screen.start.toString("X4")})") {
                isExpanded = false
                imageview {
                    val scale = 2.0
                    fitWidth = 140 * scale
                    fitHeight = 120 * scale
                    isPreserveRatio = true
                    imageProperty().bind(controller.screenImageProperty)
                }
            }
            titledpane("PIA (\$${controller.emulator.pia.start.toString("X4")})") {
                isExpanded = false
                add(PIAView())
            }
            titledpane("Befehlsübersicht") {
                isExpanded = true
                tableview(controller.commands) {
                    column("Name", CommandInfo::nameProperty)
                    column("Beschreibung", CommandInfo::descriptionProperty)
                }
            }
        }
    }

    private fun setTitledPaneDuration(durationMillis: Int) {
        var skinClazz: Class<*>? = null
        // The class varies depending on the Java-Version used
        try {
            skinClazz = Class.forName("javafx.scene.control.skin.TitledPaneSkin")
        }catch (e: ClassNotFoundException) { } catch (e: UnsupportedClassVersionError) { }
        if(skinClazz == null) {
            try {
                skinClazz = Class.forName("com.sun.javafx.scene.control.skin.TitledPaneSkin")
            }catch (e: ClassNotFoundException) { } catch (e: UnsupportedClassVersionError) { }
        }

        if(skinClazz == null) {
            println("Failed to tune duration of TitledPanes (class not found)!")
            return
        }

        try {
            val durationField = skinClazz.getField("TRANSITION_DURATION")

            val intDurationInstance: Duration = durationField.get(null) as Duration
            val millisField = Duration::class.java.getDeclaredField("millis")
            millisField.isAccessible = true
            millisField.setDouble(intDurationInstance, durationMillis.toDouble())
        }catch (e: Exception) {
            println("Failed to tune duration of TitledPanes (reflection error)!")
            e.printStackTrace()
        }
    }

    init {
        setTitledPaneDuration(150)

        if(darkModeEnabled)
            importStylesheet(MainView::class.java.getResource("dark-style.css").toExternalForm())
    }
}
