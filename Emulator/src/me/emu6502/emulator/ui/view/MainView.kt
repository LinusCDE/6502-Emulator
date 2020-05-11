package me.emu6502.emulator.ui.view

import javafx.scene.control.Tooltip
import javafx.util.Duration
import me.emu6502.emulator.darkModeEnabled
import me.emu6502.emulator.ui.CommandInfo
import me.emu6502.emulator.ui.controller.MainController
import me.emu6502.kotlinutils.toString
import tornadofx.*
import java.lang.Exception

class MainView: View(title = "6502-Emulator" /* Will get changed by AssemblyView! */) {
    val controller: MainController by inject()

    override val root = borderpane {
        center = pane {  }
        right = vbox {
            val screenScale = 1.75
            val screenWidth = controller.emulator.screen.bitmapScreen.width * screenScale
            titledpane(controller.screenTitleProperty) {
                isExpanded = true
                imageview(controller.screenImageProperty) {
                    fitWidth = screenWidth
                    isPreserveRatio = true

                    // Enlarge on double click
                    setOnMouseClicked { if(it.clickCount == 2) controller.isScreenInOwnWindow = true }
                }
            }
            titledpane(controller.textScreenTitleProperty) {
                isExpanded = controller.emulator.textscreen != null
                if(controller.emulator.textscreen != null) {
                    imageview(controller.textScreenImageProperty) {
                        fitWidth = screenWidth
                        isPreserveRatio = true

                        // Enlarge on double click
                        setOnMouseClicked { if(it.clickCount == 2) controller.isTextScreenInOwnWindow = true }
                    }
                }else {
                    hbox {
                        label("Resource benötigt.\n\n" +
                                "Bitte lege die Datei \"apple1.vid\" in\n" +
                                "das Programm-Verzeichnis und starte\n" +
                                "dieses Programm erneut, um diese\n" +
                                "Komponente verwenden zu können.") {
                            isDisable = true
                        }
                    }
                }
            }
            titledpane("PIA (\$${controller.emulator.pia.start.toString("X4")})") {
                tooltip = Tooltip("Parallel I/O Adapter")
                isExpanded = true
                add<PIAView>()
            }
            titledpane("Befehlsübersicht") {
                isExpanded = false
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
        find<AssemblerView>().apply { this@MainView.titleProperty.bind(this.titleProperty) }

        if(darkModeEnabled)
            importStylesheet(MainView::class.java.getResource("dark-style.css").toExternalForm())

        val onAssemblyViewChange: (Boolean) -> Unit = { newVal: Boolean? ->
            if(newVal === true) {
                root.center = find<ConsoleView>().root
                find<AssemblerWindow>().onDock()
            }else {
                root.center = splitpane {
                    // Use title provided by assembly view
                    add<AssemblerView>()
                    add<ConsoleView>()
                }
            }
        }
        controller.isAssemblerViewInOwnWindowProperty.onChange(onAssemblyViewChange)
        onAssemblyViewChange(controller.isAssemblerViewInOwnWindow) // Trigger onChange

        val onScreenViewChange: (Boolean) -> Unit = { newVal: Boolean? ->
            if(newVal === true)
                find<ScreenWindow>().onDock()
            else
                find<ScreenWindow>().close()
        }
        controller.isScreenInOwnWindowProperty.onChange(onScreenViewChange)
        onScreenViewChange(controller.isScreenInOwnWindow) // Trigger onChange

        val onTextScreenViewChange: (Boolean) -> Unit = { newVal: Boolean? ->
            if(newVal === true)
                find<TextScreenWindow>().onDock()
            else
                find<TextScreenWindow>().close()
        }
        controller.isTextScreenInOwnWindowProperty.onChange(onTextScreenViewChange)
        onTextScreenViewChange(controller.isTextScreenInOwnWindow) // Trigger onChange
    }

}
