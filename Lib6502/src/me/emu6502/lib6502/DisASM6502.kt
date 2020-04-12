package me.emu6502.lib6502

import toString
import ubyte

class DisASM6502 {
    companion object {
        fun Disassemble(code: UByteArray, pc: Int): String {
            var pc = pc
            return when (code[pc].toInt()) {
                0x00 -> "BRK"
                0x01 -> IndirectXCode("ORA", code[++pc])
                0x05 -> ZeropageCode("ORA", code[++pc])
                0x06 -> ZeropageCode("ASL", code[++pc])
                0x08 -> "PHP"
                0x09 -> ImmediateCode("ORA", code[++pc])
                0x0A -> "ASL"
                0x0D -> AbsCode("ORA", code[++pc], code[++pc])
                0x0E -> AbsCode("ASL", code[++pc], code[++pc])
                0x10 -> RelativeCode("BPL", code[++pc])
                0x11 -> IndirectYCode("ORA", code[++pc])
                0x15 -> ZeropageXCode("ORA", code[++pc])
                0x16 -> ZeropageXCode("ASL", code[++pc])
                0x18 -> "CLC"
                0x19 -> AbsYCode("ORA", code[++pc], code[++pc])
                0x1D -> AbsXCode("ORA", code[++pc], code[++pc])
                0x1E -> AbsXCode("ASL", code[++pc], code[++pc])
                0x20 -> AbsCode("JSR", code[++pc], code[++pc])
                0x21 -> IndirectXCode("AND", code[++pc])
                0x24 -> ZeropageCode("BIT", code[++pc])
                0x25 -> ZeropageCode("AND", code[++pc])
                0x26 -> ZeropageCode("ROL", code[++pc])
                0x28 -> "PLP"
                0x29 -> ImmediateCode("AND", code[++pc])
                0x2A -> "ROL"
                0x2C -> AbsCode("BIT", code[++pc], code[++pc])
                0x2D -> AbsCode("AND", code[++pc], code[++pc])
                0x2E -> AbsCode("ROL", code[++pc], code[++pc])
                0x30 -> RelativeCode("BMI", code[++pc])
                0x31 -> IndirectYCode("AND", code[++pc])
                0x35 -> ZeropageXCode("AND", code[++pc])
                0x36 -> ZeropageXCode("ROL", code[++pc])
                0x38 -> "SEC"
                0x39 -> AbsYCode("AND", code[++pc], code[++pc])
                0x3D -> AbsXCode("AND", code[++pc], code[++pc])
                0x3E -> AbsXCode("ROL", code[++pc], code[++pc])
                0x40 -> "RTI"
                0x41 -> IndirectXCode("EOR", code[++pc])
                0x45 -> ZeropageCode("EOR", code[++pc])
                0x46 -> ZeropageCode("LSR", code[++pc])
                0x48 -> "PHA"
                0x49 -> ImmediateCode("EOR", code[++pc])
                0x4A -> "LSR"
                0x4C -> AbsCode("JMP", code[++pc], code[++pc])
                0x4D -> AbsCode("EOR", code[++pc], code[++pc])
                0x4E -> AbsCode("LSR", code[++pc], code[++pc])
                0x50 -> RelativeCode("BVC", code[++pc])
                0x51 -> IndirectYCode("EOR", code[++pc])
                0x55 -> ZeropageXCode("EOR", code[++pc])
                0x56 -> ZeropageXCode("LSR", code[++pc])
                0x58 -> "CLI"
                0x59 -> AbsYCode("EOR", code[++pc], code[++pc])
                0x5D -> AbsXCode("EOR", code[++pc], code[++pc])
                0x5E -> AbsXCode("LSR", code[++pc], code[++pc])
                0x60 -> "RTS"
                0x61 -> IndirectXCode("ADC", code[++pc])
                0x65 -> ZeropageCode("ADC", code[++pc])
                0x66 -> ZeropageCode("ROR", code[++pc])
                0x68 -> "PLA"
                0x69 -> ImmediateCode("ADC", code[++pc])
                0x6A -> "ROR"
                0x6C -> IndirectCode("JMP", code[++pc], code[++pc])
                0x6D -> AbsCode("ADC", code[++pc], code[++pc])
                0x6E -> AbsCode("ROR", code[++pc], code[++pc])
                0x70 -> RelativeCode("BVS", code[++pc])
                0x71 -> IndirectYCode("ADC", code[++pc])
                0x75 -> ZeropageXCode("ADC", code[++pc])
                0x76 -> ZeropageXCode("ROR", code[++pc])
                0x78 -> "SEI"
                0x79 -> AbsYCode("ADC", code[++pc], code[++pc])
                0x7D -> AbsXCode("ADC", code[++pc], code[++pc])
                0x7E -> AbsXCode("ROR", code[++pc], code[++pc])
                0x81 -> IndirectXCode("STA", code[++pc])
                0x84 -> ZeropageCode("STY", code[++pc])
                0x85 -> ZeropageCode("STA", code[++pc])
                0x86 -> ZeropageCode("STX", code[++pc])
                0x88 -> "DEY"
                0x8A -> "TXA"
                0x8C -> AbsCode("STY", code[++pc], code[++pc])
                0x8D -> AbsCode("STA", code[++pc], code[++pc])
                0x8E -> AbsCode("STX", code[++pc], code[++pc])
                0x90 -> RelativeCode("BCC", code[++pc])
                0x91 -> IndirectYCode("STA", code[++pc])
                0x94 -> ZeropageXCode("STY", code[++pc])
                0x95 -> ZeropageXCode("STA", code[++pc])
                0x96 -> ZeropageYCode("STX", code[++pc])
                0x98 -> "TYA"
                0x99 -> AbsYCode("STA", code[++pc], code[++pc])
                0x9A -> "TXS"
                0x9D -> AbsXCode("STA", code[++pc], code[++pc])
                0xA0 -> ImmediateCode("LDY", code[++pc])
                0xA1 -> IndirectXCode("LDA", code[++pc])
                0xA2 -> ImmediateCode("LDX", code[++pc])
                0xA4 -> ZeropageCode("LDY", code[++pc])
                0xA5 -> ZeropageCode("LDA", code[++pc])
                0xA6 -> ZeropageCode("LDX", code[++pc])
                0xA8 -> "TAY"
                0xA9 -> ImmediateCode("LDA", code[++pc])
                0xAA -> "TAX"
                0xAC -> AbsCode("LDY", code[++pc], code[++pc])
                0xAD -> AbsCode("LDA", code[++pc], code[++pc])
                0xAE -> AbsCode("LDX", code[++pc], code[++pc])
                0xB0 -> RelativeCode("BCS", code[++pc])
                0xB1 -> IndirectYCode("LDA", code[++pc])
                0xB4 -> ZeropageXCode("LDY", code[++pc])
                0xB5 -> ZeropageXCode("LDA", code[++pc])
                0xB6 -> ZeropageYCode("LDX", code[++pc])
                0xB8 -> "CLV"
                0xB9 -> AbsYCode("LDA", code[++pc], code[++pc])
                0xBA -> "TSX"
                0xBC -> AbsXCode("LDY", code[++pc], code[++pc])
                0xBD -> AbsXCode("LDA", code[++pc], code[++pc])
                0xBE -> AbsYCode("LDX", code[++pc], code[++pc])
                0xC0 -> ImmediateCode("CPY", code[++pc])
                0xC1 -> IndirectXCode("CMP", code[++pc])
                0xC4 -> ZeropageCode("CPY", code[++pc])
                0xC5 -> ZeropageCode("CMP", code[++pc])
                0xC6 -> ZeropageCode("DEC", code[++pc])
                0xC8 -> "INY"
                0xC9 -> ImmediateCode("CMP", code[++pc])
                0xCA -> "DEX"
                0xCC -> AbsCode("CPY", code[++pc], code[++pc])
                0xCD -> AbsCode("CMP", code[++pc], code[++pc])
                0xCE -> AbsCode("DEC", code[++pc], code[++pc])
                0xD0 -> RelativeCode("BNE", code[++pc])
                0xD1 -> IndirectYCode("CMP", code[++pc])
                0xD5 -> ZeropageXCode("CMP", code[++pc])
                0xD6 -> ZeropageYCode("DEC", code[++pc])
                0xD8 -> "CLD"
                0xD9 -> AbsYCode("CMP", code[++pc], code[++pc])
                0xDD -> AbsXCode("CMP", code[++pc], code[++pc])
                0xDE -> AbsXCode("DEC", code[++pc], code[++pc])
                0xE0 -> ImmediateCode("CPX", code[++pc])
                0xE1 -> IndirectXCode("SBC", code[++pc])
                0xE4 -> ZeropageCode("CPX", code[++pc])
                0xE5 -> ZeropageCode("SBC", code[++pc])
                0xE6 -> ZeropageCode("INC", code[++pc])
                0xE8 -> "INX"
                0xE9 -> ImmediateCode("SBC", code[++pc])
                0xEA -> "NOP"
                0xEC -> AbsCode("CPX", code[++pc], code[++pc])
                0xED -> AbsCode("SBC", code[++pc], code[++pc])
                0xEE -> AbsCode("INC", code[++pc], code[++pc])
                0xF0 -> RelativeCode("BEQ", code[++pc])
                0xF1 -> IndirectYCode("SBC", code[++pc])
                0xF5 -> ZeropageXCode("SBC", code[++pc])
                0xF6 -> ZeropageXCode("INC", code[++pc])
                0xF8 -> "SED"
                0xF9 -> AbsYCode("SBC", code[++pc], code[++pc])
                0xFD -> AbsXCode("SBC", code[++pc], code[++pc])
                0xFE -> AbsXCode("INC", code[++pc], code[++pc])
                else -> "Data " + code[0].toString(16).toUpperCase()
            }
        }

        fun Disassemble(code: String): String {
            val sbytes = code.split(' ');
            val bbytes = UByteArray(sbytes.size, { i ->
                HexStringToByte(sbytes[i])
            })
            return Disassemble(bbytes, 0);
        }

        fun HexStringToByte(stringbyte: String): UByte = Integer.parseUnsignedInt(stringbyte, 16).ubyte

        //region Code Strings
        private fun ZeropageCode(mnemonic: String, address: UByte) = "$mnemonic ${address.toString("X2")}";
        private fun ZeropageXCode(mnemonic: String, address: UByte) = "${ZeropageCode(mnemonic, address)}, X";
        private fun ZeropageYCode(mnemonic: String, address: UByte) = "${ZeropageCode(mnemonic, address)}, Y";
        private fun AbsCode(mnemonic: String, low: UByte, high: UByte) = "$mnemonic ${high.toString("X2")}${low.toString("X2")}";
        private fun AbsXCode(mnemonic: String, low: UByte, high: UByte) = "${AbsCode(mnemonic, low, high)}, X";
        private fun AbsYCode(mnemonic: String, low: UByte, high: UByte) = "${AbsCode(mnemonic, low, high)}, Y";
        private fun ImmediateCode(mnemonic: String, value: UByte) = "$mnemonic #${value.toString("X2")}";
        private fun IndirectCode(mnemonic: String, low: UByte, high: UByte) = "$mnemonic (${high.toString("X2")}${low.toString("X2")})";
        private fun IndirectXCode(mnemonic: String, address: UByte) = "$mnemonic (${address.toString("X2")}, X)";
        private fun IndirectYCode(mnemonic: String, address: UByte) = "$mnemonic (${address.toString("X2")}), Y";
        private fun RelativeCode(mnemonic: String, value: UByte) = "$mnemonic ${value.toString("X2")}";
        //endregion
    }
}