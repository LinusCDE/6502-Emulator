package me.emu6502.emulator

import me.emu6502.lib6502.Bus
import me.emu6502.lib6502.CPU
import me.emu6502.lib6502.RAM
import me.emu6502.lib6502.ROM
import plusSigned
import ubyte
import ushort
import toString
import uint
import java.lang.Exception
import kotlin.system.exitProcess

class Emulator(val requestCommand: () -> String, val requestRawInput: () -> String, val reportError: (String) -> Unit,
               val clear: () -> Unit, val write: (String) -> Unit, val writeLine: (String) -> Unit,
               val updateScreen: (Screen) -> Unit, val defineCommand: (name: String, displayName: String, desc: String) -> Unit) {

    lateinit var cpu: CPU
    lateinit var mainbus: Bus
    lateinit var ram: RAM
    lateinit var rom: ROM
    lateinit var screen: Screen
    lateinit var charrom: ROM
    lateinit var textscreen: TextScreen

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
        Routines.pixelDspRoutine.forEachIndexed { i, byte -> initrom[0x0000 + i] = byte }
        Routines.charDspRoutine.forEachIndexed { i, byte -> initrom[0x001C + i] = byte }

        Routines.testRoutine.forEachIndexed { i, byte -> ram.memory[0x0200 + i] = byte }

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

    private fun setUShortOrComplain(cmdArgs: List<String>, callback: ((value: UShort) -> Unit)) {
        try {
            callback(cmdArgs[0].toInt(16).ushort)
        }catch (e: Exception) {
            reportError("Fehlerhafte Eingabe!") // Number not parsable or argument missing
        }
    }

    private fun setUByteOrComplain(cmdArgs: List<String>, callback: ((value: UByte) -> Unit)) {
        try {
            callback(cmdArgs[0].toInt(16).ubyte)
        }catch (e: Exception) {
            reportError("Fehlerhafte Eingabe!") // Number not parsable or argument missing
        }
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
        defineCommand("y", "y", "Set register X")
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

        var currentpage: UShort = 0x0000.ushort
        val breakpoints = arrayListOf<UShort>()

        while (true)
        {
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
            val inputLine = requestCommand().trim()
            val cmdArgs = if(' ' in inputLine) inputLine.split(' ').toMutableList() else arrayListOf(inputLine)
            if(cmdArgs.isEmpty())
                continue
            val cmdName = cmdArgs.removeAt(0).toLowerCase()

            when (cmdName)
            {
                "q" -> exitProcess(0)
                "ra" -> reset()
                "rc" -> cpu.reset()
                "a" -> setUByteOrComplain(cmdArgs) { cpu.A = it }
                "x" -> setUByteOrComplain(cmdArgs) { cpu.X = it }
                "y" -> setUByteOrComplain(cmdArgs) { cpu.Y = it }
                "sr" -> setUByteOrComplain(cmdArgs) { cpu.SR = it }
                "sp" -> setUByteOrComplain(cmdArgs) { cpu.SP = it }
                "pc" -> setUShortOrComplain(cmdArgs) { cpu.PC = it }
                "d" -> setUShortOrComplain(cmdArgs) { currentpage = it }
                "m" -> {
                    setUShortOrComplain(cmdArgs) { memoryAddress ->

                        val line = requestRawInput()

                        val data: UByteArray
                        try {
                            data = line.split(' ').map { it.toInt().ubyte }.toUByteArray()
                        }catch (e: Exception) {
                            reportError("Fehlerhafte Eingabe!")
                            return@setUShortOrComplain
                        }

                        for(mem in data.indices)
                            ram.setData(data[mem], memoryAddress plusSigned mem)
                    }
                }
                "" -> {
                    cpu.step()
                    mainbus.performClockActions()
                    updateScreen(screen)
                    //textscreen.screenshot()
                }
                "bl" -> {
                    writeLine(breakpoints.map { it.toString("X4") }.joinToString(", "))
                    requestRawInput()
                }
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
                else -> reportError("Unbekannter befehl! Drücke Tab für eine übersicht der Befehle.")
            }

        }
    }
}