package me.emu6502.emulator.ui.view

import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.effect.DropShadow
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Modality
import me.emu6502.emulator.ui.controller.MainController
import tornadofx.*

class TextScreenWindow: View() {

    val mainController: MainController by inject()

    override fun onDock() {
        openWindow(modality = Modality.NONE)

        // Maximize image
        imageView.fitWidthProperty().bind((root.center as StackPane).widthProperty())
        imageView.fitHeightProperty().bind((root.center as StackPane).heightProperty())
    }

    override fun onUndock() {
        mainController.uiHandleAsync {
            mainController.isTextScreenInOwnWindow = false
        }
    }

    val imageView = imageview(mainController.textScreenImageProperty) {
        isPreserveRatio = true
    }

    override val root = borderpane {
        titleProperty.bind(mainController.textScreenTitleProperty)
        center = stackpane {
            add(imageView)

            minWidth = 1.0
            minHeight = 1.0
            alignment = Pos.CENTER
        }

        // Add C64 look and feel (frame)
        style {
            backgroundColor = MultiValue(arrayOf(Paint.valueOf("#7b71d5")))
        }
        // "Border":
        top = pane { prefHeight = 50.0 }
        bottom = pane { prefHeight = 50.0 }
        left = pane { prefWidth = 50.0 }
        right = pane { prefWidth = 50.0 }
    }

}