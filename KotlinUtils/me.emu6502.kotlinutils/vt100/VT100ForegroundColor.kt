package vt100

enum class VT100ForegroundColor: VT100Attribute {

    BLACK, // = 30
    RED,   // = 31
    GREEN, // ...
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE;

    override val attribute: Int = 30 + ordinal

}