package me.emu6502.emulator.ui.view

import javafx.application.Platform
import javafx.css.converter.PaintConverter
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import me.emu6502.emulator.ui.controller.AssemblerController
import me.emu6502.lib6502.AddressMode
import me.emu6502.lib6502.Instruction
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import tornadofx.*
import java.time.Duration
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import me.emu6502.lib6502.AddressMode.*


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
        center = VirtualizedScrollPane(CodeArea().apply {
            controller.sourceCodeProperty.onChange {
                if(it != text)
                    replaceText(it)
            }
            textProperty().onChange { controller.sourceCode = it }
            replaceText(controller.sourceCode)
            setStyleClass(0, 10, "keyword")

            sceneProperty().onChange {
                it?.stylesheets?.add(AssemblerView::class.java.getResource("assembly-view.css").toExternalForm())
            }

            style {
                prefColumnCount = 30
                isWrapText = false
                fontFamily = "monospaced"
            }

            val whiteSpace = Pattern.compile("^\\s+")
            addEventHandler(KeyEvent.KEY_PRESSED) {
                if (it.code === KeyCode.ENTER) {
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
                    setStyleClass(caretPosition - 1, caretPosition, lastStyleAtCaret)
                }
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

            controller.mainController.uiHandleAsync { updateSyntax() } // Highlight on init
        })
        bottom = label(controller.statusMessageProperty) {
            tooltip = Tooltip()
            tooltip.textProperty().bind(controller.statusMessageProperty)
        }
    }

    private fun asRegex(addrMode: AddressMode): String {
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