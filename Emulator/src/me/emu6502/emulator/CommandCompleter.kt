package me.emu6502.emulator

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine


class CommandCompleter() : Completer {

    var completionEnabled = true
    val commands = arrayListOf<Candidate>()
    private var commandsSorted = false

    fun addCommand(name: String, displayName: String? = null, description: String? = null) {
        commands.add(Candidate(name, displayName, null, description, null, null, true))
        commandsSorted = false
    }

    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        if(!completionEnabled)
            return
        if(line.wordIndex() != 0)
            return

        if(!commandsSorted) {
            commands.sortBy { it.value() }
            commandsSorted = true
        }

        // Complete first word
        val word = line.word()
        for(command in commands) {
            if (command.value().startsWith(word) && command.value() != "")
                candidates.add(command)
            if(command.value() == "" && word == "")
                candidates.add(command) // Show "" command when tabbing without anything typed
        }
    }
}