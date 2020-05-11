package me.emu6502.emulator.ui.view

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.ToggleGroup
import javafx.scene.control.Tooltip
import javafx.scene.input.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import me.emu6502.emulator.ui.controller.AssemblerController
import me.emu6502.lib6502.AddressMode
import me.emu6502.lib6502.Instruction
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import tornadofx.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.time.Duration
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class AssemblerView: View() {

    val controller: AssemblerController by inject()

    val asmFileChooser = FileChooser().apply {
        extensionFilters.setAll(FileChooser.ExtensionFilter("Assembly Source Code für 6502 (.s, .asm)", "*.s", "*.asm"))
        title = "Assembly Source Datei wählen..."
    }

    val binaryFileChooser = FileChooser().apply {
        extensionFilters.setAll(
                FileChooser.ExtensionFilter("Programm (.hex, .bin)", "*.hex", "*.bin"),
                FileChooser.ExtensionFilter("Beliebige Datei (.*)", "*")
        )
        title = "Binäre Datei wählen..."
    }

    init {
        val programName = "6502-Emulator"
        titleProperty.bind(controller.openedAssemblyFileProperty.stringBinding { file ->
            if(file == null)
                "$programName - Neue Datei"
            else
                "$programName - ${file.name}"
        })
    }

    override val root: Parent = borderpane {
        top = vbox {
            menubar {
                menu("Assembly") {
                    item("Neu") {
                        action {
                            controller.openedAssemblyFile = null
                            controller.sourceCode = ""
                        }
                        accelerator = KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)
                    }
                    item("Öffnen...") {
                        action {
                            val file: File = asmFileChooser.showOpenDialog(primaryStage) ?: return@action
                            controller.openedAssemblyFile = file
                            controller.onLoadSourcePressed(file)
                        }
                        accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)
                    }
                    item("Speichern") {
                        action {
                            if(controller.openedAssemblyFile == null) {
                                controller.openedAssemblyFile = asmFileChooser.showSaveDialog(primaryStage) ?: return@action
                            }
                            controller.onSaveSourcePressed(controller.openedAssemblyFile!!)
                        }
                        accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)
                    }
                    item("Speichern unter...") {
                        action {
                            val file = asmFileChooser.showSaveDialog(primaryStage) ?: return@action
                            controller.openedAssemblyFile = file
                            controller.onSaveSourcePressed(file)
                        }
                        accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN)
                    }
                }

                menu("Binär") {
                    item("Assemblieren nach...") {
                        action {
                            val file: File = binaryFileChooser.showSaveDialog(primaryStage) ?: return@action
                            controller.onSaveAssemblyToDisk(file)
                        }
                    }
                    item("Disassemblieren von...") {
                        action {
                            val file: File = binaryFileChooser.showOpenDialog(primaryStage) ?: return@action
                            controller.onDisassemblePressed(file)
                        }
                    }
                    separator()
                    item("In Speicher laden von...") {
                        action {
                            val file: File = binaryFileChooser.showOpenDialog(primaryStage) ?: return@action
                            controller.onLoadBinaryToMemoryPressed(file)
                        }
                    }
                }

                menu("Emulation") {
                    menu("Emulations-Präzision") {
                        val toggleGroup = ToggleGroup()

                        val default = controller.mainController.emulationSyncsPerSecond
                        for(hz in arrayOf(1, 2, 5, 10, 20, 30, 50, 60, 100, 120, 200, 240, 250)) {
                            radiomenuitem("$hz Hertz", toggleGroup, value = hz) {
                                if(hz == default)
                                    isSelected = true
                                if(hz > 30) {
                                    style {
                                        textFill = Color.DARKRED
                                    }
                                }
                                action {
                                    controller.mainController.emulationSyncsPerSecond = hz
                                }
                            }
                        }
                    }
                }

                menu("Ansicht") {
                    item("Pop-Out") {
                        action { controller.mainController.isAssemblerViewInOwnWindow = true }
                    }
                    disableProperty().bind(controller.mainController.isAssemblerViewInOwnWindowProperty)
                }
            }
            borderpane {
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
                    addEventHandler(MouseEvent.MOUSE_CLICKED) {
                        if(it.isControlDown)
                            controller.onAssembleMovePcAndRunButtonPressed()
                        else
                            controller.onAssembleButtonPressed()
                    }
                }
            }
        }
        center = VirtualizedScrollPane(CodeArea().apply {
            sceneProperty().onChange {
                it?.stylesheets?.add(AssemblerView::class.java.getResource("assembly-view.css").toExternalForm())
            }

            style {
                prefWidth = Dimension(25.0, Dimension.LinearUnits.em)
                isWrapText = false
                fontFamily = "monospaced"
            }

            val whiteSpace = Pattern.compile("^\\s+")
            addEventHandler(KeyEvent.KEY_PRESSED) {
                if (it.code === KeyCode.ENTER && !it.isControlDown) {
                    val caretPosition: Int = caretPosition
                    val currentParagraph: Int = currentParagraph
                    val m0: Matcher = whiteSpace.matcher(getParagraph(currentParagraph - 1).segments[0])
                    if (m0.find()) Platform.runLater { insertText(caretPosition, m0.group()) }
                }
            }
            cssclass("assembly-editor")
            paragraphGraphicFactory = LineNumberFactory.get(this).apply {
                cssclass("line-numbers")
            }

            var lastSyntaxUpdate = System.currentTimeMillis()

            var lastStyleAtCaret: String? = null
            addEventHandler(KeyEvent.KEY_TYPED) {
                if(lastStyleAtCaret != null) {
                    // Set style that will be applied immediately after typing
                    // This might apply the wrong style briefly but not all text flat out default
                    // when writing already styled sections
                    try {
                        setStyleClass(caretPosition - 1, caretPosition, lastStyleAtCaret)
                    }catch (e: IndexOutOfBoundsException) { }
                }
            }

            addEventHandler(KeyEvent.KEY_PRESSED) {
                if(it.isControlDown && it.code == KeyCode.ENTER)
                    controller.onAssembleMovePcAndRunButtonPressed()
            }

            val updateSyntax = {
                val now = System.currentTimeMillis()
                val (styleAtCaret, styleSpans) = computeHighlighting(text, caretPosition)
                lastStyleAtCaret = styleAtCaret

                setStyleSpans(0, styleSpans)
                lastSyntaxUpdate = now
            }

            multiPlainChanges().subscribe {
                // Update every x millis when writing
                if(System.currentTimeMillis() - lastSyntaxUpdate >= 250)
                    updateSyntax()
            }
            multiPlainChanges().successionEnds(Duration.ofMillis(250))
                    .subscribe { updateSyntax() } // Update x millis after stopping to write

            controller.sourceCodeProperty.onChange {
                if(it != text) {
                    replaceText(it)
                    controller.mainController.uiHandleAsync { updateSyntax() }
                }
            }
            textProperty().onChange { controller.sourceCode = it }
            replaceText(controller.sourceCode)
            controller.mainController.uiHandleAsync { updateSyntax() } // Highlight on init
        })
        bottom = label(controller.statusMessageProperty) {
            tooltip = Tooltip()
            tooltip.textProperty().bind(controller.statusMessageProperty)
        }
    }

    private fun asRegex(addrMode: AddressMode): String {
        if(addrMode == AddressMode.ACCUMULATOR)
            return "\\bA\\b"
        //val hex = "[A-Fa-f0-9]{" + addrMode.fixedValueLength + "}";
        val hex = "([A-F0-9]{" + addrMode.fixedValueLength + "}" + "|" + "[a-f0-9]{" + addrMode.fixedValueLength + "})"; // Don't allow case mixing
        val regex = (if(addrMode.prefix != "") Pattern.quote(addrMode.prefix) else "") + hex + (if(addrMode.suffix != "") Pattern.quote(addrMode.suffix) else "")
        return "($regex)"
    }

    val MNEMONIC_PATTERN = "(" + Instruction.values().map { "\\b${it.name}\\b" }.joinToString("|") + ")"
    val MEMORYLABEL_PATTERN = "([^\n][a-zA-Z0-9_-]*:)"
    val OPERATOR_PATTERN = "([^\n]" + AddressMode.values().map { asRegex(it) }.joinToString("|") + ")"
    val COMMENT_PATTERN = "(;(.*))"

    val PATTERN = Pattern.compile(
            "(?<MEMORYLABEL>$MEMORYLABEL_PATTERN)" +
            "|(?<MNEMONIC>$MNEMONIC_PATTERN)" +
            "|(?<OPERATOR>$OPERATOR_PATTERN)" +
            "|(?<COMMENT>$COMMENT_PATTERN)"
    )

    private fun computeHighlighting(text: String, caretPosition: Int): Pair<String?/*styleclass at caret*/, StyleSpans<Collection<String>>> {
        val matcher: Matcher = PATTERN.matcher(text)
        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var styleClassAtCaret: String? = null
        while (matcher.find()) {
            val styleClass = when {
                matcher.group("MNEMONIC") != null -> "mnemonic"
                matcher.group("MEMORYLABEL") != null -> "memory-label"
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("OPERATOR") != null -> "operator"
                else -> null // Never happens
            }!!
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()

            if(caretPosition >= matcher.start() && caretPosition <= matcher.end())
                styleClassAtCaret = styleClass // Current style has caret in it -> Use the class for new text
        }
        spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)


        return styleClassAtCaret to spansBuilder.create()
    }

    init {
        importStylesheet(AssemblerView::class.java.getResource("assembly-view.css").toExternalForm())
    }
}