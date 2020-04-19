package me.emu6502.lib6502

import me.emu6502.kotlinutils.*

class Disassembler {
    companion object {

        fun disassembleVerbose(code: UByteArray, memoryAddress: Int): Pair<String/*ASM*/, Int/*Operator size*/> {
            val opCode = code[memoryAddress].int
            val instruction = Instruction.find(opCode) ?: return "Data ${opCode.toString(16).toUpperCase()}" to 0
            val addrMode = instruction.findAddressMode(opCode) ?: return instruction.name to 0
            return if (memoryAddress + addrMode.addressBytes < code.size) {
                val bytes = UByteArray(addrMode.addressBytes) { i -> code[memoryAddress + i + 1] }
                "${instruction.name} ${addrMode.toString(bytes)}" to addrMode.addressBytes
            } else
                "${instruction.name} ???" to addrMode.addressBytes
        }

        fun disassemble(code: UByteArray, memoryAddress: Int): String = disassembleVerbose(code, memoryAddress).first

    }
}