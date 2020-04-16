package me.emu6502.kotlinutils.io

import java.io.IOException
import java.io.PipedReader
import java.io.PipedWriter

class PatientPipedWriter: PipedWriter {

    constructor(): super()
    constructor(reader: PipedReader): super(reader)

    override fun flush() {
        while(true) {
            try {
                super.flush()
                return
            } catch (ex: IOException) {
                if (ex.message == "Pipe not connected")
                    Thread.yield()
                else
                    throw ex
            }
        }
    }

    override fun write(p0: Int) {
        while(true) {
            try {
                super.write(p0)
                return
            } catch (ex: IOException) {
                if (ex.message == "Pipe not connected")
                    Thread.yield()
                else
                    throw ex
            }
        }
    }

    override fun write(p0: CharArray, p1: Int, p2: Int) {
        while(true) {
            try {
                super.write(p0, p1, p2)
                return
            } catch (ex: IOException) {
                if (ex.message == "Pipe not connected")
                    Thread.yield()
                else
                    throw ex
            }
        }
    }

}