package me.emu6502.emulator

import me.emu6502.lib6502.Assembler

class Routines {
    companion object {

        val charDspRoutine by lazy {
            Assembler.assemble("""
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
                RTS
            """.trimIndent().replace('€', '$'))
        }

        val pixelDspRoutine by lazy {
            Assembler.assemble("""
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
            """.trimIndent().replace('€', '$'))
        }

        val testRoutine by lazy {
            Assembler.assemble("""
                LDA #€50
                STA €00
                LDA #€3C
                STA €01
                LDA #€FF
                STA €02
                JSR €F000
            """.trimIndent().replace('€', '$'))
        }

    }
}