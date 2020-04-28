package me.emu6502.emulator

import me.emu6502.emulator.ui.EmulatorApp
import tornadofx.launch
import kotlin.system.exitProcess

var darkModeEnabled = false;

fun main(args: Array<String>) {
    if(args.contains("--cli") || args.contains("-i")) {
        // Launch in console
        val emulator = Emulator(
                clear = { Console.clear() },
                defineCommand = { name, displayName, desc -> Console.completer.addCommand(name, displayName, desc) },
                reportError = { Console.writeLine(it); Console.readKey() },
                updateScreen = { it.screenshot() },
                updatePia = { /* Not supported in console, yet */ },
                write = { Console.write(it) },
                writeLine = { Console.writeLine(it) }
        )

        emulator.printStatus()
        while(true)
            emulator.executeDebuggerCommand(Console.readLine("> "))
    }

    if(args.contains("-d") || args.contains("--dark-mode"))
        darkModeEnabled = true;

    // Launch GUI
    Console.writeLine("Launching GUI...")
    if(!darkModeEnabled)
        Console.writeLine("Heads up! You can use a dark mode. For that you need to pass the -d or --dark-mode flag.")
    Console.writeLine("To use the console mode, pass the --cli flag.")
    launch<EmulatorApp>(args)
    exitProcess(0)
}