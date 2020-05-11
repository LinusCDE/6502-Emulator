package me.emu6502.emulator.ui.view

import javafx.stage.Modality
import me.emu6502.emulator.ui.controller.AssemblerController
import me.emu6502.emulator.ui.controller.MainController
import tornadofx.*

class AssemblerWindow: Fragment() {

    val mainController: MainController by inject()
    val asmController: AssemblerController by inject()

    override fun onDock() {
        root.center = find<AssemblerView>().root
        openWindow(modality = Modality.NONE)
    }

    override fun onUndock() {
        mainController.uiHandleAsync {
            mainController.isAssemblerViewInOwnWindow = false
        }
    }

    override val root = borderpane {
        titleProperty.bind(asmController.openedAssemblyFileProperty.stringBinding { file ->
            val programName = find<AssemblerView>().title.split(" - ")[0] // From title of main window
            if(file == null)
                "$programName - Assembler - Neue Datei"
            else
                "$programName - Assembler - ${file.name}"
        })
        prefWidth = 400.0
        prefHeight = 600.0
    }

}