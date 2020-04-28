package me.emu6502.lib6502

import me.emu6502.kotlinutils.*

class Disassembler {
    companion object {

        fun disassembleVerbose(code: UByteArray, memoryAddress: Int): Pair<String/*ASM*/, Int/*Operator size*/> {
            val opCode = code[memoryAddress].int
            val instruction = Instruction.find(opCode) ?: return "Data $${opCode.toString(16).toUpperCase()}" to 0
            val addrMode = instruction.findAddressMode(opCode) ?: return instruction.name to 0
            return if (memoryAddress + addrMode.addressBytes < code.size) {
                val bytes = UByteArray(addrMode.addressBytes) { i -> code[memoryAddress + i + 1] }
                "${instruction.name} ${addrMode.toString(bytes)}" to addrMode.addressBytes
            } else
                "${instruction.name} ???" to addrMode.addressBytes
        }

        fun disassemble(code: UByteArray, memoryAddress: Int): String = disassembleVerbose(code, memoryAddress).first

        fun disassembleFully(binary: UByteArray): Array<String> {
            val sourceCode = arrayListOf<String>()

            var i = 0
            var rawDataCounter = 0
            var instructionCounter = 0
            while(i < binary.size) {
                val (str, additionalSize) = disassembleVerbose(binary, i)
                if(str.startsWith("Data ")) {
                    var label = rawDataCounter.toString()
                    rawDataCounter++
                    while(label.length < 3)
                        label = "0$label"
                    label = "_raw$label"
                    sourceCode.add(".BYTE $label ${str.replaceFirst("Data ", "")}")
                }else {
                    instructionCounter++
                    sourceCode.add(str)
                }

                i += 1 + additionalSize
            }

            fun areInstructionsFollowing(index: Int): Boolean {
                for(i in index + 1 until sourceCode.size)
                    if(!sourceCode[i].startsWith(".BYTE "))
                        return true
                return false
            }

            var problemsFound = false
            for(i in 0 until sourceCode.size) {
                if(sourceCode[i].startsWith(".BYTE ")) {
                    if(areInstructionsFollowing(i)) {
                        sourceCode[i] = "; Invalid byte: " + sourceCode[i].split(' ')[2]
                        problemsFound = true
                    }
                }

                if(sourceCode[i].contains("???")) {
                    sourceCode[i] = ";${sourceCode[i]} ; Incomplete!"
                    problemsFound = true
                }
            }

            if(problemsFound) {
                sourceCode.add(0, ";          WARNING:")
                sourceCode.add(1, "; The disassembled program has")
                sourceCode.add(2, "; some problems!")
                sourceCode.add(3, "; Please see comments for more!")
                sourceCode.add(4, "")
            }

            return sourceCode.toTypedArray()
        }

    }
}