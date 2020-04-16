package me.emu6502.emulator

import me.emu6502.lib6502.Bus
import me.emu6502.lib6502.CPU
import me.emu6502.lib6502.RAM
import me.emu6502.lib6502.ROM
import me.emu6502.kotlinutils.*
import java.lang.Exception
import kotlin.system.exitProcess

class Emulator(val reportError: (String) -> Unit, val updateScreen: (Screen) -> Unit,
               val clear: () -> Unit, val write: (String) -> Unit, val writeLine: (String) -> Unit,
               val defineCommand: (name: String, displayName: String, desc: String) -> Unit) {

    lateinit var cpu: CPU
    lateinit var mainbus: Bus
    lateinit var ram: RAM
    lateinit var rom: ROM
    lateinit var screen: Screen
    lateinit var charrom: ROM
    lateinit var textscreen: TextScreen

    val breakpoints = arrayListOf<UShort>()
    var currentpage: UShort = 0.ushort

    fun reset() {

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

        /*textscreen = TextScreen(40, 25, 0xD004.ushort).apply { reset() }
        mainbus.devices.add(textscreen)*/

        cpu = CPU(mainbus).apply { PC = 0x0200.ushort }

        updateScreen(screen)
    }

    private fun setUShortOrComplain(cmdArgs: List<String>, usage: String = "", callback: ((value: UShort) -> Unit)) {
        var value: UShort
        try {
            value = cmdArgs[0].toInt(16).ushort
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
            value = cmdArgs[0].toInt(16).ubyte
        }catch (e: Exception) {
            // Number not parsable or argument missing
            reportError("Fehlerhafte Eingabe!${if(usage != "") "\nUsage: $usage" else ""}")
            return
        }
        callback(value)
    }

    fun printStatus(currentPage: UShort = this.currentpage) {
        clear()
        writeLine(cpu.toString())
        var line: Int = currentpage.toInt()
        while(line < if((currentpage + 0x0400.uint) > 65536.uint) 65536 else currentpage.toInt() + 0x0400) {
            write("$" + line.toString("X4") + ":")
            for (pc in line until (line + 32))
                write(" $" + mainbus.getData(pc.ushort).toString("X2"))
            writeLine("")
            line += 32
        }
        writeLine("")
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
            "rc" -> cpu.reset()
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
                //textscreen.screenshot()
            }
            "bl" -> writeLine(breakpoints.map { it.toString("X4") }.joinToString(", "))
            "ba" -> setUShortOrComplain(cmdArgs) { breakpoints.add(it) }
            "br" -> setUShortOrComplain(cmdArgs) { breakpoints.remove(it) }
            "r" -> {
                do {
                    cpu.step()
                    mainbus.performClockActions()
                } while (!breakpoints.contains(cpu.PC) && mainbus.getData(cpu.PC) != 0x00.ubyte)
                updateScreen(screen)
                //textscreen.screenshot()
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

        reset()
    }
}