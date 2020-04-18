package me.emu6502.kotlinutils.vt100

import java.lang.Exception

/**
 * See http://www.termsys.demon.co.uk/vtansi.htm
 */
enum class VT100Sequence(val syntax: String, // Just for documentation
                         val code: Char, // The char at the end
                         val attributes: Int = 0, // How many numbers are contained (0 = no; -1 = 0 to infinity) Attributes are separated by ";"
                         val prefix: String = "[",
                         val codePrefixValue: Int = -1 // The code is also defined by a fixed attribute value (-1 = not applying here) (e.g. "<ESC>[5n")
) {

    QUERY_DEVICE_CODE(          "<ESC>[c", 'c'),
    REPORT_DEVICE_CODE(         "<ESC>[{code}0c", 'c', codePrefixValue = 0),
    QUERY_DEVICE_STATUS(        "<ESC>[5n", 'n', codePrefixValue = 5),
    REPORT_DEVICE_OK(           "<ESC>[0n", 'n', codePrefixValue = 0),
    REPORT_DEVICE_FAILURE(      "<ESC>[3n", 'n', codePrefixValue = 3),
    QUERY_CURSOR_POSITION(      "<ESC>[6n", 'n', codePrefixValue = 6),
    REPORT_CURSOR_POSITION(     "<ESC>[{ROW};{COLUMN}R", 'R', attributes = 2),
    RESET_DEVICE(               "<ESC>c", 'c', prefix = ""),
    ENABLE_LINE_WRAP(           "<ESC>[7h", 'h', codePrefixValue = 7),
    DISABLE_LINE_WRAP(          "<ESC>[7l", 'l', codePrefixValue = 7),
    FONT_SET_G0_DEFAULT(        "<ESC>(", '(', prefix = ""),
    FONT_SET_G1_ALTERNATE(      "<ESC>)", ')', prefix = ""),
    CURSOR_HOME(                "<ESC>[H", 'H'),
    CURSOR_HOME_TO(             "<ESC>[{ROW};{COLUMN}H", 'H', attributes = 2),
    CURSOR_UP_ONE(              "<ESC>[A", 'A'),
    CURSOR_UP(                  "<ESC>[{COUNT}A", 'A', attributes = 1),
    CURSOR_DOWN_ONE(            "<ESC>[B", 'B'),
    CURSOR_DOWN(                "<ESC>[{COUNT}B", 'B', attributes = 1),
    CURSOR_FORWARD_ONE(         "<ESC>[C", 'C'),
    CURSOR_FORWARD(             "<ESC>[{COUNT}C", 'C', attributes = 1),
    CURSOR_BACKWARD_ONE(        "<ESC>[D", 'D'),
    CURSOR_BACKWARD(            "<ESC>[{COUNT}D", 'D', attributes = 1),
    FORCE_CURSOR_POSITION(      "<ESC>[{ROW};{COLUMN}f", 'f', attributes = 2),
    SAVE_CURSOR(                "<ESC>[s", 's'),
    UNSAVE_CURSOR(              "<ESC>[u", 'u'),
    SAVE_CURSOR_AND_ATTRS(      "<ESC>7", '7', prefix = ""),
    RESTORE_CURSOR_AND_ATTRS(   "<ESC>8", '8', prefix = ""),
    SCROLL_SCREEN(              "<ESC>[r", 'r'),
    SCROLL_SCREEN_FROM_TO(      "<ESC>[{start};{end}r", 'r', attributes = 2),
    SCROLL_DOWN(                "<ESC>D", 'D'),
    SCROLL_UP(                  "<ESC>M", 'M'),
    SET_TAB_(                   "<ESC>H", 'H'),
    CLEAR_TAB(                 "<ESC>[g", 'g'),
    CLEAR_ALL_TABS(            "<ESC>[3g", 'g', codePrefixValue = 3),
    ERASE_END_OF_LINE(          "<ESC>[K", 'K'),
    ERASE_START_OF_LINE(        "<ESC>[1K", 'K', codePrefixValue = 1),
    ERASE_LINE(                 "<ESC>[2K", 'K', codePrefixValue = 2),
    ERASE_DOWN(                 "<ESC>[J", 'J'),
    ERASE_UP(                   "<ESC>[1J", 'J', codePrefixValue = 1),
    ERASE_SCREEN(               "<ESC>[2J", 'J', codePrefixValue = 2),
    PRINT_SCREEN(               "<ESC>[i", 'i'),
    PRINT_LINE(                 "<ESC>[1i", 'i', codePrefixValue = 1),
    STOP_PRINT_LOG(             "<ESC>[4i", 'i', codePrefixValue = 4),
    START_PRINT_LOG(            "<ESC>[5i", 'i', codePrefixValue = 5),
    //SET_KEY_DEFINITION(         "<ESC>[{key};\"{string}\"p", 'p', attributes = 2), // Currently not supported
    SET_NEW_LINE(         "<ESC>[[20h", 'h', prefix = "[[", codePrefixValue = 20),
    SET_LINE_FEED(         "<ESC>[[20l", 'l', prefix = "[[", codePrefixValue = 20),
    SET_ATTRIBUTE_MODE(         "<ESC>[{attr1};...;{attrn}m", 'm', attributes = -1);

    val areInfiniteAttributesAllowed by lazy { attributes == -1 }
    val areNoAttributesAllowed by lazy { attributes == 0 }
    val rawPrefix by lazy { "$ESC$prefix" }
    val rawSuffix by lazy { "${if(codePrefixValue == -1) "" else codePrefixValue}$code" }

    /**
     * Reads the attributes of a command with a assumed sequence
     * @return null if sequence is not this one, otherwise array with arguments
     */
    fun parseAttributes(sequence: String): Array<Int>? {
        // Get and verify prefix
        if(!sequence.startsWith(rawPrefix))
            return null

        // Get and verify suffix
        if(!sequence.endsWith(rawSuffix))
            return null

        val rawAttributes: String
        try {
            val start = rawPrefix.length
            val end = sequence.length - rawSuffix.length
            rawAttributes = if (end - start == 0) "" else sequence.substring(rawPrefix.length, end)
        }catch (e: StringIndexOutOfBoundsException) {
            return null
        }

        // Parse all attributes
        lateinit var attributes: Array<Int>
        try {
            // expected: "" or "num" or "num;num" or "num;num;num" or ...
            attributes = if(";" in rawAttributes)
                rawAttributes.split(';').toTypedArray().map { it.toInt() }.toTypedArray()
            else if(rawAttributes != "")
                arrayOf(rawAttributes.toInt())
            else
                arrayOf()
        }catch (e: Exception) {
            return null // Failed to parse attributes
        }

        // Ok if amount of attributes = expected attributes
        return if(this.attributes == attributes.size || this.attributes == -1) attributes else null
    }

    override fun toString() = toString(*IntArray(0))!!
    fun toString(vararg attributes: VT100Attribute): String? = toString(*attributes.map { it.attribute }.toIntArray())
    fun toString(vararg attributes: Int): String? {
        if(this.attributes != -1 && attributes.size != this.attributes)
            return null

        return rawPrefix + attributes.joinToString(";") + rawSuffix
    }


    companion object {
        public const val ESC = 0x1B.toChar() // Prefix for VT100 codes


        private fun isValid(sequence: String, vtSeq: VT100Sequence) = vtSeq.parseAttributes(sequence) != null

        /**
         * Find out the escape sequence
         * @param text The text that contains a valid sequence
         * @return Data for the first escape sequence found in text
         */
        fun parse(text: String): Triple<String, VT100Sequence, Array<Int>>? {
            var text = text
            if(! text.contains(ESC))
                return null // No ESC in text

            text = text.substring(text.indexOf(ESC))
            if(text.length == 1)
                return null // Text ends with ESC

            text = text.substring(1) // Skip ESC

            for(end in 1..text.length) {
                val possibleSequence = "$ESC" + text.substring(0, end)
                // Iterate codes with codePrefixValue before the others
                for(vtSeq in values().filter { it.codePrefixValue != -1 } + values().filter { it.codePrefixValue == -1 }) {
                    val attrs = vtSeq.parseAttributes(possibleSequence)
                    if(attrs != null)
                        return Triple(possibleSequence, vtSeq, attrs)
                }
            }

            return null
        }
    }
}