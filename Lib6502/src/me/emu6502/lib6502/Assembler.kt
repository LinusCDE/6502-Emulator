package me.emu6502.lib6502

import me.emu6502.kotlinutils.*
import java.io.ByteArrayOutputStream
import java.lang.Exception

class Assembler {

    open class AssemblerLine(val instruction: Instruction, var operator: String, var assignedMemoryLabel: String?) {
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

        open fun isOperatorReferencingMemoryLabel() = instruction.opCodeAddrModes.size > 0 && findOpCode() == null

        open fun findAdequateReferenceAddressMode(): AddressMode? {
            if(operator.endsWith(",X", true) && instruction.isAddressModeSupported(AddressMode.ABSOLUTE_X))
                return AddressMode.ABSOLUTE_X
            if(operator.endsWith(",Y", true) && instruction.isAddressModeSupported(AddressMode.ABSOLUTE_Y))
                return AddressMode.ABSOLUTE_Y

            // Only allow ABSOLUTE for referencing directive storage
            if(referencedAssemblerLine != null && referencedAssemblerLine is AssemblerDirectiveLine) {
                if(instruction.isAddressModeSupported(AddressMode.ABSOLUTE))
                    return AddressMode.ABSOLUTE
                else
                    null
            }

            for(mode in arrayOf(AddressMode.ABSOLUTE, AddressMode.ZEROPAGE)) {
                if (instruction.opCodeAddrModes.any { (_, addrMode) -> addrMode == mode })
                    return mode
            }
            return null
        }

        open fun compile(): UByteArray? {
            val opCode = findOpCode()
            if(opCode == null || resolvedAddressData == null) return null
            return UByteArray(1 + resolvedAddressData!!.size) {i ->
                if(i == 0) opCode.ubyte else resolvedAddressData!![i-1]
            }
        }

        open fun expectedSize(): Int? {
            if(resolvedAddressData != null)
                return 1 + resolvedAddressData!!.size
            if(referencedAssemblerLine != null)
                return 1 + (findAdequateReferenceAddressMode()
                        ?: throw AssembleException("Unerwartet: Kann erwartete Größe nicht berechnen für: \"$this\"!")).addressBytes
            return null
        }

        override fun toString(): String = instruction.name + " " + operator
    }

    class AssemblerDirectiveLine(val name: String, val content: UByteArray): AssemblerLine(Instruction.NOP/*DUMMY!*/, "", name) {
        init {
            resolvedAddressData = UByteArray(0)
        }

        override fun isOperatorReferencingMemoryLabel(): Boolean = false
        override fun compile(): UByteArray? = content
        override fun expectedSize(): Int? = content.size

        override fun toString(): String = "Directive with ${content.size} bytes of data"
    }

