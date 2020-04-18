package me.emu6502.emulator

import me.emu6502.kotlinutils.vt100.*
import java.lang.Integer.max

class BasicVT100Engine(val columns: Int, val rows: Int, val tabSize: Int = 8,
                       initialIsNewLineMode: Boolean = true, val maxBufferSize: Int = 2048) {

    data class Cell(var char: Char?, var cellStyle: CellStyle) {
        fun erase() {
            char = null
            cellStyle.reset()
        }
    }

    data class CellStyle(var bright: Boolean = false, var dim: Boolean = false, var underscore: Boolean = false,
                    var blink: Boolean = false, var reverse: Boolean = false, var hidden: Boolean = false,
                    var foregroundColor: VT100ForegroundColor? = null, var backgroundColor: VT100BackgroundColor? = null) {

        fun reset() {
            bright = false
            dim = false
            underscore = false
            blink = false
            reverse = false
            hidden = false
            foregroundColor = null
            backgroundColor = null
        }

        fun apply(vararg attributes: VT100Attribute) {
            for(attr in attributes) {
                when(attr) {
                    VT100Display.RESET_ALL_ATTRIBUTES -> reset()
                    VT100Display.BRIGHT -> bright = true
                    VT100Display.DIM -> dim = true
                    VT100Display.UNDERSCORE -> underscore = true
                    VT100Display.BLINK -> blink = true
                    VT100Display.REVERSE -> reverse = true
                    VT100Display.HIDDEN -> hidden = true
                    in VT100ForegroundColor.values() -> foregroundColor = attr as VT100ForegroundColor
                    in VT100BackgroundColor.values() -> backgroundColor = attr as VT100BackgroundColor
                    else -> throw NotImplementedError("Unknown attribute found. ${attr.javaClass.name}: $attr")
                }
            }
        }

        fun applyTo(other: CellStyle) {
            other.bright = bright
            other.dim = dim
            other.underscore = underscore
            other.blink = blink
            other.reverse = reverse
            other.hidden = hidden
            other.foregroundColor = foregroundColor
            other.backgroundColor = backgroundColor
        }

    }

    var consoleTop = 0 // Columns that is the first displayed (lines above are not in the window currently)
    var cursorCol = 0
        private set
    var cursorRow = 0
        private set(newRow: Int) {
            if(newRow < 0)
                return
            //if(newRow - consoleTop > rows) // Drag consoleTop behind
            //    consoleTop = newRow - rows
            field = newRow
        }
    var isNewLineMode = initialIsNewLineMode // https://vt100.net/docs/vt510-rm/LNM.html
        private set
    val cells: MutableList<Array<Cell>> = MutableList(1) { createEmptyRow() }
    private val currentCell: Cell?
        get() {
            return if(consoleTop + cursorRow < cells.size)
                cells[consoleTop + cursorRow][cursorCol]
            else
                null
        }
    private var currentCellStyle: CellStyle = CellStyle()
    var defaultBackground: VT100BackgroundColor? = null // Background override if a cell is null for that
        private set
    private var savedCursorCol = 0
    private var savedCursorRow = 0
    private var savedCellStyle = CellStyle()

    fun reset() {
        cells.clear()
        cells.add(createEmptyRow())
        consoleTop = 0
        cursorRow = 0
        cursorCol = 0
        defaultBackground = null
        currentCellStyle = CellStyle()
        savedCursorCol = 0
        savedCursorRow = 0
        savedCellStyle = CellStyle()
        isNewLineMode = true
    }

    private fun createEmptyRow(): Array<Cell> =
            Array(columns) {
                Cell(char = null,
                    cellStyle = CellStyle()
                )
            }

    private fun warnNotSupported(name: String) {
        println("WARNING: Feature \"$name\" not supported!")
    }

    fun write(text: String) {
        var i = 0
        while(i < text.length) {
            val currentChar = text[i]
            when(currentChar) {
                // Go back one cell (back space) // Difference between DEL and BACK SPACE omitted here
                in arrayOf('\b', /*DEL*/0x7F.toChar()) -> {
                    cursorCol--

                    // Wrap to previous line if necessary
                    if (cursorCol < 0) {
                        cursorCol = columns - 1
                        if (cursorRow > 0)
                            cursorRow - 1
                    }
                    i++
                }

                // Cursor to beginning (carriage return)
                '\r' -> {
                    cursorCol = 0
                    i++
                }
                // Tab
                '\t' -> {
                    write(String(CharArray(tabSize - cursorCol % tabSize) {' '}))
                    i++
                }
                // New line
                '\n' -> {
                    if(isNewLineMode)
                        write("\r")
                    consoleTop
                    cursorRow
                    cells.size
                    val sum = consoleTop + cursorRow
                    sum
                    toNextRow()
                    i++
                }
                // Escape
                VT100Sequence.ESC -> {
                    val res = VT100Sequence.parse(text.substring(i))
                    if(res == null) {
                        println("Unknown escape sequence: ${text.substring(i).replace(VT100Sequence.ESC, '[')}")
                        // Invalid or incomplete escape sequence
                        // Skip that character (and thus print the broken rest)
                        i++
                    }else {
                        // Valid escape sequence found
                        val (string, seq, attrs) = res

                        when(seq) {
                            VT100Sequence.SET_ATTRIBUTE_MODE -> {
                                val parsed = arrayListOf<VT100Attribute>()
                                for(attrId in attrs) {
                                    val possible = arrayOf<VT100Attribute>(*VT100Display.values(), *VT100BackgroundColor.values(), *VT100ForegroundColor.values())
                                    val attr = possible.firstOrNull { it.attribute == attrId }
                                    if(attr != null)
                                        parsed.add(attr)
                                }

                                currentCellStyle.apply(*parsed.toTypedArray())
                            }
                            VT100Sequence.ERASE_DOWN -> {
                                write(VT100Sequence.ERASE_END_OF_LINE.toString()!!)
                                if(cursorRow == 0)
                                    consoleTop = cells.size - 1
                                else {
                                    while(cursorRow + 1 + consoleTop != cells.size - 1)
                                        cells.removeAt(cells.size - 1)
                                }
                            }
                            VT100Sequence.CLEAR_ALL_TABS -> warnNotSupported(seq.name)
                            VT100Sequence.CLEAR_TAB -> warnNotSupported(seq.name)
                            VT100Sequence.CURSOR_UP -> cursorRow = max(0, cursorRow - attrs[0])
                            VT100Sequence.CURSOR_BACKWARD -> {
                                val rows = attrs[0] % columns
                                if(rows > 0) write(VT100Sequence.CURSOR_UP.toString(rows)!!)
                                cursorCol -= attrs[0] % columns
                            }
                            VT100Sequence.CURSOR_DOWN -> {
                                (0 until attrs[0]).forEach {
                                    if(cursorRow + consoleTop < cells.size)
                                        cursorRow++
                                }
                            }
                            VT100Sequence.CURSOR_FORWARD -> {
                                val rows = attrs[0] % columns
                                if(rows > 0) write(VT100Sequence.CURSOR_DOWN.toString(rows)!!)
                                cursorCol += attrs[0] % columns
                            }
                            VT100Sequence.CURSOR_UP_ONE -> write(VT100Sequence.CURSOR_UP.toString(1)!!)
                            VT100Sequence.CURSOR_BACKWARD_ONE -> write(VT100Sequence.CURSOR_BACKWARD.toString(1)!!)
                            VT100Sequence.CURSOR_DOWN_ONE -> write(VT100Sequence.CURSOR_DOWN.toString(1)!!)
                            VT100Sequence.CURSOR_FORWARD_ONE -> write(VT100Sequence.CURSOR_FORWARD.toString(1)!!)
                            VT100Sequence.CURSOR_HOME -> write(VT100Sequence.CURSOR_HOME_TO.toString(0, 0)!!)
                            VT100Sequence.CURSOR_HOME_TO -> {
                                var (rows, cols) = attrs
                                var (relRows, relCols) = arrayOf(rows - cursorRow, cols - cursorCol)
                                if(relRows < 0)
                                    write(VT100Sequence.CURSOR_UP.toString(-relRows)!!)
                                else
                                    write(VT100Sequence.CURSOR_DOWN.toString(relRows)!!)
                                if(relCols < 0)
                                    write(VT100Sequence.CURSOR_BACKWARD.toString(-relCols)!!)
                                else
                                    write(VT100Sequence.CURSOR_FORWARD.toString(relCols)!!)
                            }
                            VT100Sequence.DISABLE_LINE_WRAP -> warnNotSupported(seq.name)
                            VT100Sequence.ENABLE_LINE_WRAP -> {} // = Current mode
                            VT100Sequence.ERASE_END_OF_LINE -> {
                                for(col in cursorCol until columns)
                                    cells[consoleTop + cursorRow][col].erase()
                            }
                            VT100Sequence.ERASE_LINE -> {
                                for(col in 0 until columns)
                                    cells[consoleTop + cursorRow][col].erase()
                            }
                            VT100Sequence.ERASE_SCREEN -> {
                                // Even though it's in the docs. No terminal (tty, vty) homes the cursor
                                // for this command they only delete any content
                                defaultBackground = currentCellStyle.backgroundColor

                                for(row in consoleTop until cells.size) {
                                    for(col in 0 until columns)
                                        cells[row][col].erase()
                                }
                            }
                            VT100Sequence.ERASE_START_OF_LINE -> {
                                for(col in 0..cursorCol)
                                    cells[consoleTop + cursorRow][col].erase()
                            }
                            VT100Sequence.ERASE_UP -> {
                                write(VT100Sequence.ERASE_START_OF_LINE.toString()!!)
                                for(row in consoleTop until cells.size)
                                    for(col in 0 until columns)
                                        cells[row][col].erase()
                            }
                            VT100Sequence.FONT_SET_G0_DEFAULT -> {} // Only one font used
                            VT100Sequence.FONT_SET_G1_ALTERNATE -> warnNotSupported(seq.name)
                            VT100Sequence.FORCE_CURSOR_POSITION -> write(VT100Sequence.CURSOR_HOME.toString()!!) // Same
                            VT100Sequence.PRINT_LINE -> warnNotSupported(seq.name) // Ancient
                            VT100Sequence.PRINT_SCREEN -> warnNotSupported(seq.name) // Ancient
                            VT100Sequence.QUERY_CURSOR_POSITION -> warnNotSupported(seq.name)
                            VT100Sequence.QUERY_DEVICE_CODE -> warnNotSupported(seq.name)
                            VT100Sequence.QUERY_DEVICE_STATUS -> warnNotSupported(seq.name)
                            VT100Sequence.RESET_DEVICE -> reset()
                            VT100Sequence.RESTORE_CURSOR_AND_ATTRS -> {
                                write(VT100Sequence.UNSAVE_CURSOR.toString())
                                currentCellStyle = savedCellStyle.copy()
                            }
                            VT100Sequence.SAVE_CURSOR -> {
                                savedCursorRow = cursorRow
                                savedCursorCol = cursorCol
                            }
                            VT100Sequence.SAVE_CURSOR_AND_ATTRS -> {
                                write(VT100Sequence.SAVE_CURSOR.toString())
                                savedCellStyle = currentCellStyle.copy()
                            }
                            VT100Sequence.SCROLL_DOWN -> warnNotSupported(seq.name) // Not controllable
                            VT100Sequence.SCROLL_SCREEN -> warnNotSupported(seq.name) // Not controllable
                            VT100Sequence.SCROLL_SCREEN_FROM_TO -> warnNotSupported(seq.name) // Not controllable
                            VT100Sequence.SCROLL_UP -> warnNotSupported(seq.name) // Not controllable
                            VT100Sequence.SET_TAB_ -> warnNotSupported(seq.name)
                            VT100Sequence.START_PRINT_LOG -> warnNotSupported(seq.name) // Ancient
                            VT100Sequence.STOP_PRINT_LOG -> warnNotSupported(seq.name) // Ancient
                            VT100Sequence.UNSAVE_CURSOR -> {
                                cursorRow = savedCursorRow
                                cursorCol = savedCursorCol
                            }
                            VT100Sequence.SET_NEW_LINE -> isNewLineMode = true
                            VT100Sequence.SET_LINE_FEED -> isNewLineMode = false
                            else -> warnNotSupported(seq.name)
                        }

                        i += string.length // Skip whole sequence
                    }
                }
                // Any other char
                else -> {
                    if(cursorCol == columns) {
                        toNextRow()
                        cursorCol = 0
                    }

                    val cell = currentCell
                    // Set cell
                    cell?.char = currentChar
                    if(cell != null)
                        currentCellStyle.applyTo(cell.cellStyle)

                    // Move cursor
                    cursorCol++
                    i++
                }
            }
        }
    }

    private fun toNextRow() {
        cursorRow++
        val absRow = consoleTop + cursorRow
        if(absRow == cells.size) // Next row doesn't exist
            cells.add(createEmptyRow())
        if(cursorRow == rows) {
            consoleTop++
            cursorRow--
        }
    }
}