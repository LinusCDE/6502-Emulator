package me.emu6502.lib6502

import java.lang.RuntimeException

sealed class Lib6502Exception(message: String): RuntimeException(message)
class EmulationException(message: String): Lib6502Exception(message)
class AssembleException(message: String): Lib6502Exception(message)