    companion object {
        private val ZEROPAGE_LABEL_JUMP_INSTRUCTIONS = arrayOf(Instruction.BCC, Instruction.BCS, Instruction.BEQ, Instruction.BMI, Instruction.BNE, Instruction.BPL, Instruction.BVC, Instruction.BVS)
        private val ABSOLUTE_LABEL_JUMP_INSTRUCTIONS = arrayOf(Instruction.JMP, Instruction.JSR)

        fun assemble(code: String, defaultTargetMemoryAddress: Int?): UByteArray {
            var defaultTargetMemoryAddress = defaultTargetMemoryAddress // Make changeable (for .ORG)

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

            // Parse variables (and remove those lines)
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
            val directives = arrayListOf<AssemblerDirectiveLine>() // To be added to the end of lines later

            // Parse directives (and remove those lines)
            usefulStringLines.removeIf { line ->
                if(line.startsWith('.')) {
                    // Is directive
                    if(' ' !in line)
                        throw AssembleException("Ungültige Directive: \"$line\"")
                    val sp = line.split(' ')
                    if(sp.size == 3) {
                        val name = sp[1]
                        val data = sp[2]
                        if (line.startsWith(".BYTE", true)) {
                            val values = if (',' in data) data.split(',') else listOf(data)
                            try {
                                val bytes = values
                                        .map { it.trim('$') }
                                        .map { it.toUByte(16) }
                                        .toUByteArray()
                                directives.add(AssemblerDirectiveLine(name, bytes))
                                return@removeIf true
                            } catch (e: Exception) {
                                throw AssembleException("Fehler beim parsen der byte(s) in: \"$line\"")
                            }
                        } else if (line.startsWith(".STRING", true)) {
                            try {
                                val bytes = (data.trim('"').toByteArray(Charsets.US_ASCII) + listOf(0x00.toByte())).toUByteArray()
                                directives.add(AssemblerDirectiveLine(name, bytes))
                                return@removeIf true
                            } catch (e: Exception) {
                                throw AssembleException("Fehler beim parsen der byte(s) in: \"$line\"")
                            }
                        }else {
                            throw AssembleException("Unbekannte Directive oder parameterzahl falsch: \"$line\"")
                        }
                    }else if(sp.size == 2) {
                        val data = sp[1]
                        if(line.startsWith(".ORG", true)) {
                            try {
                                defaultTargetMemoryAddress = data.trim('$').toUShort(16).int
                                return@removeIf true
                            }catch (e: Exception) {
                                throw AssembleException("Fehlerhafte Ursprungs-Adresse zu .ORG: \"$line\"")
                            }
                        }else
                            throw AssembleException("Unbekannte Directive oder parameterzahl falsch: \"$line\"")
                    }else
                        throw AssembleException("Unbekannte Directive oder parameterzahl falsch: \"$line\"")
                }
                false
            }

            val targetMemoryAddress = defaultTargetMemoryAddress // Make overrideable (by .ORG directive)


            // Basic parsing of remaining source lines (assigning of instructions and memory labels to them)
            // and applying variables
            var prevMemoryLabel: String? = null
            for(line in usefulStringLines) {
                if(line.endsWith(":")) {
                    prevMemoryLabel = line.substring(0, line.length - 1).trim()
                    continue
                }

                val spacesplit = line.split(' ')
                val mnemonic = spacesplit[0]
                var operator = if(spacesplit.size > 1) spacesplit[1] else ""
                for((varName, varValue) in variables)
                    operator = operator.replace(varName, varValue)

                val instruction = Instruction.values().firstOrNull {
                    it.name.equals(mnemonic, true)
                } ?: throw AssembleException("Unbekanntes Mnemonic: $line")

                lines.add(AssemblerLine(instruction, operator, prevMemoryLabel))

                if(prevMemoryLabel != null)
                    prevMemoryLabel = null
            }

            // Add directives after lines (directives is not used anymore!)
            lines.addAll(directives)

            // Find referenced memory labels
            for(line in lines) {
                if(line.isOperatorReferencingMemoryLabel()) {
                    if(line.findAdequateReferenceAddressMode() != null) {
                        var labelName = line.operator
                        if (line.findAdequateReferenceAddressMode() in arrayOf(AddressMode.ABSOLUTE_X, AddressMode.ABSOLUTE_Y))
                            labelName = line.operator.split(",")[0]

                        line.referencedAssemblerLine = lines.firstOrNull { it.assignedMemoryLabel == labelName }
                                ?: throw AssembleException("Failed to find memory label $labelName!")
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
                        in arrayOf(AddressMode.ABSOLUTE, AddressMode.ABSOLUTE_X, AddressMode.ABSOLUTE_Y) -> {
                            if(line.instruction !in ABSOLUTE_LABEL_JUMP_INSTRUCTIONS && line.referencedAssemblerLine !is AssemblerDirectiveLine)
                                throw AssembleException("Keine absoluten Sprünge für diese Instruction erlaubt: \"$line\"")
                            if(targetMemoryAddress == null)
                                throw AssembleException("Sprung zu Absoluter Adresse nicht berechenbar, da Speicherort fehlt!")
                            val suffix = if(addrMode in arrayOf(AddressMode.ABSOLUTE_X, AddressMode.ABSOLUTE_Y)) "," + line.operator.split(',')[1] else ""
                            line.operator = "$" + (targetMemoryAddress + line.referencedAssemblerLine!!.relativeAddress!!).toString("X4") + suffix
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