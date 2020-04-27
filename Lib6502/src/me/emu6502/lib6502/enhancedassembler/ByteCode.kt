package me.emu6502.lib6502.enhancedassembler

import me.emu6502.kotlinutils.*
import me.emu6502.lib6502.AssembleException
import me.emu6502.lib6502.Disassembler
import java.lang.Exception
import java.lang.IllegalArgumentException
import kotlin.collections.toUByteArray

data class ByteCode(var code: UByteArray, var label: String, var position: UShort) {

    override fun toString(): String {
        var returnstring = "${position.toString("X4")}: ";

        code.forEach { returnstring += "${it.toString("X2")} " }
        returnstring += Disassembler.disassemble(code, 0)

        return returnstring
    }

    companion object {
        fun parseData(line: String): ByteCode {
            val type = line.trim().split(' ', limit = 3)[0].trim('.')
            val label = line.trim().split(' ', limit = 3)[1].trim()
            val data = line.trim().split(' ', limit = 3)[2].trim()

            try {

                return when (type.toUpperCase()) {
                    "BYTE" -> {
                        val values = if (',' in data) data.split(',') else listOf(data)
                        val bytes = values
                                .map { it.trim('$') }
                                .map { it.toInt(16) }
                                .map { it.ubyte }
                                .toUByteArray()
                        ByteCode(bytes, label, 0x0000.ushort)
                    }
                    "STRING" -> {
                        val bytes = (data.trim('"').toByteArray(Charsets.US_ASCII) + listOf(0x00.toByte())).toUByteArray()
                        ByteCode(bytes, label, 0x000.ushort)
                    }
                    else -> throw IllegalArgumentException() // Just to trigger the try-catch clause
                }

            } catch (e: Exception) {
                throw AssembleException("Ungültige Daten: $line")
            }
        }

    }

}