package me.emu6502.emulator

import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import vt100.VT100Attribute
import vt100.VT100BackgroundColor
import vt100.VT100ForegroundColor
import vt100.VT100Sequence

/**
 * Good source when needing to add more functions:
 * http://www.termsys.demon.co.uk/vtansi.htm
 */
class Console {
    companion object {
        private const val CARRIAGE_RETURN = 0x0D.toChar() // aka Enter
        private const val DEL = 0x7F.toChar() // aka Backspace

        val terminal = TerminalBuilder.terminal().apply { enterRawMode() }
        val reader = terminal.reader()
        val writer = terminal.writer()
        val completer = CommandCompleter()
        var lineReader = LineReaderBuilder.builder().terminal(terminal).completer(completer).build()

        var foregroundColor: VT100ForegroundColor? = null
            set(newColor: VT100ForegroundColor?) {
                if(newColor != null)
                    writeControlEscapeSequence(VT100Sequence.SET_ATTRIBUTE_MODE, newColor)
                field = newColor
            }
        var backgroundColor: VT100BackgroundColor? = null
            set(newColor: VT100BackgroundColor?) {
                if(newColor != null)
                    writeControlEscapeSequence(VT100Sequence.SET_ATTRIBUTE_MODE, newColor)
                field = newColor
            }

        fun resizeMac(width: Int, height: Int) = write("${VT100Sequence.ESC}[8;$height;${width}t")
        fun writeControlEscapeSequence(vtSeq: VT100Sequence) = write(vtSeq.toString())
        fun writeControlEscapeSequence(vtSeq: VT100Sequence, vararg attributes: Int) = write(vtSeq.toString(*attributes))
        fun writeControlEscapeSequence(vtSeq: VT100Sequence, vararg attributes: VT100Attribute) = write(vtSeq.toString(*attributes))
        fun homeCursor() = writeControlEscapeSequence(VT100Sequence.CURSOR_HOME)
        fun homeCursor(row: Int, col: Int) = writeControlEscapeSequence(VT100Sequence.CURSOR_HOME_TO, row, col)
        fun eraseDown() = writeControlEscapeSequence(VT100Sequence.ERASE_DOWN)
        fun clear() = write("${VT100Sequence.CURSOR_HOME}${VT100Sequence.ERASE_DOWN}")

        fun write(text: Any?) { writer.print(text); writer.flush() }
        fun writeLine() = writeLine("")
        fun writeLine(text: Any? = null) { writer.println(text); writer.flush() }

        fun readKey(): Char = reader.read().toChar()
        fun readLine(): String = lineReader.readLine()
        fun readLine(prompt: String): String = lineReader.readLine(prompt)
    }
}