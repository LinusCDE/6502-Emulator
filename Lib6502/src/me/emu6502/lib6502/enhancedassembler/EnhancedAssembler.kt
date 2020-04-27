package me.emu6502.lib6502.enhancedassembler

import me.emu6502.kotlinutils.int
import me.emu6502.kotlinutils.plusSigned
import me.emu6502.kotlinutils.toString
import me.emu6502.kotlinutils.ushort
import me.emu6502.lib6502.AssembleException
import me.emu6502.lib6502.Assembler
import java.util.regex.Pattern

class EnhancedAssembler {

    companion object {
        fun assemble(code: String) {
            // Get codelines
            val lines = (if ('\n' in code) code.split('\n') else listOf(code))
                    .map { CodeLine(it) }
                    .filter { it isType CodeLine.Companion.LineType.COMMENT } // Remove comment lines

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
                    .filter { it.line.contains("ORG", true) }
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
            println("Origin set to $${pc.toString("X4")}")

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

            // Weiter hier
            // https://github.com/bomberman2910/6502Tests/blob/26a5cfdbd4e11cbce7f88a528827f88469149999/asm6502/Program.cs#L182

        }
    }

}