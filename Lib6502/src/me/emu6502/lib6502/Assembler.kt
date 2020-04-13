package me.emu6502.lib6502

import ubyte
import ushort
import java.lang.NumberFormatException

class Assembler {
    companion object {

        /**
         * Checks if a given string contains an absolute address
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkAbs(opstring: String, onUpdateAddress: (newAddres: UShort) -> Unit): Boolean {
            if ((opstring.length != 5) || !opstring.startsWith("$") || opstring.contains(',')) {
                onUpdateAddress(0.ushort)
                return false
            }
            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1), 16).ushort)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an absolute address indexed by X
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkAbsX(opstring: String, onUpdateAddress: (newAddres: UShort) -> Unit): Boolean {
            if ((opstring.length != 7) || !opstring.startsWith("$") || !opstring.contains(",X", true)) {
                onUpdateAddress(0.ushort)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1, 4), 16).ushort)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an absolute address indexed by Y
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkAbsY(opstring: String, onUpdateAddress: (newAddres: UShort) -> Unit): Boolean {
            if ((opstring.length != 7) || !opstring.startsWith("$") || !opstring.contains(",Y", true))
            {
                onUpdateAddress(0.ushort)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1, 4), 16).ushort)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an accumulator operand
         * @param opstring string containing operand
         * @return true if check is successful, false if not
         */
        private fun checkAccu(opstring: String): Boolean = opstring.equals("A", true)

        /**
         * Checks if a given string contains an immediate value
         * @param opstring string containing operand
         * @param onUpdateValue the lambda that sets the value from the string
         * @return true if check is successful, false if not
         */
        private fun checkImm(opstring: String, onUpdateValue: (newValue: UByte) -> Unit): Boolean {
            if ((opstring.length != 4) || !opstring.startsWith("#$") || opstring.contains(',')) {
                onUpdateValue(0.ubyte)
                return false
            }

            try {
                onUpdateValue(Integer.parseUnsignedInt(opstring.substring(2), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an indirect address
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkInd(opstring: String, onUpdateAddress: (newAddres: UShort) -> Unit): Boolean {
            if ((opstring.length != 7) || !opstring.startsWith("($") || opstring.contains(',') || opstring.endsWith(")")) {
                onUpdateAddress(0.ushort)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(2, 4), 16).ushort)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an indirect address indexed by X
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkIndX(opstring: String, onUpdateAddress: (newAddres: UByte) -> Unit): Boolean {
            if ((opstring.length != 7) || !opstring.startsWith("($") || !opstring.endsWith(",X)", true)) {
                onUpdateAddress(0.ubyte)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(2, 2), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains an indirect address indexed by Y
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkIndY(opstring: String, onUpdateAddress: (newAddres: UByte) -> Unit): Boolean {
            if ((opstring.length != 7) || !opstring.startsWith("($") || !opstring.endsWith("),Y", true)) {
                onUpdateAddress(0.ubyte)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(2, 2), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains a zeropage address
         * @param opstring string containing operand
         * @param onUpdateAddress the lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkZP(opstring: String, onUpdateAddress: (newAddres: UByte) -> Unit): Boolean {
            if ((opstring.length != 3) || !opstring.startsWith("$") || opstring.contains(',')) {
                onUpdateAddress(0.ubyte)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains a zeropage address indexed by X
         * @param opstring
         * @param onUpdateAddress lambda that sets the address from the string
         * @return true if check is successful, false if not
         */
        private fun checkZPX(opstring: String, onUpdateAddress: (newAddres: UByte) -> Unit): Boolean {
            if ((opstring.length != 5) || !opstring.startsWith("$") || !opstring.endsWith(",X", true)) {
                onUpdateAddress(0.ubyte)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1, 2), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Checks if a given string contains a zeropage address indexed by Y
         * @param opstring string containing operand
         * @param onUpdateAddress lambda to set the address
         * @return true if check is successful, false if not
         */
        private fun checkZPY(opstring: String, onUpdateAddress: (newAddres: UByte) -> Unit): Boolean {
            if ((opstring.length != 5) || !opstring.startsWith("$") || !opstring.endsWith(",Y", true)) {
                onUpdateAddress(0.ubyte)
                return false
            }

            try {
                onUpdateAddress(Integer.parseUnsignedInt(opstring.substring(1, 2), 16).ubyte)
                return true
            }catch (e: NumberFormatException) {
                return false
            }
        }

        /**
         * Converts a code listing into a byte array
         * @param code listing
         * @return byte array with converted code
         */
        fun assemble(code: String): UByteArray {
            val lines = code.split("\n")
                    .map { it.trim() }
                    .filter{ !it.startsWith(";") }
                    .filter{ it.isNotBlank() }

            val bytelist = arrayListOf<UByte>()
            for(line in lines) {
                val spacesplit = line.split(' ')
                val opcode = spacesplit[0]
                var addr: UShort = 0.ushort
                var op1: UByte = 0.ubyte
                when (opcode.toUpperCase()) {
                    "ADC" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x69.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x65.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x75.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x6D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x7D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0x79.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x61.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x71.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "AND" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x29.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x25.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x35.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x2D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x3D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0x39.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x21.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x31.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "ASL" -> {
                        if (checkAccu(spacesplit[1])) {
                            bytelist.add(0x0A.ubyte)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x06.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x16.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x0E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x1E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "BCC" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }

                        bytelist.add(0x90.ubyte)
                        bytelist.add(op1)
                    }
                    "BCS" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0xB0.ubyte)
                        bytelist.add(op1)
                    }
                    "BEQ" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0xF0.ubyte)
                        bytelist.add(op1)
                    }
                    "BIT" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x24.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x2C.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "BMI" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand" + line)
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0x30.ubyte)
                        bytelist.add(op1)
                    }
                    "BNE" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0xD0.ubyte)
                        bytelist.add(op1)
                    }
                    "BPL" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0x10.ubyte)
                        bytelist.add(op1)
                    }
                    "BRK" -> bytelist.add(0x00.ubyte)
                    "BVC" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0x50.ubyte)
                        bytelist.add(op1)
                    }
                    "BVS" -> {
                        if (spacesplit[1].length > 3 || !spacesplit[1].startsWith("$"))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        try {
                            op1 = Integer.parseUnsignedInt(spacesplit[1].substring(1), 16).ubyte
                        }catch(e: NumberFormatException) {
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        }
                        bytelist.add(0x70.ubyte)
                        bytelist.add(op1)
                    }
                    "CLC" -> bytelist.add(0x18.ubyte)
                    "CLD" -> bytelist.add(0xD8.ubyte)
                    "CLI" -> bytelist.add(0x58.ubyte)
                    "CLV" -> bytelist.add(0xB8.ubyte)
                    "CMP" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC9.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC5.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xD5.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xCD.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xDD.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0xD9.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC1.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xD1.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "CPX" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE0.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE4.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xEC.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "CPY" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC0.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC4.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xCC.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "DEC" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xC6.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xD6.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xCE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xDE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "DEX" -> bytelist.add(0xCA.ubyte)
                    "DEY" -> bytelist.add(0x88.ubyte)
                    "EOR" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x49.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x45.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x55.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x4D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x5D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0x59.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x41.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x51.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "INC" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE6.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xF6.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xEE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xFE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "INX" -> bytelist.add(0xE8.ubyte)
                    "INY" -> bytelist.add(0xC8.ubyte)
                    "JMP" -> {
                        if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x4C.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkInd(spacesplit[1], { addr = it })) {
                            bytelist.add(0x6C.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "JSR" -> {
                        if (!checkAbs(spacesplit[1], { addr = it }))
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                        bytelist.add(0x20.ubyte)
                        bytelist.add(BitConverter.GetBytes(addr)[0])
                        bytelist.add(BitConverter.GetBytes(addr)[1])
                    }
                    "LDA" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA9.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA5.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xB5.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xAD.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xBD.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0xB9.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA1.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xB1.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "LDX" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA2.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA6.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xB6.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xAE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0xBE.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "LDY" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA0.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xA4.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xB4.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xAC.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xBC.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "LSR" -> {
                        if (checkAccu(spacesplit[1])) {
                            bytelist.add(0x4A.ubyte)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x46.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x56.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x4E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x5E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "NOP" -> bytelist.add(0xEA.ubyte)
                    "ORA" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x09.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x05.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x15.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x0D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x1D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0x19.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x01.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x11.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "PHA" -> bytelist.add(0x48.ubyte)
                    "PHP" -> bytelist.add(0x08.ubyte)
                    "PLA" -> bytelist.add(0x68.ubyte)
                    "PLP" -> bytelist.add(0x28.ubyte)
                    "ROL" -> {
                        if (checkAccu(spacesplit[1])) {
                            bytelist.add(0x2A.ubyte)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x26.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x36.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x2E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x3E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "ROR" -> {
                        if (checkAccu(spacesplit[1])) {
                            bytelist.add(0x6A.ubyte)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x66.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x76.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x6E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x7E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "RTI" -> bytelist.add(0x40.ubyte)
                    "RTS" -> bytelist.add(0x60.ubyte)
                    "SBC" -> {
                        if (checkImm(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE9.ubyte)
                            bytelist.add(op1)
                        } else if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE5.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xF5.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0xED.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0xFD.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0xF9.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xE1.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0xF1.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "SEC" -> bytelist.add(0x38.ubyte)
                    "SED" -> bytelist.add(0xF8.ubyte)
                    "SEI" -> bytelist.add(0x78.ubyte)
                    "STA" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x85.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x95.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x8D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsX(spacesplit[1], { addr = it })) {
                            bytelist.add(0x9D.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkAbsY(spacesplit[1], { addr = it })) {
                            bytelist.add(0x99.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else if (checkIndX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x81.ubyte)
                            bytelist.add(op1)
                        } else if (checkIndY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x91.ubyte)
                            bytelist.add(op1)
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "STX" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x86.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPY(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x96.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x8E.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "STY" -> {
                        if (checkZP(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x84.ubyte)
                            bytelist.add(op1)
                        } else if (checkZPX(spacesplit[1], { op1 = it })) {
                            bytelist.add(0x94.ubyte)
                            bytelist.add(op1)
                        } else if (checkAbs(spacesplit[1], { addr = it })) {
                            bytelist.add(0x8C.ubyte)
                            bytelist.add(BitConverter.GetBytes(addr)[0])
                            bytelist.add(BitConverter.GetBytes(addr)[1])
                        } else
                            throw IllegalArgumentException ("Ungültiger Operand $line")
                    }
                    "TAX" -> bytelist.add(0xAA.ubyte)
                    "TAY" -> bytelist.add(0xA8.ubyte)
                    "TSX" -> bytelist.add(0xBA.ubyte)
                    "TXA" -> bytelist.add(0x8A.ubyte)
                    "TXS" -> bytelist.add(0x9A.ubyte)
                    "TYA" -> bytelist.add(0x98.ubyte)
                    else ->
                        throw IllegalArgumentException("Ungültiger Opcode " + line)
                }
            }
            return bytelist.toUByteArray()
        }

    }
}