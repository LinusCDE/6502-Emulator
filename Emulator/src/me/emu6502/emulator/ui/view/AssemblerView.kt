package me.emu6502.emulator.ui.view

import javafx.geometry.Pos
import javafx.scene.Parent
import me.emu6502.emulator.ui.controller.AssemblerController
import tornadofx.*

class AssemblerView: View() {

    val controller: AssemblerController by inject()

    override val root: Parent = borderpane {
        top = borderpane {
            left = hbox {
                alignment = Pos.CENTER_LEFT
                label("Ziel-Adresse: ")
                textfield(controller.memoryAddressProperty) {
                    style {
                        fontFamily = "monospaced"
                        prefColumnCount = 5
                    }
                }
            }
            right = button("Assemble") {
                action { controller.onAssembleButtonPressed() }
            }
        }
        center = textarea(controller.sourceCodeProperty) {
            style {
                prefColumnCount = 30
                isWrapText = false
                fontFamily = "monospaced"
            }
        }
        bottom = label(controller.statusMessageProperty)
    }
}