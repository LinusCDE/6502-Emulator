package me.emu6502.lib6502.enhancedassembler

import me.emu6502.kotlinutils.*
import me.emu6502.lib6502.AddressMode
import me.emu6502.lib6502.AssembleException
import me.emu6502.lib6502.Assembler
import me.emu6502.lib6502.Instruction
import me.emu6502.lib6502.Instruction.*
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class EnhancedAssembler {

    companion object {
        private val BRANCH_INSTRUCTIONS = arrayOf(BCC, BCS, BEQ, BMI, BNE, BPL, BVC, BVS)
        private val JUMP_INSTRUCTIONS = arrayOf(JMP, JSR)

        fun assemble(code: String): UByteArray {
            // Get codelines
            val lines = (if ('\n' in code) code.split('\n') else listOf(code))
                    .map { CodeLine(it) }
                    .filter { it.type != CodeLine.Companion.LineType.COMMENT } // Remove comment lines

            // Remove in-line comments
            for (line in lines)
                line.line = line.cleaned()

            // Get line index for each memory label
            val labelIndexes = lines
                    .filter { it isType CodeLine.Companion.LineType.LABEL }
                    .map { it.line.substring(0, it.line.length - 1) to -1 }
                    .toMap().toMutableMap()

            val dataValues = lines
                    .filter { it isType CodeLine.Companion.LineType.DIRECTIVE }
                    .filter { ! it.line.contains("ORG", true) }
                    .map { ByteCode.parseData(it.line) }
                    .toList()

            val variables = lines
                    .filter { it isType CodeLine.Companion.LineType.VARIABLE }
                    .map { it.line.split("=") }
                    .map { it[0].trim() to it[1].trim() }
                    .map { (key, value) -> key to value.replace("$", "$$") }
                    .toMap()

            for (line in lines.filter { it isType CodeLine.Companion.LineType.CODE })
                variables
                        .filter { line.line.matches(Regex("\\b${Pattern.quote(it.key)}\\b")) }
                        .forEach {
                            line.line.replace(Regex("\\b${Pattern.quote(it.key)}\\b"), it.key)
                        }

            val byteCodes = arrayListOf<ByteCode>()

            var pc: UShort = 0x0000.ushort

            val origin = lines
                    .filter { it isType CodeLine.Companion.LineType.DIRECTIVE }
                    .firstOrNull { it.line.contains(".ORG", true) }
                    ?.runCatching {
                        pc = this.line.split(' ')[1].trim('$').toInt(16).ushort
                    }
            //println("Origin set to $${pc.toString("X4")}")

            for (codeline in lines) {
                if (codeline isType CodeLine.Companion.LineType.LABEL) {
                    labelIndexes[codeline.line.substring(0, codeline.line.length - 1)] = pc.int
                    continue
                }

                if (!(codeline isType CodeLine.Companion.LineType.CODE))
                    continue

                val line = codeline.line
                var containsLabel = labelIndexes.keys.firstOrNull { line.matches(Regex("\\b$it\\b")) } ?: ""
                var containsDataLabel = dataValues.firstOrNull { line.matches(Regex("\\b${it.label}\\b")) }?.label ?: ""

                val bc = if (containsLabel.isNotEmpty()) {
                    if (labelIndexes[containsLabel] != -1) {
                        if (line.startsWith('J', true)) {
                            ByteCode(
                                    code = Assembler.assemble(line.replace(Regex("\\b$containsLabel\\b"), "$$0000"), null),
                                    label = containsLabel,
                                    position = pc
                            )
                        }else if(line.startsWith('B', true)) {
                            ByteCode(
                                    code = Assembler.assemble(line.replace(Regex("\\b$containsDataLabel\\b"), "$$0000"), null),
                                    label = containsLabel,
                                    position = pc
                            )
                        } else {
                            throw AssembleException("Label at illegal position ($line)")
                        }
                    } else {
                        if(line.startsWith('J', true)) {
                            ByteCode(
                                    code = Assembler.assemble(line.replace(Regex("\\b$containsLabel\\b"), "$$${labelIndexes[containsLabel]?.toString("X4")}"), null),
                                    label = "",
                                    position = pc
                            )
                        }else if(line.startsWith('B', true)) {
                            ByteCode(
                                    code = Assembler.assemble(line.replace(Regex("\\b$containsLabel\\b"),
                                            "$$" + (if(labelIndexes[containsLabel]!! >= pc.int) (labelIndexes[containsLabel]!! - pc.int - 2).ushort else (0xFE - (pc.int - labelIndexes[containsLabel]!!)).ushort).toString("X2")
                                    ), null),
                                    label = "",
                                    position = pc
                            )
                        }else {
                            throw AssembleException("Label at illegal position ($line)")
                        }
                    }
                }else if(containsDataLabel.isNotEmpty()) {
                    ByteCode(
                            code = Assembler.assemble(line.replace(Regex("\\b$containsDataLabel\\b"), "$$0000"), null),
                            label = containsDataLabel,
                            position = pc
                    )
                }else {
                    ByteCode(
                            code = Assembler.assemble(line, null),
                            label = "",
                            position = pc
                    )
                }

                pc = pc plusSigned bc.code.size
                byteCodes.add(bc)
            }

            for(data in dataValues) {
                for(bc in byteCodes.filter { it.label == data.label }) {
                    bc.code[1] = BitConverter.GetBytes(pc)[0];
                    bc.code[2] = BitConverter.GetBytes(pc)[1];
                    //println("Data ${data.label} is used at ${pc.toString("X4")}")
                }

                data.position = pc
                data.label = ""
                byteCodes.add(data)
                pc = pc plusSigned data.code.size
            }

            for(bc in byteCodes.filter { it.label != "" }) {
                val instr = Instruction.find(bc.code[0].int)
                val addrMode = instr?.findAddressMode(bc.code[0].int)

                if(instr in BRANCH_INSTRUCTIONS && addrMode == AddressMode.ZEROPAGE) {
                    bc.code[1] = if(labelIndexes[bc.label]!! >= bc.position.int)
                        (labelIndexes[bc.label]!! - bc.position.int - 2).ubyte
                    else
                        (0xFE - (bc.position.int - labelIndexes[bc.label]!!)).ubyte

                } else if(instr in JUMP_INSTRUCTIONS && (addrMode == AddressMode.ABSOLUTE || addrMode == AddressMode.INDIRECT)) {
                    bc.code[1] = BitConverter.GetBytes(labelIndexes[bc.label]!!.ushort)[0]
                    bc.code[2] = BitConverter.GetBytes(labelIndexes[bc.label]!!.ushort)[1]
                }

            }

            val bOut = ByteArrayOutputStream()
            for(bc in byteCodes)
                bOut.write(bc.code.toByteArray())

            return bOut.toByteArray().toUByteArray()
        }
    }

}