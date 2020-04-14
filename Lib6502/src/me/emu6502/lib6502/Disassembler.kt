package me.emu6502.lib6502

import toString
import ubyte

class Disassembler {
    companion object {

        fun disassemble(code: UByteArray, pc: Int): String {
            var pc = pc
            return when (code[pc].toInt()) {
                0x00 -> "BRK"
                0x01 -> indirectXCode("ORA", code[++pc])
                0x05 -> zeropageCode("ORA", code[++pc])
                0x06 -> zeropageCode("ASL", code[++pc])
                0x08 -> "PHP"
                0x09 -> immediateCode("ORA", code[++pc])
                0x0A -> "ASL"
                0x0D -> absCode("ORA", code[++pc], code[++pc])
                0x0E -> absCode("ASL", code[++pc], code[++pc])
                0x10 -> relativeCode("BPL", code[++pc])
                0x11 -> indirectYCode("ORA", code[++pc])
                0x15 -> zeropageXCode("ORA", code[++pc])
                0x16 -> zeropageXCode("ASL", code[++pc])
                0x18 -> "CLC"
                0x19 -> absYCode("ORA", code[++pc], code[++pc])
                0x1D -> absXCode("ORA", code[++pc], code[++pc])
                0x1E -> absXCode("ASL", code[++pc], code[++pc])
                0x20 -> absCode("JSR", code[++pc], code[++pc])
                0x21 -> indirectXCode("AND", code[++pc])
                0x24 -> zeropageCode("BIT", code[++pc])
                0x25 -> zeropageCode("AND", code[++pc])
                0x26 -> zeropageCode("ROL", code[++pc])
                0x28 -> "PLP"
                0x29 -> immediateCode("AND", code[++pc])
                0x2A -> "ROL"
                0x2C -> absCode("BIT", code[++pc], code[++pc])
                0x2D -> absCode("AND", code[++pc], code[++pc])
                0x2E -> absCode("ROL", code[++pc], code[++pc])
                0x30 -> relativeCode("BMI", code[++pc])
                0x31 -> indirectYCode("AND", code[++pc])
                0x35 -> zeropageXCode("AND", code[++pc])
                0x36 -> zeropageXCode("ROL", code[++pc])
                0x38 -> "SEC"
                0x39 -> absYCode("AND", code[++pc], code[++pc])
                0x3D -> absXCode("AND", code[++pc], code[++pc])
                0x3E -> absXCode("ROL", code[++pc], code[++pc])
                0x40 -> "RTI"
                0x41 -> indirectXCode("EOR", code[++pc])
                0x45 -> zeropageCode("EOR", code[++pc])
                0x46 -> zeropageCode("LSR", code[++pc])
                0x48 -> "PHA"
                0x49 -> immediateCode("EOR", code[++pc])
                0x4A -> "LSR"
                0x4C -> absCode("JMP", code[++pc], code[++pc])
                0x4D -> absCode("EOR", code[++pc], code[++pc])
                0x4E -> absCode("LSR", code[++pc], code[++pc])
                0x50 -> relativeCode("BVC", code[++pc])
                0x51 -> indirectYCode("EOR", code[++pc])
                0x55 -> zeropageXCode("EOR", code[++pc])
                0x56 -> zeropageXCode("LSR", code[++pc])
                0x58 -> "CLI"
                0x59 -> absYCode("EOR", code[++pc], code[++pc])
                0x5D -> absXCode("EOR", code[++pc], code[++pc])
                0x5E -> absXCode("LSR", code[++pc], code[++pc])
                0x60 -> "RTS"
                0x61 -> indirectXCode("ADC", code[++pc])
                0x65 -> zeropageCode("ADC", code[++pc])
                0x66 -> zeropageCode("ROR", code[++pc])
                0x68 -> "PLA"
                0x69 -> immediateCode("ADC", code[++pc])
                0x6A -> "ROR"
                0x6C -> indirectCode("JMP", code[++pc], code[++pc])
                0x6D -> absCode("ADC", code[++pc], code[++pc])
                0x6E -> absCode("ROR", code[++pc], code[++pc])
                0x70 -> relativeCode("BVS", code[++pc])
                0x71 -> indirectYCode("ADC", code[++pc])
                0x75 -> zeropageXCode("ADC", code[++pc])
                0x76 -> zeropageXCode("ROR", code[++pc])
                0x78 -> "SEI"
                0x79 -> absYCode("ADC", code[++pc], code[++pc])
                0x7D -> absXCode("ADC", code[++pc], code[++pc])
                0x7E -> absXCode("ROR", code[++pc], code[++pc])
                0x81 -> indirectXCode("STA", code[++pc])
                0x84 -> zeropageCode("STY", code[++pc])
                0x85 -> zeropageCode("STA", code[++pc])
                0x86 -> zeropageCode("STX", code[++pc])
                0x88 -> "DEY"
                0x8A -> "TXA"
                0x8C -> absCode("STY", code[++pc], code[++pc])
                0x8D -> absCode("STA", code[++pc], code[++pc])
                0x8E -> absCode("STX", code[++pc], code[++pc])
                0x90 -> relativeCode("BCC", code[++pc])
                0x91 -> indirectYCode("STA", code[++pc])
                0x94 -> zeropageXCode("STY", code[++pc])
                0x95 -> zeropageXCode("STA", code[++pc])
                0x96 -> zeropageYCode("STX", code[++pc])
                0x98 -> "TYA"
                0x99 -> absYCode("STA", code[++pc], code[++pc])
                0x9A -> "TXS"
                0x9D -> absXCode("STA", code[++pc], code[++pc])
                0xA0 -> immediateCode("LDY", code[++pc])
                0xA1 -> indirectXCode("LDA", code[++pc])
                0xA2 -> immediateCode("LDX", code[++pc])
                0xA4 -> zeropageCode("LDY", code[++pc])
                0xA5 -> zeropageCode("LDA", code[++pc])
                0xA6 -> zeropageCode("LDX", code[++pc])
                0xA8 -> "TAY"
                0xA9 -> immediateCode("LDA", code[++pc])
                0xAA -> "TAX"
                0xAC -> absCode("LDY", code[++pc], code[++pc])
                0xAD -> absCode("LDA", code[++pc], code[++pc])
                0xAE -> absCode("LDX", code[++pc], code[++pc])
                0xB0 -> relativeCode("BCS", code[++pc])
                0xB1 -> indirectYCode("LDA", code[++pc])
                0xB4 -> zeropageXCode("LDY", code[++pc])
                0xB5 -> zeropageXCode("LDA", code[++pc])
                0xB6 -> zeropageYCode("LDX", code[++pc])
                0xB8 -> "CLV"
                0xB9 -> absYCode("LDA", code[++pc], code[++pc])
                0xBA -> "TSX"
                0xBC -> absXCode("LDY", code[++pc], code[++pc])
                0xBD -> absXCode("LDA", code[++pc], code[++pc])
                0xBE -> absYCode("LDX", code[++pc], code[++pc])
                0xC0 -> immediateCode("CPY", code[++pc])
                0xC1 -> indirectXCode("CMP", code[++pc])
                0xC4 -> zeropageCode("CPY", code[++pc])
                0xC5 -> zeropageCode("CMP", code[++pc])
                0xC6 -> zeropageCode("DEC", code[++pc])
                0xC8 -> "INY"
                0xC9 -> immediateCode("CMP", code[++pc])
                0xCA -> "DEX"
                0xCC -> absCode("CPY", code[++pc], code[++pc])
                0xCD -> absCode("CMP", code[++pc], code[++pc])
                0xCE -> absCode("DEC", code[++pc], code[++pc])
                0xD0 -> relativeCode("BNE", code[++pc])
                0xD1 -> indirectYCode("CMP", code[++pc])
                0xD5 -> zeropageXCode("CMP", code[++pc])
                0xD6 -> zeropageYCode("DEC", code[++pc])
                0xD8 -> "CLD"
                0xD9 -> absYCode("CMP", code[++pc], code[++pc])
                0xDD -> absXCode("CMP", code[++pc], code[++pc])
                0xDE -> absXCode("DEC", code[++pc], code[++pc])
                0xE0 -> immediateCode("CPX", code[++pc])
                0xE1 -> indirectXCode("SBC", code[++pc])
                0xE4 -> zeropageCode("CPX", code[++pc])
                0xE5 -> zeropageCode("SBC", code[++pc])
                0xE6 -> zeropageCode("INC", code[++pc])
                0xE8 -> "INX"
                0xE9 -> immediateCode("SBC", code[++pc])
                0xEA -> "NOP"
                0xEC -> absCode("CPX", code[++pc], code[++pc])
                0xED -> absCode("SBC", code[++pc], code[++pc])
                0xEE -> absCode("INC", code[++pc], code[++pc])
                0xF0 -> relativeCode("BEQ", code[++pc])
                0xF1 -> indirectYCode("SBC", code[++pc])
                0xF5 -> zeropageXCode("SBC", code[++pc])
                0xF6 -> zeropageXCode("INC", code[++pc])
                0xF8 -> "SED"
                0xF9 -> absYCode("SBC", code[++pc], code[++pc])
                0xFD -> absXCode("SBC", code[++pc], code[++pc])
                0xFE -> absXCode("INC", code[++pc], code[++pc])
                else -> "Data " + code[0].toString(16).toUpperCase()
            }
        }

        fun disassemble(code: String): String {
            val sbytes = code.split(' ');
            val bbytes = UByteArray(sbytes.size) { i ->
                hexStringToByte(sbytes[i])
            }
            return disassemble(bbytes, 0);
        }

        fun hexStringToByte(stringbyte: String): UByte = Integer.parseUnsignedInt(stringbyte, 16).ubyte

        //region Code Strings
        private fun zeropageCode(mnemonic: String, address: UByte) = "$mnemonic \$${address.toString("X2")}";
        private fun zeropageXCode(mnemonic: String, address: UByte) = "${zeropageCode(mnemonic, address)}, X";
        private fun zeropageYCode(mnemonic: String, address: UByte) = "${zeropageCode(mnemonic, address)}, Y";
        private fun absCode(mnemonic: String, low: UByte, high: UByte) = "$mnemonic \$${high.toString("X2")}${low.toString("X2")}";
        private fun absXCode(mnemonic: String, low: UByte, high: UByte) = "${absCode(mnemonic, low, high)}, X";
        private fun absYCode(mnemonic: String, low: UByte, high: UByte) = "${absCode(mnemonic, low, high)}, Y";
        private fun immediateCode(mnemonic: String, value: UByte) = "$mnemonic #\$${value.toString("X2")}";
        private fun indirectCode(mnemonic: String, low: UByte, high: UByte) = "$mnemonic (\$${high.toString("X2")}${low.toString("X2")})";
        private fun indirectXCode(mnemonic: String, address: UByte) = "$mnemonic (\$${address.toString("X2")}, X)";
        private fun indirectYCode(mnemonic: String, address: UByte) = "$mnemonic (\$${address.toString("X2")}), Y";
        private fun relativeCode(mnemonic: String, value: UByte) = "$mnemonic \$${value.toString("X2")}";
        //endregion
    }
}