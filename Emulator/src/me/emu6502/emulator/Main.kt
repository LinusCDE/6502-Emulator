package me.emu6502.emulator

fun main(args: Array<String>) {
    if(!args.contains("--gui")) {
        Emulator(
                requestCommand = { Console.readLine("> ") },
                clear = { Console.clear() },
                defineCommand = { name, displayName, desc -> Console.completer.addCommand(name, displayName, desc) },
                requestRawInput = {
                    Console.completer.completionEnabled = false
                    val input = Console.readLine()
                    Console.completer.completionEnabled = true
                    input
                },
                requestAnyAction = { Console.readKey() },
                updateScreen = { it.screenshot() },
                write = { Console.write(it) },
                writeLine = { Console.writeLine(it) }
        )
    }
}