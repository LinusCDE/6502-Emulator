package me.emu6502.emulator

import me.emu6502.kotlinutils.ushort
import me.emu6502.lib6502.Assembler

enum class Routine(val memoryAddress: UShort, unformattedSourceCode: String) {

    CHAR_DSP(0x001C.ushort, """
                LDA €D007
                CMP #€01
                BNE €F9
                LDA €00
                STA €D004
                LDA €01
                STA €D005
                LDA €02
                STA €D006
                LDA #€02
                STA €D007
                RTS"""),

    PIXEL_DSP(0x0000.ushort, """
                LDA €D003
                CMP #€01
                BNE €F9
                LDA €00
                STA €D000
                LDA €01
                STA €D001
                LDA €02
                STA €D002
                LDA #€02
                STA €D003
                RTS
            """);

    val sourceCode = unformattedSourceCode.trimIndent().replace("€", "$")
    val data by lazy { Assembler.assemble(sourceCode) }

}