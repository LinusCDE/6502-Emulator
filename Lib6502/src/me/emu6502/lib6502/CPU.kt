package me.emu6502.lib6502

import BitConverter
import plus
import shl
import shr
import toString
import ubyte
import ushort
import kotlin.math.log2

class CPU(val bus: Bus) {

    //will not (yet) support decimal mode

    var cycles = 0

    var PC: UShort = ((bus.getData(0xFFFD.ushort) shl 8) + bus.getData(0xFFFC.ushort)).ushort
    var SP: UByte = 0xFF.ubyte
    var A: UByte = 0x00.ubyte
    var X: UByte = 0x00.ubyte
    var Y: UByte = 0x00.ubyte
    var SR: UByte = 0x20.ubyte //N V - - D I Z C

    var NMI: Boolean = false
    var IRQ: Boolean = false


    //region Helpers
    companion object {
        fun checkBit(value: UByte, bit: Int): Boolean = ((value shl (7 - bit)) shr 7) == 1.ubyte
    }

    override fun toString(): String {
        var output = ""
        output += "A: ${A.toString("X2")}\tX: ${X.toString("X2")}\tY: ${Y.toString("X2")}\tSP: ${SP.toString("X2")}\tPC: ${PC.toString("X4")}"
        output += "\tSR: ${if(checkFlag(EFlag.NEG)) "N" else "-"}${if(checkFlag(EFlag.OVR)) "O" else "-"}${if(checkFlag(EFlag.BRK)) "B" else "-"}-" +
                "${if(checkFlag(EFlag.DEC)) "D" else "-"}${if(checkFlag(EFlag.IRQ)) "I" else "-"}${if(checkFlag(EFlag.ZER)) "Z" else "-"}${if(checkFlag(EFlag.CAR)) "C" else "-"}\t"
        output += "Instruction: ${Disassembler.disassemble(ubyteArrayOf(bus.getData(PC), bus.getData((PC + 1.ushort).ushort), bus.getData((PC + 2.ushort).ushort)), 0)}\n\n"
        return output
    }

    fun reset() {
        PC = ((bus.getData(0xFFFD.ushort) shl 8) + bus.getData(0xFFFC.ushort)).ushort
        SP = 0xFF.ubyte
        A = 0x00.ubyte
        X = 0x00.ubyte
        Y = 0x00.ubyte
        SR = 0x20.ubyte //N V - - D I Z C

        cycles = 0
        // TODO: Also reset NMI and IRQ?
    }
    //endregion

    //region Status register methods
    private fun checkFlag(flag: UByte): Boolean = ((SR and flag) shr log2(flag.toDouble()).toInt()) == 1.ubyte

    private fun setFlag(flag: UByte, state: Boolean)
    {
        if (state)
            SR = SR or flag
        else
            SR = SR and flag.inv()
    }

    private fun testForCarry(value: UShort): Boolean = (value < 0.ushort) || (value > 255.ushort)

    private fun testForOverflow(value: UShort): Boolean = (value < (-128).ushort) || (value > 127.ushort)
    //endregion

    //region Stack operations
    private fun pushToStack(value: UByte) {
        bus.setData(value, (0x0100 + SP).ushort)
        SP--
    }

    private fun pullFromStack(): UByte {
        SP++
        return bus.getData((0x0100 + SP).ushort)
    }
    //endregion

    //region Interrupt routines
    fun handleIRQ() {
        pushToStack(BitConverter.GetBytes(PC)[1])
        pushToStack(BitConverter.GetBytes(PC)[0])
        pushToStack(SR)
        setFlag(EFlag.IRQ, true)
        PC = ((bus.getData(0xFFFF.ushort) shl 8) + bus.getData(0xFFFE.ushort)).ushort
        cycles = 8
    }

    fun handleNMI() {
        pushToStack(BitConverter.GetBytes(PC)[1])
        pushToStack(BitConverter.GetBytes(PC)[0])
        pushToStack(SR)
        setFlag(EFlag.IRQ, true)
        NMI = false
        PC = ((bus.getData(0xFFFB.ushort) shl 8) + bus.getData(0xFFFA.ushort)).ushort
        cycles = 8
    }
    //endregion

    //region Addressing modes
    private fun getRelAddr(): UShort = (PC + (bus.getData(PC + 1).toByte() + 2.toByte()).ushort).ushort

