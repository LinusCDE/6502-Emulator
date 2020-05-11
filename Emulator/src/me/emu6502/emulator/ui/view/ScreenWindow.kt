package me.emu6502.emulator.ui.view

import javafx.scene.image.ImageView
import javafx.stage.Modality
import me.emu6502.emulator.ui.controller.MainController
import tornadofx.*

class ScreenWindow: View() {

    val mainController: MainController by inject()

    override fun onDock() {
        openWindow(modality = Modality.NONE)

        // Maximize image
        (root.center as ImageView).fitWidthProperty().bind(currentStage?.widthProperty())
        (root.center as ImageView).fitHeightProperty().bind(currentStage?.heightProperty())
    }

    override fun onUndock() {
        mainController.uiHandleAsync {
            mainController.isScreenInOwnWindow = false
        }
    }

    override val root = borderpane {
        titleProperty.bind(mainController.screenTitleProperty)
        center = imageview(mainController.screenImageProperty) {
            isPreserveRatio = true
        }
    }

}