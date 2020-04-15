package me.emu6502.kotlinutils.vt100

enum class VT100Display: VT100Attribute {

    RESET_ALL_ATTRIBUTES,
    BRIGHT,
    DIM,
    UNDERSCORE,
    BLINK,
    REVERSE,
    HIDDEN;

    override val attribute: Int = ordinal // 0 ...

}