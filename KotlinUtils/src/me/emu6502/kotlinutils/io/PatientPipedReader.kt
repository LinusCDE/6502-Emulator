package me.emu6502.kotlinutils.io

import java.io.IOException
import java.io.PipedReader
import java.io.PipedWriter

class PatientPipedReader: PipedReader {

    constructor(): super()
    constructor(writer: PipedWriter): super(writer)

    override fun read(): Int {
        while(true) {
            try {
                return super.read()
            } catch (ex: IOException) {
                if (ex.message == "Pipe not connected" || ex.message == "Write end dead")
                    Thread.yield()
                else
                    throw ex
            }
        }
    }

    override fun read(p0: CharArray, p1: Int, p2: Int): Int {
        try {
            return super.read(p0, p1, p2)
        } catch (ex: IOException) {
            if (ex.message == "Pipe not connected" || ex.message == "Write end dead")
                return 0
            else
                throw ex
        }
    }

    override fun ready(): Boolean {
        try {
            return super.ready()
        } catch (ex: IOException) {
            if (ex.message == "Pipe not connected" || ex.message == "Write end dead")
                return false
            else
                throw ex
        }
    }
}