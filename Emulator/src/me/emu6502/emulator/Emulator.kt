package me.emu6502.emulator

import me.emu6502.lib6502.Bus
import me.emu6502.lib6502.CPU
import me.emu6502.lib6502.RAM
import me.emu6502.lib6502.ROM
import plusSigned
import toUByteArray
import ubyte
import ushort
import java.io.File
import toString
import java.lang.Exception
import kotlin.system.exitProcess

class Emulator {
    companion object {
        @JvmStatic fun main(args: Array<out String>) {
            Emulator()
        }
    }

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
        val bbytes: UByteArray = File("textscreen01.bin").readBytes().toUByteArray()
        for (pc in bbytes.indices)
            ram.setData(bbytes[pc], (0x0200 + pc).ushort)
        mainbus.devices.add(ram)

        rom = ROM(4096.ushort, 0xF000.ushort)
        val initrom = UByteArray(4096)
        initrom[0x0FFD] = 0x02.ubyte
        Routines.pixelDspRoutine.forEachIndexed { i, byte -> initrom[0x0000 + i] = byte }
        Routines.charDspRoutine.forEachIndexed { i, byte -> initrom[0x001C + i] = byte }

        rom.setMemory(initrom)
        mainbus.devices.add(rom)

        charrom = ROM(1024.ushort, 0xEC00.ushort)
        charrom.setMemory(File("apple1.vid").readBytes().toUByteArray())

        screen = Screen(160, 120, 0xD000.ushort).apply { reset() }
        mainbus.devices.add(screen)

        textscreen = TextScreen(40, 25, 0xD004.ushort).apply { reset() }
        mainbus.devices.add(textscreen)

        cpu = CPU(mainbus).apply { PC = 0x0200.ushort }
    }

    private fun setUShortOrComplain(cmdArgs: List<String>, callback: ((value: UShort) -> Unit)) {
        try {
            callback(cmdArgs[0].toInt(16).ushort)
        }catch (e: Exception) {
            // Number not parsable or argument missing
            Console.writeLine("Fehlerhafte Eingabe!")
            Console.readKey()
        }
    }

    private fun setUByteOrComplain(cmdArgs: List<String>, callback: ((value: UByte) -> Unit)) {
        try {
            callback(cmdArgs[0].toInt(16).ubyte)
        }catch (e: Exception) {
            // Number not parsable or argument missing
            Console.writeLine("Fehlerhafte Eingabe!")
            Console.readKey()
        }
    }

    init {
        // Console setup
        Console.resizeMac(140, 40)
        //Console.SetWindowSize(140, 43)
        Console.completer.addCommand("q", "q", "Quit")
        Console.completer.addCommand("ra", "ra", "Reset all")
        Console.completer.addCommand("rc", "rc", "Reset CPU")
        Console.completer.addCommand("a", "a", "Set register A")
        Console.completer.addCommand("x", "x", "Set register X")
        Console.completer.addCommand("y", "y", "Set register X")
        Console.completer.addCommand("sr", "sr", "Set register StatusRegister")
        Console.completer.addCommand("sp", "sp", "Set register StackPointer")
        Console.completer.addCommand("pc", "pc", "Set register ProgramCounter")
        Console.completer.addCommand("d", "d", "Set current page")
        Console.completer.addCommand("m", "m", "Write custom data to memory address")
        Console.completer.addCommand("", "<Enter>", "Step. Run single instruction and make screenshot")
        Console.completer.addCommand("bl", "bl", "List breakpoints")
        Console.completer.addCommand("ba", "ba", "Add breakpoint")
        Console.completer.addCommand("br", "br", "Remove breakpoint")
        Console.completer.addCommand("r", "r", "Run until breakpoint or end")

        reset()

        var currentpage: UShort = 0x0000.ushort
        val breakpoints = arrayListOf<UShort>()

        while (true)
        {
            Console.clear()
            Console.writeLine(cpu)
            var line: Int = currentpage.toInt()
            while(line < if((currentpage + 0x0400.ushort) > 65536.ushort) 65536 else currentpage.toInt() + 0x0400) {
                Console.write("$" + line.toString("X4") + ":")
                for (pc in line until (line + 32))
                    Console.write(" $" + mainbus.getData(pc.ushort).toString("X2"))
                Console.writeLine()
                line += 32
            }
            Console.writeLine()
            val inputLine = Console.readLine(">").trim()
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

                        Console.completer.completionEnabled = false
                        val line = Console.readLine()
                        Console.completer.completionEnabled = true

                        val data: UByteArray
                        try {
                            data = line.split(' ').map { it.toInt().ubyte }.toUByteArray()
                        }catch (e: Exception) {
                            Console.writeLine("Fehlerhafte Eingabe!")
                            Console.readKey()
                            return@setUShortOrComplain
                        }

                        for(mem in data.indices)
                            ram.setData(data[mem], memoryAddress plusSigned mem)
                    }
                }
                "" -> {
                    cpu.step()
                    mainbus.performClockActions()
                    screen.screenshot()
                    textscreen.screenshot() // TODO: Screenshot of textscreen overwrites screenshot of screen!
                }
                "bl" -> {
                    Console.writeLine(breakpoints.map { it.toString("X4") }.joinToString(", "))
                    Console.readKey()
                }
                "ba" -> setUShortOrComplain(cmdArgs) { breakpoints.add(it) }
                "br" -> setUShortOrComplain(cmdArgs) { breakpoints.remove(it) }
                "r" -> {
                    do {
                        cpu.step()
                    } while (!breakpoints.contains(cpu.PC) && mainbus.getData(cpu.PC) != 0x00.ubyte)
                    screen.screenshot()
                    textscreen.screenshot() // TODO: Screenshot of textscreen overwrites screenshot of screen!
                }
                else -> {
                    Console.writeLine("Unbekannter befehl! Drücke Tab für eine übersicht der Befehle.")
                    Console.readKey()
                }
            }

        }
    }
}