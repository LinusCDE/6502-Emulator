package me.emu6502.emulator

import me.emu6502.emulator.ui.EmulatorApp
import tornadofx.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if(args.contains("--gui") || args.contains("-g")) {
        // Launch GUI
        Console.writeLine("Launching GUI...")
        launch<EmulatorApp>(args)
        exitProcess(0)
    }

    // Launch in console
    val emulator = Emulator(
            clear = { Console.clear() },
            defineCommand = { name, displayName, desc -> Console.completer.addCommand(name, displayName, desc) },
            reportError = { Console.writeLine(it); Console.readKey() },
            updateScreen = { it.screenshot() },
            write = { Console.write(it) },
            writeLine = { Console.writeLine(it) }
    )

    emulator.printStatus()
    while(true)
        emulator.executeDebuggerCommand(Console.readLine("> "))
}