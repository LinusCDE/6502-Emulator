# 6502-Emulator

This project is a port of [bomberman2910](https://github.com/bomberman2910)'s [work](https://github.com/bomberman2910/6502Tests) with his aid.

The Lib6502 module is a direct port from his repo and Emulator is based on the emu6520 module and has an additional GUI added to it.

The project aims to make that work cross-platform and create an easy-to-use ui for educational purposes.

## Build and run

To create the final jar run `mvn package`. The final file will be in Emulator/target.

You can just run it with `java -jar emulator-**-jar-with-dependencies.jar` to use it in cli mode. To open the GUI, append the parameter `--gui` to the previous command.

You can also of course import this in IntelliJ, create a Application-Configuration and run that.
