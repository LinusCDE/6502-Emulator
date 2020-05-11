package me.emu6502.emulator

import me.emu6502.kotlinutils.*
import me.emu6502.lib6502.*
import java.io.File
import me.emu6502.kotlinutils.vt100.*
import java.lang.NumberFormatException
import java.lang.StringBuilder
import java.text.NumberFormat
import kotlin.system.exitProcess

class Emulator(val reportError: (String) -> Unit, val updateScreen: (Screen) -> Unit,
               val updateTextScreen: (TextScreen) -> Unit, val updatePia: (PIA) -> Unit,
               val clear: () -> Unit, val write: (String) -> Unit, val writeLine: (String) -> Unit,
               val defineCommand: (name: String, displayName: String, desc: String) -> Unit) {

    companion object {
        enum class MemoryTag(var memoryStart: UShort, var memoryEnd: UShort, vararg val attributes: VT100Attribute, var visible: Boolean = true) {
            INSTRUCTION_OPCODE_PC(0.ushort, 0.ushort, VT100BackgroundColor.CYAN),
            INSTRUCTION_DATA(0.ushort, 0.ushort, VT100BackgroundColor.YELLOW),

            LAST_ASSEMBLY(0.ushort, 0.ushort, VT100Display.DIM, visible = false),

            ZEROPAGE(0x0000.ushort, 0x00FF.ushort, VT100Display.DIM, VT100ForegroundColor.MAGENTA),
            STACK_POINTER(0.ushort, 0.ushort, VT100Display.DIM, VT100ForegroundColor.BLUE, VT100Display.UNDERSCORE),
            STACK(0x0100.ushort, 0x01FF.ushort, VT100Display.DIM, VT100ForegroundColor.BLUE),
            VECTORS(0xFFFA.ushort, 0xFFFF.ushort, VT100ForegroundColor.RED, VT100Display.DIM)
        }

        fun findMemoryTag(memoryAddress: UShort): MemoryTag? =
                MemoryTag.values()
                        .filter { it.visible }
                        .firstOrNull { memoryAddress >= it.memoryStart && memoryAddress <= it.memoryEnd }
    }

    var syncsPerSecond = 5 // Probably best for consoles (changed by ui)
    var runningThread: Thread? = null

    lateinit var cpu: CPU
    lateinit var mainbus: Bus
    lateinit var ram: RAM
    lateinit var rom: ROM
    lateinit var screen: Screen
    lateinit var charrom: ROM
    var textscreen: TextScreen? = null
    lateinit var pia: PIA

    data class MemoryColorSet(var foregroundColor: VT100ForegroundColor?, var backgroundColor: VT100BackgroundColor?,
                              var display: VT100Display?)

    val breakpoints = arrayListOf<UShort>()
    var currentpage: UShort = 0.ushort

    fun reset() {

        if(runningThread != null) {
            try {
                runningThread!!.stop()
                printStatus()
            }catch (e: Exception) { }
        }

        mainbus = Bus()
        ram = RAM(4096.ushort, 0x0000.ushort)
        /*val bbytes: UByteArray = File("textscreen01.bin").readBytes().toUByteArray()
        for (pc in bbytes.indices)
            ram.setData(bbytes[pc], (0x0200 + pc).ushort)*/
        mainbus.devices.add(ram)

        rom = ROM(4096.ushort, 0xF000.ushort)
        val initrom = UByteArray(4096)
        initrom[0x0FFD] = 0x02.ubyte

        for(routine in Routine.values())
            routine.data.forEachIndexed { i, byte -> initrom[routine.memoryAddress.int + i] = byte }

        rom.setMemory(initrom)
        mainbus.devices.add(rom)

        /*charrom = ROM(1024.ushort, 0xEC00.ushort)
        charrom.setMemory(File("apple1.vid").readBytes().toUByteArray())*/

        screen = Screen(160, 120, 0xD000.ushort).apply { reset() }
        mainbus.devices.add(screen)

        if(File("apple1.vid").exists()) {
            textscreen = TextScreen(40, 25, 0xD010.ushort).apply { reset() }
            mainbus.devices.add(textscreen!!)
        }

        cpu = CPU(mainbus).apply { PC = 0x0200.ushort }

        pia = PIA(cpu, 0xD020.ushort)
        mainbus.devices.add(pia)

        updateScreen(screen)
        if(textscreen != null)
            updateTextScreen(textscreen!!)
        updatePia(pia)
    }

    private fun setUShortOrComplain(cmdArgs: List<String>, usage: String = "", callback: ((value: UShort) -> Unit)) {
        var value: UShort
        try {
            value = cmdArgs[0].trim('$').toInt(16).ushort
        }catch (e: Exception) {
            // Number not parsable or argument missing
            reportError("Fehlerhafte Eingabe!${if(usage != "") "\nUsage: $usage" else ""}")
            return
        }
        callback(value)
    }

    private fun setUByteOrComplain(cmdArgs: List<String>, usage: String = "", callback: ((value: UByte) -> Unit)) {
        var value: UByte
        try {
            value = cmdArgs[0].trim('$').toInt(16).ubyte
        }catch (e: Exception) {
            // Number not parsable or argument missing
            reportError("Fehlerhafte Eingabe!${if(usage != "") "\nUsage: $usage" else ""}")
            return
        }
        callback(value)
    }

    fun printStatus(currentPage: UShort = this.currentpage, overwriteOnly: Boolean = false) {
        if(overwriteOnly) {
            // Might now overwrite remaining text from a command.
            // But can be helpful to prevent flicker in rapidly refreshing
            // scenarios. (Used for in-emulation results)
            write(VT100Sequence.CURSOR_HOME.toString())
        }else {
            clear()
        }
        writeLine(cpu.toString())

        //writeLine("${VT100Sequence.SET_ATTRIBUTE_MODE.toString(VT100Display.RESET_ALL_ATTRIBUTES)}")
        MemoryTag.INSTRUCTION_OPCODE_PC.memoryStart = cpu.PC
        MemoryTag.INSTRUCTION_OPCODE_PC.memoryEnd = cpu.PC
        val (_, instructionSize) = Disassembler.disassembleVerbose(ubyteArrayOf(mainbus.getData(cpu.PC), mainbus.getData((cpu.PC + 1.ushort).ushort), mainbus.getData((cpu.PC + 2.ushort).ushort)), 0)
        MemoryTag.INSTRUCTION_DATA.memoryStart = (cpu.PC + 1.uint).ushort
        MemoryTag.INSTRUCTION_DATA.memoryEnd = (cpu.PC.int + 1 + (instructionSize - 1)).ushort
        MemoryTag.STACK_POINTER.memoryStart = (MemoryTag.STACK.memoryStart + cpu.SP - 1.uint).ushort
        MemoryTag.STACK_POINTER.memoryEnd = (MemoryTag.STACK.memoryStart + cpu.SP).ushort

        //ubyteArrayOf(bus.getData(PC), bus.getData((PC + 1.ushort).ushort), bus.getData((PC + 2.ushort).ushort))

        var lastMemoryTag: MemoryTag? = null
        var line: Int = currentpage.toInt()
        while(line < if((currentpage + 0x0400.uint) > 65536.uint) 65536 else currentpage.toInt() + 0x0400) {
            val builder = StringBuilder()
            builder.append(VT100Sequence.SET_ATTRIBUTE_MODE.toString(VT100Display.RESET_ALL_ATTRIBUTES))
            lastMemoryTag = null // To reapply after row name
            builder.append("$" + line.toString("X4") + ": ")
            for (pc in line until (line + 32)) {

                // Colorize memory
                val tag = findMemoryTag(pc.ushort)
                if(tag != lastMemoryTag) {
                    // Remove color from previous space
                    builder.append("${VT100Sequence.SET_ATTRIBUTE_MODE.toString(VT100Display.RESET_ALL_ATTRIBUTES)}\b ")

                    // Reset attributes and apply new ones for memory tag
                    builder.append(VT100Sequence.SET_ATTRIBUTE_MODE.toString(VT100Display.RESET_ALL_ATTRIBUTES, *tag?.attributes ?: arrayOf()))
                    lastMemoryTag = tag
                }

                builder.append("$" + mainbus.getData(pc.ushort).toString("X2") + if(pc < line + 32) " " else "")
            }
            writeLine(builder.toString().substring(0, builder.toString().length - 1))
            line += 32
        }
        writeLine(VT100Sequence.SET_ATTRIBUTE_MODE.toString(VT100Display.RESET_ALL_ATTRIBUTES) ?: "")
    }

    fun executeDebuggerCommand(commandLine: String) {
        val cmdArgs = if(' ' in commandLine) commandLine.split(' ').toMutableList() else arrayListOf(commandLine)
        if(cmdArgs.isEmpty())
            return
        val cmdName = cmdArgs.removeAt(0).toLowerCase()

        when (cmdName)
        {
            "q" -> exitProcess(0)
            "ra" -> reset()
            "rc" -> {
                if(runningThread != null) {
                    try {
                        runningThread!!.stop()
                        printStatus()
                    }catch (e: Exception) { }
                }
                cpu.reset()

            }
            "a" -> setUByteOrComplain(cmdArgs, "a <Byte_in_Hex>") { cpu.A = it }
            "x" -> setUByteOrComplain(cmdArgs, "x <Byte_in_Hex>") { cpu.X = it }
            "y" -> setUByteOrComplain(cmdArgs, "y <Byte_in_Hex>") { cpu.Y = it }
            "sr" -> setUByteOrComplain(cmdArgs, "sr <Byte_in_Hex>") { cpu.SR = it }
            "sp" -> setUByteOrComplain(cmdArgs, "sp <Byte_in_Hex>") { cpu.SP = it }
            "pc" -> setUShortOrComplain(cmdArgs, "pc <Byte_in_Hex>") { cpu.PC = it }
            "d" -> setUShortOrComplain(cmdArgs, "d <Address_in_Hex>") { currentpage = it }
            "m" -> {
                if(cmdArgs.size < 2) {
                    reportError("Fehlerhafte Eingabe!\nUsage: m <Address_in_Hex> <Bytes_in_Hex> ...")
                }else {
                    setUShortOrComplain(cmdArgs, "m <Address_in_Hex> <Bytes_in_Hex> ...") { memoryAddress ->
                        cmdArgs.removeAt(0)
                        val data = cmdArgs.joinToString("")
                                .replace(" ", "")
                                .replace("$", "")
                                .chunked(2)
                                .map { Integer.parseInt(it, 16).ubyte }
                                .toUByteArray()

                        for (mem in data.indices)
                            ram.setData(data[mem], memoryAddress plusSigned mem)
                    }
                }
            }
            "" -> {
                cpu.step()
                mainbus.performClockActions()
                updateScreen(screen)
                if(textscreen != null)
                    updateTextScreen(textscreen!!)
                updatePia(pia)
                //textscreen.screenshot()
            }
            "bl" -> writeLine(breakpoints.map { it.toString("X4") }.joinToString(", "))
            "ba" -> setUShortOrComplain(cmdArgs) { breakpoints.add(it) }
            "br" -> setUShortOrComplain(cmdArgs) { breakpoints.remove(it) }
            "r" -> {
                runningThread = Thread() {
                    val frequency = 1000000 // Cycled per second
                    val cyclesPerSync = frequency / syncsPerSecond
                    val millisPerSync = 1000 / syncsPerSecond
                    val startedAt = System.currentTimeMillis()
                    var syncCount: Long = 0
                    var cycleSum: Long = 0
                    var awaitedCycles: Long = cyclesPerSync.toLong()
                    do {
                        cycleSum += cpu.step()
                        mainbus.performClockActions()
                        //if(cycleSum >= syncCount * cyclesPerSync) {
                        if(cycleSum >= awaitedCycles) {
                            awaitedCycles += cyclesPerSync
                            val sleepTime = millisPerSync - (System.currentTimeMillis() - (startedAt + (syncCount*millisPerSync)))
                            syncCount++
                            updateScreen(screen)
                            if(textscreen != null)
                                updateTextScreen(textscreen!!)
                            updatePia(pia)
                            printStatus(overwriteOnly = (syncCount > 0))

                            if(sleepTime >= 8 || (syncsPerSecond >= 60 && sleepTime > 3))
                                Thread.sleep(sleepTime)
                        }
                    } while (!breakpoints.contains(cpu.PC) && mainbus.getData(cpu.PC) != 0x00.ubyte)
                    updateScreen(screen)
                    if(textscreen != null)
                        updateTextScreen(textscreen!!)
                    updatePia(pia)
                    //textscreen.screenshot()
                    printStatus()
                    val f = NumberFormat.getIntegerInstance()
                    writeLine("Program exited after ${f.format(cycleSum)} cycles (${f.format(System.currentTimeMillis() - startedAt)} ms).")
                }
                runningThread!!.start()
                return
            }
            "as" -> {
                if(cmdArgs.size < 2) {
                    reportError("Fehlerhafte Eingabe. Bitte Adresse und dateinamen angeben.")
                    return
                }

                setUShortOrComplain(cmdArgs) { memAddr ->
                    cmdArgs.removeAt(0)
                    val file = File(cmdArgs.joinToString(" "))
                    if(!file.exists() || file.isDirectory) {
                        reportError("Fehlerhafte Eingabe. Die Datei wurde nicht gefunden.")
                        return@setUShortOrComplain
                    }

                    val (success, message) = assembleToMemory(file.readText(), memAddr.int)
                    if(!success)
                        reportError(message)
                }
            }
            "pia" -> {
                var success = false
                if (cmdArgs.size < 2 || cmdArgs.size > 3) {
                    reportError("Fehlerhafte Eingabe. Usage: pia <get/set/irq> [a/b] [hexvalue]")
                    return
                }

                if (cmdArgs[0] == "get") {
                    if (cmdArgs[1] == "a") {
                        writeLine("Port A value: \$${pia.porta.toString("X2")}");
                        success = true
                    } else if (cmdArgs[1] == "b") {
                        writeLine("Port B value: \$${pia.portb.toString("X2")}");
                        success = true
                    }
                    else
                        success = false
                } else if (cmdArgs[0] == "set") {
                    if (cmdArgs[1] == "a") {
                        try {
                            pia.porta = cmdArgs[2].toInt(16).ubyte
                            success = true
                        }catch (e: NumberFormatException) { }
                    } else if (cmdArgs[1] == "b") {
                        try {
                            pia.portb = cmdArgs[2].toInt(16).ubyte
                            success = true
                        }catch (e: NumberFormatException) { }
                    } else if (cmdArgs[1] == "irq") {
                        pia.irq = !pia.irq
                        success = true
                    } else
                        success = false
                }
                if (success) {
                    updatePia(pia)
                    return
                }
                writeLine("Fehlerhafte Eingabe!")
            }
            else -> reportError("Unbekannter Befehl! Tabulatortaste für eine Befehlsübersicht drücken.")
        }

        printStatus()
    }

    init {
        // Console setup
        //Console.resizeMac(140, 40)
        //Console.SetWindowSize(140, 43)

        defineCommand("q", "q", "Quit")
        defineCommand("ra", "ra", "Reset all")
        defineCommand("rc", "rc", "Reset CPU")
        defineCommand("a", "a", "Set register A")
        defineCommand("x", "x", "Set register X")
        defineCommand("y", "y", "Set register Y")
        defineCommand("sr", "sr", "Set register StatusRegister")
        defineCommand("sp", "sp", "Set register StackPointer")
        defineCommand("pc", "pc", "Set register ProgramCounter")
        defineCommand("d", "d", "Set current page")
        defineCommand("m", "m", "Write custom data to memory address")
        defineCommand("", "<Enter>", "Step. Run single instruction")
        defineCommand("bl", "bl", "List breakpoints")
        defineCommand("ba", "ba", "Add breakpoint")
        defineCommand("br", "br", "Remove breakpoint")
        defineCommand("r", "r", "Run until breakpoint or end")
        defineCommand("as", "as", "Assemble file to memory address")
        defineCommand("pia", "pia", "Get or set Port A/B or trigger q")

        reset()
    }


    var lastMemoryAddress = -1
    var lastProgramSize = 0

    fun assembleToMemory(sourceCode: String, memAddr: Int): Pair<Boolean/*Success*/, String/*Message*/> {
        try {
            //val compiled = Assembler.assemble(sourceCode, memAddr)
            val compiled = Assembler.assemble(sourceCode, memAddr)
            if (lastMemoryAddress != memAddr) {
                lastMemoryAddress = memAddr
            } else {
                for (addr in memAddr until memAddr + lastProgramSize)
                    ram.setData(0x00.ubyte, addr.ushort)
            }

            lastProgramSize = compiled.size
            for (addr in memAddr until memAddr + compiled.size)
                ram.setData(compiled[addr - memAddr], addr.ushort)
            cpu.reset()
            MemoryTag.LAST_ASSEMBLY.visible = true
            MemoryTag.LAST_ASSEMBLY.memoryStart = memAddr.ushort
            MemoryTag.LAST_ASSEMBLY.memoryEnd = (memAddr + compiled.size - 1).ushort
            printStatus()
            return true to "Assembled and written ($lastProgramSize bytes)"
        } catch (e: AssembleException) {
            return false to "Failed: ${e.message}"
        } catch (e: Exception) {
            e.printStackTrace()
            return false to "Unexpected Error!"
        }
    }
}