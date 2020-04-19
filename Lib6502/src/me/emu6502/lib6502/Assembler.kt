package me.emu6502.lib6502

import me.emu6502.kotlinutils.*
import java.io.ByteArrayOutputStream
import java.lang.NumberFormatException

class Assembler {
    companion object {

        fun assemble(code: String): UByteArray {
            val lines = code.split("\n")
                    .map { it.trim() }
                    .filter{ !it.startsWith(";") }
                    .filter{ it.isNotBlank() }

            val binary = ByteArrayOutputStream()

            for(line in lines) {
                val spacesplit = line.split(' ')
                val mnemonic = spacesplit[0]
                val operator = if(spacesplit.size > 1) spacesplit[1] else ""
                var addr: UShort = 0.ushort
                var op1: UByte = 0.ubyte
                val instruction = Instruction.values().firstOrNull {
                    it.name.equals(mnemonic, true)
                } ?: throw AssembleException("Unbekanntes Mnemonic: $line")

                var compiled = false
                if(operator == "") {
                    if(instruction.fixedOpCode != null && instruction.opCodeAddrModes.isEmpty()) {
                        binary.write(instruction.fixedOpCode)
                        compiled = true
                    }
                }else {
                    for((opcode, addrMode) in instruction.opCodeAddrModes) {
                        val parsed = addrMode.parse(operator) ?: continue
                        binary.write(opcode)
                        binary.write(parsed.toByteArray())
                        compiled = true
                        break
                    }
                }

                if(!compiled)
                    throw AssembleException("Operator falsch oder nicht unterstützt: $line")
            }

            return binary.toByteArray().toUByteArray()
        }

    }
}