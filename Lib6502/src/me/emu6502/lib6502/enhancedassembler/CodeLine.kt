package me.emu6502.lib6502.enhancedassembler

class CodeLine(line: String) {

    companion object {
        enum class LineType {
            CODE,
            COMMENT,
            DIRECTIVE,
            LABEL,
            VARIABLE,
            UNDEFINED
        }
    }

    val type: LineType
    var line = line.trim()
    val containsComment = ';' in this.line

    init {
        val line = this.line
        type = when {
            line.startsWith(';') -> LineType.COMMENT
            line.startsWith('.') -> LineType.DIRECTIVE
            ':' in line -> LineType.LABEL
            '=' in line && ';' !in line || (';' in line && '=' in line.split(';')[0]) -> LineType.VARIABLE
            else -> LineType.CODE
        }
    }

    fun cleaned() = if(containsComment) line.split(';')[0].trim() else line

    override fun equals(other: Any?): Boolean {
        if(other == null || other !is CodeLine)
            return false
        return line == other.line
    }

    override fun hashCode() = line.hashCode()

    override fun toString() = line

    infix fun isType(type: LineType) = this.type == type

}