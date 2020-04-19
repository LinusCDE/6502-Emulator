package me.emu6502.kotlinutils.vt100

enum class VT100Display(override val attribute: Int): VT100Attribute {

    RESET_ALL_ATTRIBUTES(0),
    BRIGHT(1),
    DIM(2),
    UNDERSCORE(4),
    BLINK(5),
    REVERSE(7),
    HIDDEN(8);
}