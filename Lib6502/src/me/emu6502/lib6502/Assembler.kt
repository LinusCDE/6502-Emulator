package me.emu6502.lib6502

import me.emu6502.kotlinutils.*
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class Assembler {

    class AssemblerLine(val instruction: Instruction, var operator: String, var assignedMemoryLabel: String?) {
        fun findAddressMode(): AddressMode? {
            for((opcode, addrMode) in instruction.opCodeAddrModes) {
                if(addrMode.parse(operator) != null)
                    return addrMode
            }
            return null
        }

        fun findOpCode(): Int? {
            for((opcode, addrMode) in instruction.opCodeAddrModes) {
                if(addrMode.parse(operator) != null)
                    return opcode
            }
            return instruction.fixedOpCode
        }

        var relativeAddress: Int? = null

        var referencedAssemblerLine: AssemblerLine? = null

        var resolvedAddressData: UByteArray? = null

        fun isOperatorReferencingMemoryLabel() = instruction.opCodeAddrModes.size > 0 && findOpCode() == null

        fun findAdequateReferenceAddressMode(): AddressMode? {
            for(mode in arrayOf(AddressMode.ABSOLUTE, AddressMode.ZEROPAGE)) {
                if (instruction.opCodeAddrModes.any { (_, addrMode) -> addrMode == mode })
                    return mode
            }
            return null
        }

        fun compile(): UByteArray? {
            val opCode = findOpCode()
            if(opCode == null || resolvedAddressData == null) return null
            return UByteArray(1 + resolvedAddressData!!.size) {i ->
                if(i == 0) opCode.ubyte else resolvedAddressData!![i-1]
            }
        }

        fun expectedSize(): Int? {
            if(resolvedAddressData != null)
                return 1 + resolvedAddressData!!.size
            if(referencedAssemblerLine != null)
                return 1 + (findAdequateReferenceAddressMode()
                        ?: throw AssembleException("Unerwartet: Kann erwartete Größe nicht berechnen für: \"$this\"!")).addressBytes
            return null
        }

        override fun toString(): String = instruction.name + " " + operator
    }

    companion object {
        private val ZEROPAGE_LABEL_JUMP_INSTRUCTIONS = arrayOf(Instruction.BCC, Instruction.BCS, Instruction.BEQ, Instruction.BMI, Instruction.BNE, Instruction.BPL, Instruction.BVC, Instruction.BVS)
        private val ABSOLUTE_LABEL_JUMP_INSTRUCTIONS = arrayOf(Instruction.JMP, Instruction.JSR)

        private infix fun String.hasWord(word: String): Boolean {
            assert(' ' !in word && '\t' !in word)
            return this.contains(Regex("\\b${Pattern.quote(word)}\\b"))
        }

        private fun replaceWord(line: String, word: String, replacementWord: String): String {
            assert(' ' !in word && '\t' !in word)
            assert(' ' !in replacementWord && '\t' !in replacementWord)
            val lineWords = (if(' ' in line) line.split(' ') else listOf(line)).toMutableList()
            for(i in lineWords.indices) {
                if(lineWords[i] == word)
                    lineWords[i] = replacementWord
            }
            return lineWords.joinToString(" ")
        }

        fun assemble(code: String, targetMemoryAddress: Int?): UByteArray {
            val usefulStringLines = code.split("\n")
                    .map { if(";" in it) it.split(";")[0] else it }
                    .map { it.trim() }
                    .filter{ !it.startsWith(";") }
                    .filter{ it.isNotBlank() }
                    .toMutableList()

            // Normalize common whitespace in lines to just a space
            usefulStringLines.replaceAll { line ->
                var cleaned = line
                cleaned = cleaned.replace('\t', ' ')
                while("  " in cleaned)
                    cleaned = cleaned.replace("  ", " ")

                return@replaceAll cleaned
            }

            val variables = hashMapOf<String, String>()

            // Remove and gather variables
            usefulStringLines.removeIf { line ->
                if('=' in line) {
                    val sp = line.split('=')
                    if (sp.size != 2)
                        throw AssembleException("Unbekanntes Mnemonic: $line")
                    variables[sp[0].trim()] = sp[1].trim()
                    true
                } else false
            }

            val lines = arrayListOf<AssemblerLine>()

            // Basic parsing of source code (assigning of instructions and memory labels to them)
            // and finding variables
            var prevMemoryLabel: String? = null
            for(line in usefulStringLines) {
                if(line.endsWith(":")) {
                    prevMemoryLabel = line.substring(0, line.length - 1).trim()
                    continue
                }

                val spacesplit = line.split(' ')
                val mnemonic = spacesplit[0]
                var operator = if(spacesplit.size > 1) spacesplit[1] else ""
                if(operator in variables)
                    operator = variables[operator]!!

                val instruction = Instruction.values().firstOrNull {
                    it.name.equals(mnemonic, true)
                } ?: throw AssembleException("Unbekanntes Mnemonic: $line")

                lines.add(AssemblerLine(instruction, operator, prevMemoryLabel))

                if(prevMemoryLabel != null)
                    prevMemoryLabel = null
            }

            // Find referenced memory labels
            for(line in lines) {
                if(line.isOperatorReferencingMemoryLabel()) {
                    if(line.findAdequateReferenceAddressMode() != null) {
                        line.referencedAssemblerLine = lines.firstOrNull { it.assignedMemoryLabel == line.operator }
                                ?: throw AssembleException("Failed to find memory label ${line.operator}!")
                    }else
                        throw AssembleException("Mnemonic ${line.instruction.name} unterstützt kein Memory Label!")
                }else{
                    if(line.operator != "") {
                        line.resolvedAddressData = line.findAddressMode()?.parse(line.operator) ?:
                                throw AssembleException("Kann Operator nicht verarbeiten: \"$line\"")
                    }else {
                        line.resolvedAddressData = UByteArray(0)
                    }
                }
            }

            // Now at least the size of every command should be calculateable
            var addr = 0
            for(line in lines) {
                line.relativeAddress = addr
                addr += line.expectedSize() ?: throw AssembleException("Unerwartet: Kann Befehlsgröße von \"$line\" nicht ermitteln!")
            }

            // Calculate remaining (labeled) addresses
            for(line in lines) {
                if(line.resolvedAddressData == null) {
                   if(!line.isOperatorReferencingMemoryLabel())
                       throw AssembleException("Unerwartet: Operand-Daten von \"$line\" sollten bereits resolved sein!")

                    val addrMode = line.findAdequateReferenceAddressMode()

                    when(addrMode) {
                        AddressMode.ZEROPAGE -> {
                            if(line.instruction !in ZEROPAGE_LABEL_JUMP_INSTRUCTIONS)
                                throw AssembleException("Keine relativen/zeropage Sprünge für diese Instruction erlaubt: \"$line\"")
                            val offset = line.referencedAssemblerLine!!.relativeAddress!! - (line.relativeAddress!! + line.expectedSize()!!)
                            if(offset < -128 || offset > 127)
                                throw AssembleException("Label \"${line.referencedAssemblerLine?.assignedMemoryLabel ?: "???"}\" ist nicht von $line mit einem signed Byte erreichbar (Offset: $offset)!")
                            val indirectData = ByteArray(1) { offset.toByte() }.toUByteArray()
                            line.operator = AddressMode.ZEROPAGE.toString(indirectData) ?: throw AssembleException("Konnte Adresse für \"$line\" nicht kodieren (Offset: $offset)!")
                            line.resolvedAddressData = line.findAddressMode()!!.parse(line.operator)
                        }
                        AddressMode.ABSOLUTE -> {
                            if(line.instruction !in ABSOLUTE_LABEL_JUMP_INSTRUCTIONS)
                                throw AssembleException("Keine absoluten Sprünge für diese Instruction erlaubt: \"$line\"")
                            if(targetMemoryAddress == null)
                                throw AssembleException("Sprung zu Absoluter Adresse nicht berechenbar, da Speicherort fehlt!")
                            line.operator = "$" + (targetMemoryAddress + line.referencedAssemblerLine!!.relativeAddress!!).toString("X4")
                            line.resolvedAddressData = line.findAddressMode()!!.parse(line.operator)
                        }
                        else -> throw AssembleException("Unerwartet: Sprung-Berechnung zu AddressenTyp $addrMode nicht implementiert!")
                    }

                }
            }

            // Everything should be find for final compilation now
            val out = ByteArrayOutputStream()
            for(line in lines)
                out.write(line.compile()?.toByteArray()
                        ?: throw AssembleException("Unerwartet: \"$line\" ist nicht kompilierbar!"))

            return out.toByteArray().toUByteArray()
        }

    }
}