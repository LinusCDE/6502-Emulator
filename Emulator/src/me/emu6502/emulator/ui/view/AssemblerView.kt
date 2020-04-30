package me.emu6502.emulator.ui.view

import javafx.application.Platform
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
            return "A"
        //val hex = "[A-Fa-f0-9]{" + addrMode.fixedValueLength + "}";
        val hex = "([A-F0-9]{" + addrMode.fixedValueLength + "}" + "|" + "[a-f0-9]{" + addrMode.fixedValueLength + "})"; // Don't allow case mixing
        val regex = (if(addrMode.prefix != "") Pattern.quote(addrMode.prefix) else "") + hex + (if(addrMode.suffix != "") Pattern.quote(addrMode.suffix) else "")
        return "($regex)"
    }

    val addressModeRegex = Regex("^" + AddressMode.values().map { asRegex(it) }.joinToString("|") + "$")


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

    private fun computeHighlightingOld(text: String, caretPosition: Int): Pair<String?/*styleclass at caret*/, StyleSpans<Collection<String>>> {
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

    private enum class LineType {
        VARIABLE,
        INSTRUCTION,
        DIRECTIVE,
        MEMORY_LABEL,
        ERROR,
        UNKNOWN
    }

    private val lastMemoryLabels = arrayListOf<String>()
    private val lastDirectiveLabels = arrayListOf<String>()


    private fun computeHighlighting(text: String, caretPosition: Int): Pair<String?/*styleclass at caret*/, StyleSpans<Collection<String>>> {
        val newMemoryLabels = arrayListOf<String>()
        val newDirectiveLabels = arrayListOf<String>()
        val spansBuilder = StyleSpansBuilder<Collection<String>>()

        println("LINES:")
        var lastLineEnd = 0
        for((lStart, lEnd) in getLineSubstrings(text)) {
            // Fill line gap
            if (lStart - lastLineEnd > 0)
                spansBuilder.add(Collections.emptyList(), lStart - lastLineEnd)
            lastLineEnd = lEnd

            val fullLine = text.substring(lStart, lEnd)
            //spansBuilder.add(Collections.singleton("comment"), line.length)

            val semicolonIndex = fullLine.indexOf(';')
            val (codeLine, commentLine) = if (semicolonIndex == -1)
                fullLine to ""
            else
                fullLine.substring(0, semicolonIndex) to fullLine.substring(semicolonIndex, fullLine.length)

            //println("\"$codeLine\" ; \"$commentLine\"")

            //spansBuilder.add(Collections.singleton("mnemonic"), codeLine.length)

            val words = arrayListOf<String>() // Words (and part word) that arent a comment
            for((wStart, wEnd) in getWordSubstrings(codeLine)) {
                var word = codeLine.substring(wStart, wEnd)
                words.add(word)
            }

            // Check type of line (and check for obvious errors)
            lateinit var lineType: LineType
            var currentInstruction: Instruction? = null
            if(words.size > 0) {
                val word = words[0]
                if (word.startsWith('.')) {
                    // Valid directive?
                    if (word.equals(".STRING", true) || word.equals(".BYTE", true)) {
                        lineType = LineType.DIRECTIVE
                        if(words.size > 1)
                            newDirectiveLabels.add(words[1])
                    } else if(word.equals(".STRING", true))
                        lineType = LineType.DIRECTIVE
                    else
                        lineType = LineType.ERROR // Unknown directive
                } else if (word.endsWith(":")) {
                    lineType = if (words.size == 1) LineType.MEMORY_LABEL else LineType.ERROR
                    newMemoryLabels.add(word.substring(0, word.length - 1))
                } else if (words.size > 1 && words[1] == "=") {
                    lineType = LineType.VARIABLE
                } else {
                    lineType = LineType.UNKNOWN
                    for (instr in Instruction.values()) {
                        if (instr.name.equals(word, true)) {
                            lineType = LineType.INSTRUCTION
                            currentInstruction = instr
                            break
                        }
                    }
                }
            }


            var lastWordEnd = 0
            var wordIndex = -1

            for((wStart, wEnd) in getWordSubstrings(codeLine)) {
                // Fill word gap (whitespace)
                if(wStart - lastWordEnd > 0)
                    spansBuilder.add(Collections.emptyList(), wStart - lastWordEnd)
                lastWordEnd = wEnd
                wordIndex++

                val word = codeLine.substring(wStart, wEnd)

                val type: String? = when(lineType) {
                    LineType.INSTRUCTION -> {
                        when(wordIndex) {
                            0 -> "mnemonic"
                            1 -> {
                                val label = if(word.endsWith(",Y", true) || word.endsWith(",X", true)) word.split(',')[0] else word
                                if(currentInstruction == null || currentInstruction?.opCodeAddrModes.isEmpty())
                                    "error"
                                else if(word.matches(addressModeRegex))
                                    "operator"
                                else if((currentInstruction.isAddressModeSupported(AddressMode.ZEROPAGE) || currentInstruction.isAddressModeSupported(AddressMode.ABSOLUTE)) && lastMemoryLabels.contains(word))
                                    "memory-label"
                                else if(lastMemoryLabels.contains(label) && (currentInstruction.isAddressModeSupported(AddressMode.ABSOLUTE_Y) || currentInstruction.isAddressModeSupported(AddressMode.ABSOLUTE_X)))
                                    "memory-label"
                                else if(currentInstruction.isAddressModeSupported(AddressMode.ABSOLUTE) && lastDirectiveLabels.contains(word))
                                    "memory-label"
                                else
                                    null
                            }
                            else -> "error"
                        }
                    }
                    LineType.VARIABLE -> {
                        when(wordIndex) {
                            0 -> "directive"
                            1 -> null
                            2 -> if(word.matches(addressModeRegex)) "operator" else null
                            else -> "error"
                        }
                    }
                    LineType.DIRECTIVE -> {
                        when(wordIndex) {
                            0 -> "directive"
                            1 -> "memory-label"
                            2 -> null
                            else -> "error"
                        }
                    }
                    LineType.MEMORY_LABEL -> "memory-label"
                    LineType.ERROR -> "error"
                    LineType.UNKNOWN -> null
                    else -> null
                }

                if(type != null)
                    println("\"$word\": $type")
                spansBuilder.add(if(type != null) Collections.singleton(type) else Collections.emptyList(), word.length)
            }
            // Fill remaining word gap (whitespace)
            if(lastWordEnd < codeLine.length)
                spansBuilder.add(Collections.emptyList(), codeLine.length - lastWordEnd)


            // Mark comment
            spansBuilder.add(Collections.singleton("comment"), commentLine.length)
        }

        // Fill remaining line gap
        if(lastLineEnd < text.length)
            spansBuilder.add(Collections.emptyList(), text.length - lastLineEnd)

        lastDirectiveLabels.clear()
        lastDirectiveLabels.addAll(newDirectiveLabels)
        lastMemoryLabels.clear()
        lastMemoryLabels.addAll(newMemoryLabels)
        return null to spansBuilder.create()
    }

    private fun getWordSubstrings(line: String): Array<Pair<Int, Int>> {
        val words = arrayListOf<Pair<Int, Int>>()
        var currentWordStart: Int = -1
        var prevChar: Char = ' '
        for(i in line.indices) {
            val c = line[i]
            if(prevChar.isWhitespace() && !c.isWhitespace())
                currentWordStart = i
            else if(!prevChar.isWhitespace() && c.isWhitespace()) {
                words.add(currentWordStart to i)
            }

            prevChar = c
        }

        if(!prevChar.isWhitespace())
            words.add(currentWordStart to line.length)

        return words.toTypedArray()
    }

    private fun getLineSubstrings(text: String): Array<Pair<Int, Int>> {
        val lines = arrayListOf<Pair<Int, Int>>()
        var lastLineStart: Int? = null
        for(i in text.indices) {
            val c = text[i]
            if(lastLineStart == null)
                lastLineStart = i
            if(c == '\n') {
                if(lastLineStart != null) {
                    lines.add(lastLineStart to i)
                    lastLineStart = null
                }else {
                    lines.add(i to i)
                }
            }
        }

        if(lastLineStart != null)
            lines.add(lastLineStart to text.length)

        return lines.toTypedArray()
    }

    init {
        importStylesheet(AssemblerView::class.java.getResource("assembly-view.css").toExternalForm())
    }
}