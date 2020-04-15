package me.emu6502.kotlinutils.vt100

enum class VT100BackgroundColor: VT100Attribute {

    BLACK, // = 40
    RED,   // = 41
    GREEN, // ...
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE;

    override val attribute: Int = 40 + ordinal // 40...

}