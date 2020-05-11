package me.emu6502.emulator.ui.view

import javafx.stage.Modality
import me.emu6502.emulator.ui.controller.MainController
import tornadofx.*

class AssemblerWindow: Fragment() {

    val mainController: MainController by inject()

    override fun onDock() {
        root.center = find<AssemblerView>().root
        openWindow(modality = Modality.NONE)!!.apply {
            width = 400.0
            height = 600.0
        }
    }

    override fun onUndock() {
        mainController.uiHandleAsync {
            mainController.isAssemblerViewInOwnWindow = false
        }
    }

    override val root = borderpane { }

}