    private fun getAbsAddr(): UShort = ((bus.getData(PC + 2) shl 8) + bus.getData(PC + 1)).ushort

    private fun getAbsXAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort{
        onUpdateWrap((bus.getData((PC + 1)) + X) > 0xFF.ushort)
        return ((bus.getData(PC + 2) shl 8) + bus.getData(PC + 1) + X + (if (SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }

    private fun getAbsYAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort
    {
        onUpdateWrap((bus.getData(PC + 1) + X).ushort > 0xFF.ushort)
        return ((bus.getData(PC + 2) shl 8) + bus.getData(PC + 1) + Y + (if (SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }

    private fun getZPAddr(): UByte = bus.getData((PC + 1.ushort).ushort)

    private fun getZPXAddr(): UByte = (bus.getData((PC + 1.ushort).ushort) + X.ushort).ubyte

    private fun getZPYAddr(): UByte = (bus.getData((PC + 1.ushort).ushort) + Y.ushort).ubyte

    private fun getIndXAddr(): UShort
    {
        val index = (bus.getData(PC + 1) + X).ubyte
        return ((bus.getData(index + 1.ubyte) shl 8) + bus.getData(index.ushort)).ushort
    }

    private fun getIndYAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort
    {
        val index = bus.getData(PC + 1)
        onUpdateWrap(index + Y > 0xFF.ushort)
        return ((bus.getData(index + 1.ubyte) shl 8) + bus.getData(index.ushort) + Y + ( if(SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }
    //endregion

    //region Operations with multiple addressing modes (except store operations)
    private fun ADC(value: UByte): Unit {
        val sum = (A + value + if (checkFlag(EFlag.CAR)) 1 else 0).ushort
        setFlag(EFlag.OVR, checkBit(A, 7) === checkBit(value, 7) && checkBit(A, 7) !== checkBit(sum.ubyte, 7))
        setFlag(EFlag.CAR, sum > 255.ushort || sum < 0.ushort)
        setFlag(EFlag.NEG, checkBit(sum.ubyte, 7))
        setFlag(EFlag.ZER, sum.toInt() == 0)
        A = sum.ubyte
    }

    private fun AND(value: UByte) {
        A = A and value
        setFlag(EFlag.ZER, A == 0.ubyte)
        setFlag(EFlag.NEG, checkBit(A, 7))
    }

    private fun ASL(value: UByte): UByte {
        var value = value
        setFlag(EFlag.CAR, checkBit(value, 7))
        value = value shl 1
        setFlag(EFlag.NEG, checkBit(value, 7))
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun BIT(value: UByte) {
        val temp = A and value
        setFlag(EFlag.ZER, temp == 0.ubyte)
        setFlag(EFlag.OVR, checkBit(temp.toUByte(), 6))
        setFlag(EFlag.NEG, checkBit(temp.toUByte(), 7))
    }

    private fun CMP(value: UByte) {
        when {
            A < value.toUInt() -> {
                setFlag(EFlag.NEG, checkBit((A - value).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, false)
            }
            A == value -> {
                setFlag(EFlag.NEG, false)
                setFlag(EFlag.ZER, true)
                setFlag(EFlag.CAR, true)
            }
            else -> {
                setFlag(EFlag.NEG, checkBit((A - value).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, true)
            }
        }
    }

    private fun CPX(value: UByte) {
        when {
            X < value.toUInt() -> {
                setFlag(EFlag.NEG, checkBit((X - value).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, false)
            }
            X == value -> {
                setFlag(EFlag.NEG, false)
                setFlag(EFlag.ZER, true)
                setFlag(EFlag.CAR, true)
            }
            else -> {
                setFlag(EFlag.NEG, checkBit((X - value).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, true)
            }
        }
    }

    private fun CPY(value: UByte) {
        when {
            Y < value.toUInt() -> {
                setFlag(EFlag.NEG, checkBit((Y - value.toUInt()).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, false)
            }
            Y == value -> {
                setFlag(EFlag.NEG, false)
                setFlag(EFlag.ZER, true)
                setFlag(EFlag.CAR, true)
            }
            else -> {
                setFlag(EFlag.NEG, checkBit((Y - value).ubyte, 7))
                setFlag(EFlag.ZER, false)
                setFlag(EFlag.CAR, true)
            }
        }
    }

    private fun DEC(value: UByte): UByte {
        var value = value
        value--
        setFlag(EFlag.NEG, checkBit(value, 7))
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun EOR(value: UByte) {
        A = A xor value
        setFlag(EFlag.NEG, checkBit(A, 7))
        setFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun INC(value: UByte): UByte {
        var value = value
        value++
        setFlag(EFlag.NEG, checkBit(value, 7))
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun JMP(address: UShort) {
        PC = address
    }

    private fun LDA(value: UByte) {
        A = value
        setFlag(EFlag.NEG, checkBit(A, 7))
        setFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun LDX(value: UByte) {
        X = value
        setFlag(EFlag.NEG, checkBit(X, 7))
        setFlag(EFlag.ZER, X == 0.ubyte)
    }

    private fun LDY(value: UByte) {
        Y = value
        setFlag(EFlag.NEG, checkBit(Y, 7))
        setFlag(EFlag.ZER, Y == 0.ubyte)
    }

    private fun LSR(value: UByte): UByte {
        var value = value
        setFlag(EFlag.CAR, checkBit(value, 0))
        value = value shr 1
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun ORA(value: UByte) {
        A = A or value
        setFlag(EFlag.NEG, checkBit(A, 7))
        setFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun ROL(value: UByte): UByte {
        var value = value
        val carrytemp = checkFlag(EFlag.CAR)
        setFlag(EFlag.CAR, checkBit(value, 7))
        value = value shl 1
        if (carrytemp) value++
        setFlag(EFlag.NEG, checkBit(value, 7))
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun ROR(value: UByte): UByte {
        var value = value
        val carrytemp = checkFlag(EFlag.CAR)
        setFlag(EFlag.CAR, checkBit(value, 0))
        value = value shr 1
        if (carrytemp) value = (value + (1 shl 7).ubyte).ubyte
        setFlag(EFlag.NEG, checkBit(value, 7))
        setFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun SBC(value: UByte) = ADC(value.inv())
    //endregion

    //region Execution
    //region Execution
    fun exec() {
        if (!checkFlag(EFlag.IRQ) && IRQ)
            handleIRQ()
        if (NMI)
            handleNMI()

        if (cycles == 0) {
            val instruction = bus.getData(PC)
            var wrap = false

            when (instruction.toInt()) {
                0x00 -> { //BRK
                    cycles = 7
                    PC = (PC + 2).ushort
                    pushToStack(BitConverter.GetBytes(PC)[1])
                    pushToStack(BitConverter.GetBytes(PC)[0])
                    pushToStack(SR)
                    setFlag(EFlag.BRK, true)
                    setFlag(EFlag.IRQ, true)
                    PC = ((bus.getData(0xFFFF.ushort) shl 8) + bus.getData(0xFFFE.ushort)).ushort
                }
                0x01 -> {  //ORA   (indirect, X)
                    cycles = 6
                    ORA(bus.getData(getIndXAddr()))
                    PC = (PC + 2).ushort
                }
                0x05 -> {  //ORA   (zeropage)
                    cycles = 3
                    ORA(bus.getData(getZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x06 -> {  //ASL   (zeropage)
                    cycles = 5
                    bus.setData(ASL(bus.getData(getZPAddr())), getZPAddr())
                    PC = (PC + 2).ushort
                }
                0x08 -> {  //PHP
                    cycles = 3
                    pushToStack(SR)
                    PC = (PC + 1).ushort
                }
                0x09 -> {  //ORA   (immediate)
                    cycles = 2
                    ORA(bus.getData(PC + 1))
                    PC = (PC + 2).ushort
                }
                0x0A -> {  //ASL   (accumulator)
                    cycles = 2
                    A = ASL(A)
                    PC = (PC + 1).ushort
                }
                0x0D -> {  //ORA   (absolute)
                    cycles = 4
                    ORA(bus.getData(getAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x0E -> {  //ASL   (absolute)
                    cycles = 6
                    bus.setData(ASL(bus.getData(getAbsAddr())), getAbsAddr())
                    PC = (PC + 3).ushort
                }
                0x10 -> {  //BPL   (relative)
                    if (checkFlag(EFlag.NEG)) {
                        cycles = 2
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    }
                }
                0x11 -> {  //ORA   (indirect, Y)
                    ORA(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if (wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0x15 -> {  //ORA   (zeropage, X)
                    ORA(bus.getData(getZPXAddr()))
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x16 -> {  //ASL   (zeropage, X)
                    cycles = 6
                    bus.setData(ASL(bus.getData(getZPXAddr())), getZPXAddr())
                    PC = (PC + 2).ushort
                }
                0x18 -> {  //CLC
                    cycles = 2
                    setFlag(EFlag.CAR, false)
                    PC = (PC + 1).ushort
                }
                0x19 -> {  //ORA   (absolute, Y)
                    ORA(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if (wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x1D -> {  //ORA   (absolute, X)
                    ORA(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if (wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x1E -> {  //ASL   (absolute, X)
                    cycles = 7
                    bus.setData(ASL(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    PC = (PC + 3).ushort
                }
                0x20 -> {  //JSR   (absolute)
                    cycles = 6
                    pushToStack(BitConverter.GetBytes((PC + 2).ushort)[1])
                    pushToStack(BitConverter.GetBytes((PC + 2).ushort)[0])
                    PC = getAbsAddr()
                }
                0x21 -> {  //AND   (indirect, X)
                    cycles = 6
                    AND(bus.getData(getIndXAddr()))
                    PC = (PC + 2).ushort
                }
                0x24 -> {  //BIT   (zeropage)
                    cycles = 3
                    BIT(bus.getData(getZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x25 -> {  //AND   (zeropage)
                    cycles = 3
                    AND(bus.getData(getZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x26 -> {  //ROL   (zeropage)
                    cycles = 5
                    ROL(bus.getData(getZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x28 -> {  //PLP
                    SR = pullFromStack()
                    PC = (PC + 1).ushort
                }
                0x29 -> {  //AND   (immediate)
                    cycles = 2
                    AND(bus.getData(PC + 1))
                    PC = (PC + 2).ushort
                }
                0x2A -> {  //ROL   (accumulator)
                    cycles = 2
                    A = ROL(A)
                    PC = (PC + 1).ushort
                }
                0x2C -> {  //BIT   (absolute)
                    cycles = 4
                    BIT(bus.getData(getAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x2D -> {  //AND   (absolute)
                    cycles = 4
                    AND(bus.getData(getAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x2E -> {  //ROL   (absolute)
                    cycles = 6
                    bus.setData(ROL(bus.getData(getAbsAddr())), getAbsAddr())
                    PC = (PC + 3).ushort
                }
                0x30 -> {  //BMI   (relative)
                    if (checkFlag(EFlag.NEG)) {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    } else {
                        cycles = 2
                        PC = (PC + 2).ushort
                    }
                }
                0x31 -> {  //AND   (indirect, Y)
                    AND(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0x35 -> {  //AND   (zeropage, X)
                    cycles = 4
                    AND(bus.getData(getZPXAddr()))
                    PC = (PC + 2).ushort
                }
                0x36 -> {  //ROL   (zeropage, x)
                    bus.setData(ROL(bus.getData(getZPXAddr())), getZPXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x38 -> {  //SEC
                    cycles = 2
                    setFlag(EFlag.CAR, true)
                    PC = (PC + 1).ushort
                }
                0x39 -> {  //AND   (absolute, Y)
                    AND(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x3D -> {  //AND   (absolute, X)
                    AND(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x3E -> {  //ROL   (absolute, X)
                    bus.setData(ROL(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    cycles = 7
                    PC = (PC + 3).ushort
                }
                0x40 -> {  //RTI
                    cycles = 6
                    SR = pullFromStack()
                    PC = (pullFromStack() + (pullFromStack() shl 8)).ushort
                }
                0x41 -> {  //EOR   (indirect, Y)
                    EOR(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0x45 -> {  //EOR   (zeropage)
                    EOR(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0x46 -> {  //LSR   (zeropage)
                    bus.setData(LSR(bus.getData(getZPAddr())), getZPAddr())
                    cycles = 5
                    PC = (PC + 2).ushort
                }
                0x48 -> {  //PHA
                    cycles = 3
                    pushToStack(A)
                    PC = (PC + 1).ushort
                }
                0x49 -> {  //EOR   (immediate)
                    EOR(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0x4A -> {  //LSR   (accumulator)
                    A = LSR(A)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x4C -> {  //JMP   (absolute)
                    cycles = 3
                    JMP(getAbsAddr())
                }
                0x4D -> {  //EOR   (absolute)
                    cycles = 4
                    EOR(bus.getData(getAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x4E -> {  //LSR   (absolute)
                    bus.setData(LSR(bus.getData(getAbsAddr())), getAbsAddr())
                    cycles = 6
                    PC = (PC + 3).ushort
                }
                0x50 -> {  //BVC   (relative)
                    if (checkFlag(EFlag.OVR)) {
                        cycles = 2
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    }
                }
                0x51 -> {  //EOR   (indirect, Y)
                    EOR(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0x55 -> {  //EOR   (zeropage, X)
                    EOR(bus.getData(getZPXAddr()))
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x56 -> {  //LSR   (zeropage, X)
                    bus.setData(LSR(bus.getData(getZPXAddr())), getZPXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x58 -> {  //CLI
                    setFlag(EFlag.IRQ, false)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x59 -> {  //EOR   (absolute, Y)
                    EOR(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x5D -> {  //EOR   (absolute, X)
                    EOR(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x5E -> {  //LSR   (absolute, X)
                    bus.setData(LSR(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    cycles = 7
                    PC = (PC + 3).ushort
                }
                0x60 -> {  //RTS
                    cycles = 6
                    PC = (pullFromStack() + (pullFromStack() shl 8)).ushort
                    PC = (PC + 1).ushort
                }
                0x61 -> {  //ADC   (indirect, X)
                    ADC(bus.getData(getIndXAddr()))
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x65 -> {  //ADC   (zeropage)
                    ADC(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0x66 -> {  //ROR   (zeropage)
                    bus.setData(ROR(bus.getData(getZPAddr())), getZPAddr())
                    cycles = 5
                    PC = (PC + 2).ushort
                }
                0x68 -> {  //PLA
                    cycles = 4
                    A = pullFromStack()
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0x69 -> {  //ADC   (immediate)
                    ADC(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0x6A -> {  //ROR   (accumulator)
                    A = ROR(A)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x6C -> {  //JMP   (indirect)
                    PC = ((bus.getData(getAbsAddr()) shl 8) + bus.getData((getAbsAddr() + 1).ushort)).ushort
                    cycles = 5
                }
                0x6D -> {  //ADC   (absolute)
                    ADC(bus.getData(getAbsAddr()))
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0x6E -> {  //ROR   (absolute)
                    bus.setData(ROR(bus.getData(getAbsAddr())), getAbsAddr())
                    cycles = 6
                    PC = (PC + 3).ushort
                }
                0x70 -> {  //BVS   (relative)
                    if (checkFlag(EFlag.OVR)) {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    } else {
                        cycles = 2
                        PC = (PC + 2).ushort
                    }
                }
                0x71 -> {  //ADC   (indirect, Y)
                    ADC(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0x75 -> {  //ADC   (zeropage, X)
                    ADC(bus.getData(getZPXAddr()))
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x76 -> {  //ROR   (zeropage, X)
                    bus.setData(ROR(bus.getData(getZPXAddr())), getZPXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x78 -> {  //SEI
                    setFlag(EFlag.IRQ, true)
                    PC = (PC + 1).ushort
                    cycles = 2
                }
                0x79 -> {  //ADC   (absolute, Y)
                    ADC(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x7D -> {  //ADC   (absolute, X)
                    ADC(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0x7E -> {  //ROR   (absolute, X)
                    bus.setData(ROR(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    cycles = 7
                    PC = (PC + 3).ushort
                }
                0x81 -> {  //STA   (indirect, X)
                    bus.setData(A, getIndXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x84 -> {  //STY   (zeropage)
                    bus.setData(Y, getZPAddr())
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0x85 -> {  //STA   (zeropage)
                    bus.setData(A, getZPAddr())
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0x86 -> {  //STX   (zeropage)
                    bus.setData(X, getZPAddr())
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0x88 -> {  //DEY
                    Y--
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x8A -> {  //TXA
                    A = X
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x8C -> {  //STY   (absolute)
                    bus.setData(Y, getAbsAddr())
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0x8D -> {  //STA   (absolute)
                    bus.setData(A, getAbsAddr())
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0x8E -> {  //STX   (absolute)
                    bus.setData(X, getAbsAddr())
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0x90 -> {  //BCC   (relative)
                    if (checkFlag(EFlag.CAR)) {
                        cycles = 2
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    }
                }
                0x91 -> {  //STA   (indirect, Y)
                    bus.setData(A, getIndYAddr({ wrap = it }))
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0x94 -> {  //STY   (zeropage, X)
                    bus.setData(Y, getZPXAddr())
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x95 -> {  //STA   (zeropage, X)
                    bus.setData(A, getZPXAddr())
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x96 -> {  //STX   (zeropage, Y)
                    bus.setData(X, getZPYAddr())
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0x98 -> {  //TYA
                    A = Y
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0x99 -> {  //STA   (absolute, Y)
                    bus.setData(A, getAbsYAddr({ wrap = it }))
                    cycles = 5
                    PC = (PC + 3).ushort
                }
                0x9A -> {  //TXS
                    SP = X
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0x9D -> {  //STA   (absolute, X)
                    bus.setData(A, getAbsXAddr({ wrap = it }))
                    cycles = 5
                    PC = (PC + 3).ushort
                }
                0xA0 -> {  //LDY   (immediate)
                    Y = bus.getData(PC + 1)
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA1 -> {  //LDA   (indirect, X)
                    A = bus.getData(getIndXAddr())
                    cycles = 6
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA2 -> {  //LDX   (immediate)
                    X = bus.getData(PC + 1)
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA4 -> {  //LDY   (zeropage)
                    Y = bus.getData(getZPAddr())
                    cycles = 3
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA5 -> {  //LDA   (zeropage)
                    A = bus.getData(getZPAddr())
                    cycles = 3
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA6 -> {  //LDX   (zeropage)
                    X = bus.getData(getZPAddr())
                    cycles = 3
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA8 -> {  //TAY
                    Y = A
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xA9 -> {  //LDA   (immediate)
                    A = bus.getData(PC + 1)
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xAA -> {  //TAX
                    X = A
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xAC -> {  //LDY   (absolute)
                    Y = bus.getData(getAbsAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xAD -> {  //LDA   (absolute)
                    A = bus.getData(getAbsAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xAE -> {  //LDX   (absolute)
                    X = bus.getData(getAbsAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xB0 -> {  //BCS   (relative)
                    if (checkFlag(EFlag.CAR)) {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    } else {
                        cycles = 2
                        PC = (PC + 2).ushort
                    }
                }
                0xB1 -> {  //LDA   (indirect, Y)
                    A = bus.getData(getIndYAddr({ wrap = it }))
                    cycles = if(wrap) 6 else 5
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB4 -> {  //LDY   (zeropage, X)
                    Y = bus.getData(getZPXAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB5 -> {  //LDA   (zeropage, X)
                    A = bus.getData(getZPXAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB6 -> {  //LDX   (zeropage, Y)
                    X = bus.getData(getZPYAddr())
                    cycles = 4
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB8 -> {  //CLV
                    setFlag(EFlag.OVR, false)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0xB9 -> {  //LDA   (absolute, Y)
                    A = bus.getData(getAbsYAddr({ wrap = it }))
                    cycles = if(wrap) 5 else 4
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBA -> {  //TSX
                    X = SP
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xBC -> {  //LDY   (absolute, X)
                    Y = bus.getData(getAbsXAddr({ wrap = it }))
                    cycles = if(wrap) 5 else 4
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBD -> {  //LDA   (absolute, X)
                    A = bus.getData(getAbsXAddr({ wrap = it }))
                    cycles = if(wrap) 5 else 4
                    setFlag(EFlag.NEG, checkBit(A, 7))
                    setFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBE -> {  //LDX   (absolute, Y)
                    X = bus.getData(getAbsYAddr({ wrap = it }))
                    cycles = if(wrap) 5 else 4
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xC0 -> {  //CPY   (immediate)
                    CPY(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0xC1 -> {  //CMP   (indirect, X)
                    CMP(bus.getData(getIndXAddr()))
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0xC4 -> {  //CPY   (zeropage)
                    CPY(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0xC5 -> {  //CMP   (zeropage)
                    CMP(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0xC6 -> {  //DEC   (zeropage)
                    bus.setData(DEC(bus.getData(getZPAddr())), getZPAddr())
                    cycles = 5
                    PC = (PC + 2).ushort
                }
                0xC8 -> {  //INY
                    Y++
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(Y, 7))
                    setFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xC9 -> {  //CMP   (immediate)
                    CMP(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0xCA -> {  //DEX
                    X--
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xCC -> {  //CPY   (absolute)
                    CPY(bus.getData(getAbsAddr()))
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0xCD -> {  //CMP   (absolute)
                    CMP(bus.getData(getAbsAddr()))
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0xCE -> {  //DEC   (absolute)
                    bus.setData(DEC(bus.getData(getAbsAddr())), getAbsAddr())
                    cycles = 6
                    PC = (PC + 3).ushort
                }
                0xD0 -> {  //BNE   (relative)
                    if (checkFlag(EFlag.ZER)) {
                        cycles = 2
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    }
                }
                0xD1 -> {  //CMP   (indirect, Y)
                    CMP(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0xD5 -> {  //CMP   (zeropage, X)
                    CMP(bus.getData(getZPXAddr()))
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0xD6 -> {  //DEC   (zeropage, X)
                    bus.setData(DEC(bus.getData(getZPXAddr())), getZPXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0xD8 -> {  //CLD
                    setFlag(EFlag.DEC, false)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0xD9 -> {  //CMP   (absolute, Y)
                    CMP(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0xDD -> {  //CMP   (absolute, X)
                    CMP(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0xDE -> {  //DEC   (absolute, X)
                    bus.setData(DEC(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    cycles = 7
                    PC = (PC + 3).ushort
                }
                0xE0 -> {  //CPX   (immediate)
                    CPX(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0xE1 -> {  //SBC   (indirect, X)
                    SBC(bus.getData(getIndXAddr()))
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0xE4 -> {  //CPX   (zeropage)
                    CPX(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0xE5 -> {  //SBC   (zeropage)
                    SBC(bus.getData(getZPAddr()))
                    cycles = 3
                    PC = (PC + 2).ushort
                }
                0xE6 -> {  //INC   (zeropage)
                    bus.setData(INC(bus.getData(getZPAddr())), getZPAddr())
                    cycles = 5
                    PC = (PC + 2).ushort
                }
                0xE8 -> {  //INX
                    X++
                    cycles = 2
                    setFlag(EFlag.NEG, checkBit(X, 7))
                    setFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xE9 -> {  //SBC   (immediate)
                    SBC(bus.getData(PC + 1))
                    cycles = 2
                    PC = (PC + 2).ushort
                }
                0xEA -> {  //NOP
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0xEC -> {  //CPX   (absolute)
                    CPX(bus.getData(getAbsAddr()))
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0xED -> {  //SBC   (absolute)
                    SBC(bus.getData(getAbsAddr()))
                    cycles = 4
                    PC = (PC + 3).ushort
                }
                0xEE -> {  //INC   (absolute)
                    bus.setData(INC(bus.getData(getAbsAddr())), getAbsAddr())
                    cycles = 6
                    PC = (PC + 3).ushort
                }
                0xF0 -> {  //BEQ   (relative)
                    if (checkFlag(EFlag.ZER)) {
                        val newaddr: UShort = getRelAddr()
                        cycles = if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3
                        PC = newaddr
                    } else {
                        cycles = 2
                        PC = (PC + 2).ushort
                    }
                }
                0xF1 -> {  //SBC   (indirect, Y)
                    SBC(bus.getData(getIndYAddr({ wrap = it })))
                    cycles = if(wrap) 6 else 5
                    PC = (PC + 2).ushort
                }
                0xF5 -> {  //SBC   (zeropage, X)
                    SBC(bus.getData(getZPXAddr()))
                    cycles = 4
                    PC = (PC + 2).ushort
                }
                0xF6 -> {  //INC   (zeropage, X)
                    bus.setData(INC(bus.getData(getZPXAddr())), getZPXAddr())
                    cycles = 6
                    PC = (PC + 2).ushort
                }
                0xF8 -> {  //SED
                    setFlag(EFlag.DEC, true)
                    cycles = 2
                    PC = (PC + 1).ushort
                }
                0xF9 -> {  //SBC   (absolute, Y)
                    SBC(bus.getData(getAbsYAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0xFD -> {  //SBC   (absolute, X)
                    SBC(bus.getData(getAbsXAddr({ wrap = it })))
                    cycles = if(wrap) 5 else 4
                    PC = (PC + 3).ushort
                }
                0xFE -> {  //INC   (absolute, X)
                    bus.setData(INC(bus.getData(getAbsXAddr({ wrap = it }))), getAbsXAddr({ wrap = it }))
                    cycles = 7
                    PC = (PC + 3).ushort
                }
                else -> // Any other opcode
                    throw IllegalArgumentException("Ungültiger Opcode")
            }
        }
        else { cycles-- }
    }

    fun step() {
        exec()
        while (cycles > 0) exec()
    }
    //endregion
}