package me.emu6502.lib6502

import me.emu6502.lib6502.AddressMode.*

enum class Instruction(vararg val opCodeAddrModes: Pair<Int, AddressMode>, val fixedOpCode: Int? = null) {

    ADC(0x69 to IMMEDIATE, 0x65 to ZEROPAGE, 0x75 to ZEROPAGE_X, 0x6D to ABSOLUTE,
            0x7D to ABSOLUTE_X, 0x79 to ABSOLUTE_Y, 0x61 to INDEX_X, 0x71 to INDEX_Y),
    AND(0x29 to IMMEDIATE, 0x25 to ZEROPAGE, 0x35 to ZEROPAGE_X, 0x2D to ABSOLUTE,
            0x3D to ABSOLUTE_X, 0x39 to ABSOLUTE_Y, 0x21 to INDEX_X, 0x31 to INDEX_Y),
    ASL(0x0A to ACCUMULATOR, 0x06 to ZEROPAGE, 0x16 to ZEROPAGE_X, 0x0E to ABSOLUTE_Y, 0x1E to ABSOLUTE_X),
    BCC(0x90 to ZEROPAGE),
    BCS(0xB0 to ZEROPAGE),
    BEQ(0xF0 to ZEROPAGE),
    BIT(0x24 to ZEROPAGE, 0x2C to ABSOLUTE),
    BMI(0x30 to ZEROPAGE),
    BNE(0xD0 to ZEROPAGE),
    BPL(0x10 to ZEROPAGE),
    BRK(fixedOpCode = 0x00),
    BVC(0x50 to ZEROPAGE),
    BVS(0x70 to ZEROPAGE),
    CLC(fixedOpCode = 0x18),
    CLD(fixedOpCode = 0xD8),
    CLI(fixedOpCode = 0x58),
    CLV(fixedOpCode = 0xB8),
    CMP(0xC9 to IMMEDIATE, 0xC5 to ZEROPAGE, 0xD5 to ZEROPAGE_X, 0xCD to ABSOLUTE,
            0xDD to ABSOLUTE_X, 0xD9 to ABSOLUTE_Y, 0xC1 to INDEX_X, 0xD1 to INDEX_Y),
    CPX(0xE0 to IMMEDIATE, 0xE4 to ZEROPAGE, 0xEC to ABSOLUTE),
    CPY(0xC0 to IMMEDIATE, 0xC4 to ZEROPAGE, 0xCC to ABSOLUTE),
    DEC(0xC6 to ZEROPAGE, 0xD6 to ZEROPAGE_X, 0xCE to ABSOLUTE, 0xDE to ABSOLUTE_X),
    DEX(fixedOpCode = 0xCA),
    DEY(fixedOpCode = 0x88),
    EOR(0x49 to IMMEDIATE, 0x45 to ZEROPAGE, 0x55 to ZEROPAGE_X, 0x45 to ABSOLUTE,
            0x5D to ABSOLUTE_X, 0x59 to ABSOLUTE_Y, 0x41 to INDEX_X, 0x51 to INDEX_Y),
    INC(0xE6 to ZEROPAGE, 0xF6 to ZEROPAGE_X, 0xEE to ABSOLUTE, 0xFE to ABSOLUTE_X),
    INX(fixedOpCode = 0xE8),
    INY(fixedOpCode = 0xC8),
    JMP(0x4C to ABSOLUTE, 0x6C to INDIRECT),
    JSR(0x20 to ABSOLUTE),
    LDA(0xA9 to IMMEDIATE, 0xA5 to ZEROPAGE, 0xB5 to ZEROPAGE_X, 0xAD to ABSOLUTE,
            0xBD to ABSOLUTE_X, 0xB9 to ABSOLUTE_Y, 0xA1 to INDEX_X, 0xB1 to INDEX_Y),
    LDX(0xA2 to IMMEDIATE, 0xA6 to ZEROPAGE, 0xB6 to ZEROPAGE_Y, 0xAE to ABSOLUTE, 0xBE to ABSOLUTE_Y),
    LDY(0xA0 to IMMEDIATE, 0xA4 to ZEROPAGE, 0xB4 to ZEROPAGE_X, 0xAC to ABSOLUTE, 0xBC to ABSOLUTE_X),
    LSR(0xA4 to ACCUMULATOR, 0x46 to ZEROPAGE, 0x56 to ZEROPAGE_X, 0x4E to ABSOLUTE, 0x5E to ABSOLUTE_X),
    NOP(fixedOpCode = 0xEA),
    ORA(0x09 to IMMEDIATE, 0x05 to ZEROPAGE, 0x15 to ZEROPAGE_X, 0x0D to ABSOLUTE,
            0x1D to ABSOLUTE_X, 0x19 to ABSOLUTE_Y, 0x01 to INDEX_X, 0x11 to INDEX_Y),
    PHA(fixedOpCode = 0x48),
    PHP(fixedOpCode = 0x08),
    PLA(fixedOpCode = 0x68),
    PLP(fixedOpCode = 0x28),
    ROL(0x2A to ACCUMULATOR, 0x26 to ZEROPAGE, 0x36 to ZEROPAGE_X, 0x2E to ABSOLUTE, 0x3E to ABSOLUTE_X),
    ROR(0x6A to ACCUMULATOR, 0x66 to ZEROPAGE, 0x76 to ZEROPAGE_X, 0x6E to ABSOLUTE, 0x7E to ABSOLUTE_X),
    RTI(fixedOpCode = 0x40),
    RTS(fixedOpCode = 0x60),
    SBC(0xE9 to IMMEDIATE, 0xE5 to ZEROPAGE, 0xF5 to ZEROPAGE_X, 0xED to ABSOLUTE,
            0xFD to ABSOLUTE_X, 0xF9 to ABSOLUTE_Y, 0xE1 to INDEX_X, 0xF1 to INDEX_Y),
    SEC(fixedOpCode = 0x38),
    SED(fixedOpCode = 0xF8),
    SEI(fixedOpCode = 0x78),
    STA(0x85 to ZEROPAGE, 0x95 to ZEROPAGE_X, 0x8D to ABSOLUTE, 0x9D to ABSOLUTE_X,
            0x99 to ABSOLUTE_Y, 0x81 to INDEX_X, 0x91 to INDEX_Y),
    STX(0x86 to ZEROPAGE, 0x96 to ZEROPAGE_Y, 0x8E to ABSOLUTE),
    STY(0x84 to ZEROPAGE, 0x94 to ZEROPAGE_X, 0x8C to ABSOLUTE),
    TAX(fixedOpCode = 0xAA),
    TAY(fixedOpCode = 0xA8),
    TSX(fixedOpCode = 0xBA),
    TXA(fixedOpCode = 0x8A),
    TXS(fixedOpCode = 0x9A),
    TYA(fixedOpCode = 0x98);

    fun findAddressMode(opCode: Int): AddressMode? =
            opCodeAddrModes.firstOrNull { (instrOpCode, _) -> instrOpCode == opCode }?.second

    companion object {
        fun find(opCode: Int): Instruction? = values().firstOrNull() {
            opCode == it.fixedOpCode || it.findAddressMode(opCode) != null
        }
    }

}