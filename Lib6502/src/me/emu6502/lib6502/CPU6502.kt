package me.emu6502.lib6502

import BitConverter
import plus
import shl
import shr
import toString
import ubyte
import uint
import ushort
import java.lang.IllegalArgumentException
import kotlin.math.log2

class CPU6502(val Bus: Bus) {

    //will not (yet) support decimal mode

    var Cycles: UInt = 0.uint

    var PC: UShort = ((Bus.getData(0xFFFD.ushort) shl 8) + Bus.getData(0xFFFC.ushort)).ushort
    var SP: UByte = 0xFF.ubyte
    var A: UByte = 0x00.ubyte
    var X: UByte = 0x00.ubyte
    var Y: UByte = 0x00.ubyte
    var SR: UByte = 0x20.ubyte //N V - - D I Z C

    var NMI: Boolean = false
    var IRQ: Boolean = false


    //region Helpers
    companion object {
        fun CheckBit(value: UByte, bit: Int): Boolean = ((value shl (7 - bit)) shr 7) == 1.ubyte
    }

    override fun toString(): String {
        var output = ""
        output += "A: ${A.toString("X2")}\tX: ${X.toString("X2")}\tY: ${Y.toString("X2")}\tSP: ${SP.toString("X2")}\tPC: ${PC.toString("X4")}"
        output += "\tSR: ${if(CheckFlag(EFlag.NEG)) "N" else "-"}${if(CheckFlag(EFlag.OVR)) "O" else "-"}${if(CheckFlag(EFlag.BRK)) "B" else "-"}-" +
                "${if(CheckFlag(EFlag.DEC)) "D" else "-"}${if(CheckFlag(EFlag.IRQ)) "I" else "-"}${if(CheckFlag(EFlag.ZER)) "Z" else "-"}${if(CheckFlag(EFlag.CAR)) "C" else "-"}\t"
        output += "Instruction: ${DisASM6502.Disassemble(ubyteArrayOf(Bus.getData(PC), Bus.getData((PC + 1.ushort).ushort), Bus.getData((PC + 2.ushort).ushort)), 0)}\n\n"
        return output
    }

    fun Reset() {
        PC = ((Bus.getData(0xFFFD.ushort) shl 8) + Bus.getData(0xFFFC.ushort)).ushort
        SP = 0xFF.ubyte
        A = 0x00.ubyte
        X = 0x00.ubyte
        Y = 0x00.ubyte
        SR = 0x20.ubyte //N V - - D I Z C

        Cycles = 0.uint
        // TODO: Also reset NMI and IRQ?
    }
    //endregion

    //region Status register methods
    private fun CheckFlag(flag: UByte): Boolean = ((SR and flag) shr log2(flag.toDouble()).toInt()) == 1.ubyte

    private fun SetFlag(flag: UByte, state: Boolean)
    {
        if (state)
            SR = SR or flag
        else
            SR = SR and flag.inv()
    }

    private fun TestForCarry(value: UShort): Boolean = (value < 0.ushort) || (value > 255.ushort)

    private fun TestForOverflow(value: UShort): Boolean = (value < (-128).ushort) || (value > 127.ushort)
    //endregion

    //region Stack operations
    private fun PushToStack(value: UByte) {
        Bus.setData(value, (0x0100 + SP).ushort)
        SP--
    }

    private fun PullFromStack(): UByte {
        SP++
        return Bus.getData((0x0100 + SP).ushort)
    }
    //endregion

    //region Interrupt routines
    fun HandleIRQ() {
        PushToStack(BitConverter.GetBytes(PC)[1])
        PushToStack(BitConverter.GetBytes(PC)[0])
        PushToStack(SR)
        SetFlag(EFlag.IRQ, true)
        PC = ((Bus.getData(0xFFFF.ushort) shl 8) + Bus.getData(0xFFFE.ushort)).ushort
        Cycles = 8.uint
    }

    fun HandleNMI() {
        PushToStack(BitConverter.GetBytes(PC)[1])
        PushToStack(BitConverter.GetBytes(PC)[0])
        PushToStack(SR)
        SetFlag(EFlag.IRQ, true)
        NMI = false
        PC = ((Bus.getData(0xFFFB.ushort) shl 8) + Bus.getData(0xFFFA.ushort)).ushort
        Cycles = 8.uint
    }
    //endregion

    //region Addressing modes
    private fun GetRelAddr(): UShort = (PC + (Bus.getData(PC + 1).toByte() + 2.toByte()).ushort).ushort

    private fun GetAbsAddr(): UShort = ((Bus.getData(PC + 2) shl 8) + Bus.getData(PC + 1)).ushort

    private fun GetAbsXAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort{
        onUpdateWrap((Bus.getData((PC + 1)) + X) > 0xFF.ushort)
        return ((Bus.getData(PC + 2) shl 8) + Bus.getData(PC + 1) + X + (if (SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }

    private fun GetAbsYAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort
    {
        onUpdateWrap((Bus.getData(PC + 1) + X).ushort > 0xFF.ushort)
        return ((Bus.getData(PC + 2) shl 8) + Bus.getData(PC + 1) + Y + (if (SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }

    private fun GetZPAddr(): UByte = Bus.getData((PC + 1.ushort).ushort)

    private fun GetZPXAddr(): UByte = (Bus.getData((PC + 1.ushort).ushort) + X.ushort).ubyte

    private fun GetZPYAddr(): UByte = (Bus.getData((PC + 1.ushort).ushort) + Y.ushort).ubyte

    private fun GetIndXAddr(): UShort
    {
        val index = (Bus.getData(PC + 1) + X).ubyte
        return ((Bus.getData(index + 1.ubyte) shl 8) + Bus.getData(index.ushort)).ushort
    }

    private fun GetIndYAddr(onUpdateWrap: (newWrap: Boolean) -> Unit): UShort
    {
        val index = Bus.getData(PC + 1)
        onUpdateWrap(index + Y > 0xFF.ushort)
        return ((Bus.getData(index + 1.ubyte) shl 8) + Bus.getData(index.ushort) + Y + ( if(SR and EFlag.CAR == 1.ubyte) 1 else 0)).ushort
    }
    //endregion

    //region Operations with multiple addressing modes (except store operations)
    private fun ADC(value: UByte): Unit {
        val sum = (A + value + if (CheckFlag(EFlag.CAR)) 1 else 0).ushort
        SetFlag(EFlag.OVR, CheckBit(A, 7) === CheckBit(value, 7) && CheckBit(A, 7) !== CheckBit(sum.ubyte, 7))
        SetFlag(EFlag.CAR, sum > 255.ushort || sum < 0.ushort)
        SetFlag(EFlag.NEG, CheckBit(sum.ubyte, 7))
        SetFlag(EFlag.ZER, sum.toInt() == 0)
        A = sum.ubyte
    }

    private fun AND(value: UByte) {
        A = A and value
        SetFlag(EFlag.ZER, A == 0.ubyte)
        SetFlag(EFlag.NEG, CheckBit(A, 7))
    }

    private fun ASL(value: UByte): UByte {
        var value = value
        SetFlag(EFlag.CAR, CheckBit(value, 7))
        value = value shl 1
        SetFlag(EFlag.NEG, CheckBit(value, 7))
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun BIT(value: UByte) {
        val temp = A and value
        SetFlag(EFlag.ZER, temp == 0.ubyte)
        SetFlag(EFlag.OVR, CheckBit(temp.toUByte(), 6))
        SetFlag(EFlag.NEG, CheckBit(temp.toUByte(), 7))
    }

    private fun CMP(value: UByte) {
        when {
            A < value.toUInt() -> {
                SetFlag(EFlag.NEG, CheckBit((A - value).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, false)
            }
            A == value -> {
                SetFlag(EFlag.NEG, false)
                SetFlag(EFlag.ZER, true)
                SetFlag(EFlag.CAR, true)
            }
            else -> {
                SetFlag(EFlag.NEG, CheckBit((A - value).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, true)
            }
        }
    }

    private fun CPX(value: UByte) {
        when {
            X < value.toUInt() -> {
                SetFlag(EFlag.NEG, CheckBit((X - value).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, false)
            }
            X == value -> {
                SetFlag(EFlag.NEG, false)
                SetFlag(EFlag.ZER, true)
                SetFlag(EFlag.CAR, true)
            }
            else -> {
                SetFlag(EFlag.NEG, CheckBit((X - value).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, true)
            }
        }
    }

    private fun CPY(value: UByte) {
        when {
            Y < value.toUInt() -> {
                SetFlag(EFlag.NEG, CheckBit((Y - value.toUInt()).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, false)
            }
            Y == value -> {
                SetFlag(EFlag.NEG, false)
                SetFlag(EFlag.ZER, true)
                SetFlag(EFlag.CAR, true)
            }
            else -> {
                SetFlag(EFlag.NEG, CheckBit((Y - value).ubyte, 7))
                SetFlag(EFlag.ZER, false)
                SetFlag(EFlag.CAR, true)
            }
        }
    }

    private fun DEC(value: UByte): UByte {
        var value = value
        value--
        SetFlag(EFlag.NEG, CheckBit(value, 7))
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun EOR(value: UByte) {
        A = A xor value
        SetFlag(EFlag.NEG, CheckBit(A, 7))
        SetFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun INC(value: UByte): UByte {
        var value = value
        value++
        SetFlag(EFlag.NEG, CheckBit(value, 7))
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun JMP(address: UShort) {
        PC = address
    }

    private fun LDA(value: UByte) {
        A = value
        SetFlag(EFlag.NEG, CheckBit(A, 7))
        SetFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun LDX(value: UByte) {
        X = value
        SetFlag(EFlag.NEG, CheckBit(X, 7))
        SetFlag(EFlag.ZER, X == 0.ubyte)
    }

    private fun LDY(value: UByte) {
        Y = value
        SetFlag(EFlag.NEG, CheckBit(Y, 7))
        SetFlag(EFlag.ZER, Y == 0.ubyte)
    }

    private fun LSR(value: UByte): UByte {
        var value = value
        SetFlag(EFlag.CAR, CheckBit(value, 0))
        value = value shr 1
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun ORA(value: UByte) {
        A = A or value
        SetFlag(EFlag.NEG, CheckBit(A, 7))
        SetFlag(EFlag.ZER, A == 0.ubyte)
    }

    private fun ROL(value: UByte): UByte {
        var value = value
        val carrytemp = CheckFlag(EFlag.CAR)
        SetFlag(EFlag.CAR, CheckBit(value, 7))
        value = value shl 1
        if (carrytemp) value++
        SetFlag(EFlag.NEG, CheckBit(value, 7))
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun ROR(value: UByte): UByte {
        var value = value
        val carrytemp = CheckFlag(EFlag.CAR)
        SetFlag(EFlag.CAR, CheckBit(value, 0))
        value = value shr 1
        if (carrytemp) value = (value + (1 shl 7).ubyte).ubyte
        SetFlag(EFlag.NEG, CheckBit(value, 7))
        SetFlag(EFlag.ZER, value == 0.ubyte)
        return value
    }

    private fun SBC(value: UByte) = ADC(value.inv())
    //endregion

    //region Execution
    //region Execution
    fun Exec() {
        if (!CheckFlag(EFlag.IRQ) && IRQ)
            HandleIRQ()
        if (NMI)
            HandleNMI()

        if (Cycles == 0.uint) {
            val instruction = Bus.getData(PC)
            var wrap = false

            when (instruction.toInt()) {
                0x00 -> { //BRK
                    Cycles = 7.uint
                    PC = (PC + 2).ushort
                    PushToStack(BitConverter.GetBytes(PC)[1])
                    PushToStack(BitConverter.GetBytes(PC)[0])
                    PushToStack(SR)
                    SetFlag(EFlag.BRK, true)
                    SetFlag(EFlag.IRQ, true)
                    PC = ((Bus.getData(0xFFFF.ushort) shl 8) + Bus.getData(0xFFFE.ushort)).ushort
                }
                0x01 -> {  //ORA   (indirect, X)
                    Cycles = 6.uint
                    ORA(Bus.getData(GetIndXAddr()))
                    PC = (PC + 2).ushort
                }
                0x05 -> {  //ORA   (zeropage)
                    Cycles = 3.uint
                    ORA(Bus.getData(GetZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x06 -> {  //ASL   (zeropage)
                    Cycles = 5.uint
                    Bus.setData(ASL(Bus.getData(GetZPAddr())), GetZPAddr())
                    PC = (PC + 2).ushort
                }
                0x08 -> {  //PHP
                    Cycles = 3.uint
                    PushToStack(SR)
                    PC = (PC + 1).ushort
                }
                0x09 -> {  //ORA   (immediate)
                    Cycles = 2.uint
                    ORA(Bus.getData(PC + 1))
                    PC = (PC + 2).ushort
                }
                0x0A -> {  //ASL   (accumulator)
                    Cycles = 2.uint
                    A = ASL(A)
                    PC = (PC + 1).ushort
                }
                0x0D -> {  //ORA   (absolute)
                    Cycles = 4.uint
                    ORA(Bus.getData(GetAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x0E -> {  //ASL   (absolute)
                    Cycles = 6.uint
                    Bus.setData(ASL(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    PC = (PC + 3).ushort
                }
                0x10 -> {  //BPL   (relative)
                    if (CheckFlag(EFlag.NEG)) {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    }
                }
                0x11 -> {  //ORA   (indirect, Y)
                    ORA(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if (wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0x15 -> {  //ORA   (zeropage, X)
                    ORA(Bus.getData(GetZPXAddr()))
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x16 -> {  //ASL   (zeropage, X)
                    Cycles = 6.uint
                    Bus.setData(ASL(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    PC = (PC + 2).ushort
                }
                0x18 -> {  //CLC
                    Cycles = 2.uint
                    SetFlag(EFlag.CAR, false)
                    PC = (PC + 1).ushort
                }
                0x19 -> {  //ORA   (absolute, Y)
                    ORA(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = if (wrap) 5.uint else 4.uint
                    PC = (PC + 3).ushort
                }
                0x1D -> {  //ORA   (absolute, X)
                    ORA(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if (wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x1E -> {  //ASL   (absolute, X)
                    Cycles = 7.uint
                    Bus.setData(ASL(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    PC = (PC + 3).ushort
                }
                0x20 -> {  //JSR   (absolute)
                    Cycles = 6.uint
                    PushToStack(BitConverter.GetBytes((PC + 2).ushort)[1])
                    PushToStack(BitConverter.GetBytes((PC + 2).ushort)[0])
                    PC = GetAbsAddr()
                }
                0x21 -> {  //AND   (indirect, X)
                    Cycles = 6.uint
                    AND(Bus.getData(GetIndXAddr()))
                    PC = (PC + 2).ushort
                }
                0x24 -> {  //BIT   (zeropage)
                    Cycles = 3.uint
                    BIT(Bus.getData(GetZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x25 -> {  //AND   (zeropage)
                    Cycles = 3.uint
                    AND(Bus.getData(GetZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x26 -> {  //ROL   (zeropage)
                    Cycles = 5.uint
                    ROL(Bus.getData(GetZPAddr()))
                    PC = (PC + 2).ushort
                }
                0x28 -> {  //PLP
                    SR = PullFromStack()
                    PC = (PC + 1).ushort
                }
                0x29 -> {  //AND   (immediate)
                    Cycles = 2.uint
                    AND(Bus.getData(PC + 1))
                    PC = (PC + 2).ushort
                }
                0x2A -> {  //ROL   (accumulator)
                    Cycles = 2.uint
                    A = ROL(A)
                    PC = (PC + 1).ushort
                }
                0x2C -> {  //BIT   (absolute)
                    Cycles = 4.uint
                    BIT(Bus.getData(GetAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x2D -> {  //AND   (absolute)
                    Cycles = 4.uint
                    AND(Bus.getData(GetAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x2E -> {  //ROL   (absolute)
                    Cycles = 6.uint
                    Bus.setData(ROL(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    PC = (PC + 3).ushort
                }
                0x30 -> {  //BMI   (relative)
                    if (CheckFlag(EFlag.NEG)) {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    } else {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    }
                }
                0x31 -> {  //AND   (indirect, Y)
                    AND(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if(wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0x35 -> {  //AND   (zeropage, X)
                    Cycles = 4.uint
                    AND(Bus.getData(GetZPXAddr()))
                    PC = (PC + 2).ushort
                }
                0x36 -> {  //ROL   (zeropage, x)
                    Bus.setData(ROL(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x38 -> {  //SEC
                    Cycles = 2.uint
                    SetFlag(EFlag.CAR, true)
                    PC = (PC + 1).ushort
                }
                0x39 -> {  //AND   (absolute, Y)
                    AND(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x3D -> {  //AND   (absolute, X)
                    AND(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x3E -> {  //ROL   (absolute, X)
                    Bus.setData(ROL(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    Cycles = 7.uint
                    PC = (PC + 3).ushort
                }
                0x40 -> {  //RTI
                    Cycles = 6.uint
                    SR = PullFromStack()
                    PC = (PullFromStack() + (PullFromStack() shl 8)).ushort
                }
                0x41 -> {  //EOR   (indirect, Y)
                    EOR(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = if(wrap) 6.uint else 5.uint
                    PC = (PC + 2).ushort
                }
                0x45 -> {  //EOR   (zeropage)
                    EOR(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0x46 -> {  //LSR   (zeropage)
                    Bus.setData(LSR(Bus.getData(GetZPAddr())), GetZPAddr())
                    Cycles = 5.uint
                    PC = (PC + 2).ushort
                }
                0x48 -> {  //PHA
                    Cycles = 3.uint
                    PushToStack(A)
                    PC = (PC + 1).ushort
                }
                0x49 -> {  //EOR   (immediate)
                    EOR(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0x4A -> {  //LSR   (accumulator)
                    A = LSR(A)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x4C -> {  //JMP   (absolute)
                    Cycles = 3.uint
                    JMP(GetAbsAddr())
                }
                0x4D -> {  //EOR   (absolute)
                    Cycles = 4.uint
                    EOR(Bus.getData(GetAbsAddr()))
                    PC = (PC + 3).ushort
                }
                0x4E -> {  //LSR   (absolute)
                    Bus.setData(LSR(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    Cycles = 6.uint
                    PC = (PC + 3).ushort
                }
                0x50 -> {  //BVC   (relative)
                    if (CheckFlag(EFlag.OVR)) {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    }
                }
                0x51 -> {  //EOR   (indirect, Y)
                    EOR(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if(wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0x55 -> {  //EOR   (zeropage, X)
                    EOR(Bus.getData(GetZPXAddr()))
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x56 -> {  //LSR   (zeropage, X)
                    Bus.setData(LSR(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x58 -> {  //CLI
                    SetFlag(EFlag.IRQ, false)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x59 -> {  //EOR   (absolute, Y)
                    EOR(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x5D -> {  //EOR   (absolute, X)
                    EOR(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x5E -> {  //LSR   (absolute, X)
                    Bus.setData(LSR(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    Cycles = 7.uint
                    PC = (PC + 3).ushort
                }
                0x60 -> {  //RTS
                    Cycles = 6.uint
                    PC = (PullFromStack() + (PullFromStack() shl 8)).ushort
                    PC = (PC + 1).ushort
                }
                0x61 -> {  //ADC   (indirect, X)
                    ADC(Bus.getData(GetIndXAddr()))
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x65 -> {  //ADC   (zeropage)
                    ADC(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0x66 -> {  //ROR   (zeropage)
                    Bus.setData(ROR(Bus.getData(GetZPAddr())), GetZPAddr())
                    Cycles = 5.uint
                    PC = (PC + 2).ushort
                }
                0x68 -> {  //PLA
                    Cycles = 4.uint
                    A = PullFromStack()
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0x69 -> {  //ADC   (immediate)
                    ADC(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0x6A -> {  //ROR   (accumulator)
                    A = ROR(A)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x6C -> {  //JMP   (indirect)
                    PC = ((Bus.getData(GetAbsAddr()) shl 8) + Bus.getData((GetAbsAddr() + 1).ushort)).ushort
                    Cycles = 5.uint
                }
                0x6D -> {  //ADC   (absolute)
                    ADC(Bus.getData(GetAbsAddr()))
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0x6E -> {  //ROR   (absolute)
                    Bus.setData(ROR(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    Cycles = 6.uint
                    PC = (PC + 3).ushort
                }
                0x70 -> {  //BVS   (relative)
                    if (CheckFlag(EFlag.OVR)) {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    } else {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    }
                }
                0x71 -> {  //ADC   (indirect, Y)
                    ADC(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if(wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0x75 -> {  //ADC   (zeropage, X)
                    ADC(Bus.getData(GetZPXAddr()))
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x76 -> {  //ROR   (zeropage, X)
                    Bus.setData(ROR(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x78 -> {  //SEI
                    SetFlag(EFlag.IRQ, true)
                    PC = (PC + 1).ushort
                    Cycles = 2.uint
                }
                0x79 -> {  //ADC   (absolute, Y)
                    ADC(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x7D -> {  //ADC   (absolute, X)
                    ADC(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0x7E -> {  //ROR   (absolute, X)
                    Bus.setData(ROR(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    Cycles = 7.uint
                    PC = (PC + 3).ushort
                }
                0x81 -> {  //STA   (indirect, X)
                    Bus.setData(A, GetIndXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x84 -> {  //STY   (zeropage)
                    Bus.setData(Y, GetZPAddr())
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0x85 -> {  //STA   (zeropage)
                    Bus.setData(A, GetZPAddr())
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0x86 -> {  //STX   (zeropage)
                    Bus.setData(X, GetZPAddr())
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0x88 -> {  //DEY
                    Y--
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x8A -> {  //TXA
                    A = X
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x8C -> {  //STY   (absolute)
                    Bus.setData(Y, GetAbsAddr())
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0x8D -> {  //STA   (absolute)
                    Bus.setData(A, GetAbsAddr())
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0x8E -> {  //STX   (absolute)
                    Bus.setData(X, GetAbsAddr())
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0x90 -> {  //BCC   (relative)
                    if (CheckFlag(EFlag.CAR)) {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    }
                }
                0x91 -> {  //STA   (indirect, Y)
                    Bus.setData(A, GetIndYAddr({ wrap = it }))
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0x94 -> {  //STY   (zeropage, X)
                    Bus.setData(Y, GetZPXAddr())
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x95 -> {  //STA   (zeropage, X)
                    Bus.setData(A, GetZPXAddr())
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x96 -> {  //STX   (zeropage, Y)
                    Bus.setData(X, GetZPYAddr())
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0x98 -> {  //TYA
                    A = Y
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0x99 -> {  //STA   (absolute, Y)
                    Bus.setData(A, GetAbsYAddr({ wrap = it }))
                    Cycles = 5.uint
                    PC = (PC + 3).ushort
                }
                0x9A -> {  //TXS
                    SP = X
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0x9D -> {  //STA   (absolute, X)
                    Bus.setData(A, GetAbsXAddr({ wrap = it }))
                    Cycles = 5.uint
                    PC = (PC + 3).ushort
                }
                0xA0 -> {  //LDY   (immediate)
                    Y = Bus.getData(PC + 1)
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA1 -> {  //LDA   (indirect, X)
                    A = Bus.getData(GetIndXAddr())
                    Cycles = 6.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA2 -> {  //LDX   (immediate)
                    X = Bus.getData(PC + 1)
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA4 -> {  //LDY   (zeropage)
                    Y = Bus.getData(GetZPAddr())
                    Cycles = 3.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA5 -> {  //LDA   (zeropage)
                    A = Bus.getData(GetZPAddr())
                    Cycles = 3.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA6 -> {  //LDX   (zeropage)
                    X = Bus.getData(GetZPAddr())
                    Cycles = 3.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xA8 -> {  //TAY
                    Y = A
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xA9 -> {  //LDA   (immediate)
                    A = Bus.getData(PC + 1)
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xAA -> {  //TAX
                    X = A
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xAC -> {  //LDY   (absolute)
                    Y = Bus.getData(GetAbsAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xAD -> {  //LDA   (absolute)
                    A = Bus.getData(GetAbsAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xAE -> {  //LDX   (absolute)
                    X = Bus.getData(GetAbsAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xB0 -> {  //BCS   (relative)
                    if (CheckFlag(EFlag.CAR)) {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    } else {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    }
                }
                0xB1 -> {  //LDA   (indirect, Y)
                    A = Bus.getData(GetIndYAddr({ wrap = it }))
                    Cycles = (if(wrap) 6 else 5).uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB4 -> {  //LDY   (zeropage, X)
                    Y = Bus.getData(GetZPXAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB5 -> {  //LDA   (zeropage, X)
                    A = Bus.getData(GetZPXAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB6 -> {  //LDX   (zeropage, Y)
                    X = Bus.getData(GetZPYAddr())
                    Cycles = 4.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 2).ushort
                }
                0xB8 -> {  //CLV
                    SetFlag(EFlag.OVR, false)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0xB9 -> {  //LDA   (absolute, Y)
                    A = Bus.getData(GetAbsYAddr({ wrap = it }))
                    Cycles = (if(wrap) 5 else 4).uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBA -> {  //TSX
                    X = SP
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xBC -> {  //LDY   (absolute, X)
                    Y = Bus.getData(GetAbsXAddr({ wrap = it }))
                    Cycles = (if(wrap) 5 else 4).uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBD -> {  //LDA   (absolute, X)
                    A = Bus.getData(GetAbsXAddr({ wrap = it }))
                    Cycles = (if(wrap) 5 else 4).uint
                    SetFlag(EFlag.NEG, CheckBit(A, 7))
                    SetFlag(EFlag.ZER, A == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xBE -> {  //LDX   (absolute, Y)
                    X = Bus.getData(GetAbsYAddr({ wrap = it }))
                    Cycles = (if(wrap) 5 else 4).uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 3).ushort
                }
                0xC0 -> {  //CPY   (immediate)
                    CPY(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0xC1 -> {  //CMP   (indirect, X)
                    CMP(Bus.getData(GetIndXAddr()))
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0xC4 -> {  //CPY   (zeropage)
                    CPY(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0xC5 -> {  //CMP   (zeropage)
                    CMP(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0xC6 -> {  //DEC   (zeropage)
                    Bus.setData(DEC(Bus.getData(GetZPAddr())), GetZPAddr())
                    Cycles = 5.uint
                    PC = (PC + 2).ushort
                }
                0xC8 -> {  //INY
                    Y++
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(Y, 7))
                    SetFlag(EFlag.ZER, Y == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xC9 -> {  //CMP   (immediate)
                    CMP(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0xCA -> {  //DEX
                    X--
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xCC -> {  //CPY   (absolute)
                    CPY(Bus.getData(GetAbsAddr()))
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0xCD -> {  //CMP   (absolute)
                    CMP(Bus.getData(GetAbsAddr()))
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0xCE -> {  //DEC   (absolute)
                    Bus.setData(DEC(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    Cycles = 6.uint
                    PC = (PC + 3).ushort
                }
                0xD0 -> {  //BNE   (relative)
                    if (CheckFlag(EFlag.ZER)) {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    } else {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    }
                }
                0xD1 -> {  //CMP   (indirect, Y)
                    CMP(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if(wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0xD5 -> {  //CMP   (zeropage, X)
                    CMP(Bus.getData(GetZPXAddr()))
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0xD6 -> {  //DEC   (zeropage, X)
                    Bus.setData(DEC(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0xD8 -> {  //CLD
                    SetFlag(EFlag.DEC, false)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0xD9 -> {  //CMP   (absolute, Y)
                    CMP(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0xDD -> {  //CMP   (absolute, X)
                    CMP(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0xDE -> {  //DEC   (absolute, X)
                    Bus.setData(DEC(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    Cycles = 7.uint
                    PC = (PC + 3).ushort
                }
                0xE0 -> {  //CPX   (immediate)
                    CPX(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0xE1 -> {  //SBC   (indirect, X)
                    SBC(Bus.getData(GetIndXAddr()))
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0xE4 -> {  //CPX   (zeropage)
                    CPX(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0xE5 -> {  //SBC   (zeropage)
                    SBC(Bus.getData(GetZPAddr()))
                    Cycles = 3.uint
                    PC = (PC + 2).ushort
                }
                0xE6 -> {  //INC   (zeropage)
                    Bus.setData(INC(Bus.getData(GetZPAddr())), GetZPAddr())
                    Cycles = 5.uint
                    PC = (PC + 2).ushort
                }
                0xE8 -> {  //INX
                    X++
                    Cycles = 2.uint
                    SetFlag(EFlag.NEG, CheckBit(X, 7))
                    SetFlag(EFlag.ZER, X == 0.ubyte)
                    PC = (PC + 1).ushort
                }
                0xE9 -> {  //SBC   (immediate)
                    SBC(Bus.getData(PC + 1))
                    Cycles = 2.uint
                    PC = (PC + 2).ushort
                }
                0xEA -> {  //NOP
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0xEC -> {  //CPX   (absolute)
                    CPX(Bus.getData(GetAbsAddr()))
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0xED -> {  //SBC   (absolute)
                    SBC(Bus.getData(GetAbsAddr()))
                    Cycles = 4.uint
                    PC = (PC + 3).ushort
                }
                0xEE -> {  //INC   (absolute)
                    Bus.setData(INC(Bus.getData(GetAbsAddr())), GetAbsAddr())
                    Cycles = 6.uint
                    PC = (PC + 3).ushort
                }
                0xF0 -> {  //BEQ   (relative)
                    if (CheckFlag(EFlag.ZER)) {
                        val newaddr: UShort = GetRelAddr()
                        Cycles = (if(BitConverter.GetBytes(newaddr)[1] != BitConverter.GetBytes(PC)[1]) 4 else 3).uint
                        PC = newaddr
                    } else {
                        Cycles = 2.uint
                        PC = (PC + 2).ushort
                    }
                }
                0xF1 -> {  //SBC   (indirect, Y)
                    SBC(Bus.getData(GetIndYAddr({ wrap = it })))
                    Cycles = (if(wrap) 6 else 5).uint
                    PC = (PC + 2).ushort
                }
                0xF5 -> {  //SBC   (zeropage, X)
                    SBC(Bus.getData(GetZPXAddr()))
                    Cycles = 4.uint
                    PC = (PC + 2).ushort
                }
                0xF6 -> {  //INC   (zeropage, X)
                    Bus.setData(INC(Bus.getData(GetZPXAddr())), GetZPXAddr())
                    Cycles = 6.uint
                    PC = (PC + 2).ushort
                }
                0xF8 -> {  //SED
                    SetFlag(EFlag.DEC, true)
                    Cycles = 2.uint
                    PC = (PC + 1).ushort
                }
                0xF9 -> {  //SBC   (absolute, Y)
                    SBC(Bus.getData(GetAbsYAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0xFD -> {  //SBC   (absolute, X)
                    SBC(Bus.getData(GetAbsXAddr({ wrap = it })))
                    Cycles = (if(wrap) 5 else 4).uint
                    PC = (PC + 3).ushort
                }
                0xFE -> {  //INC   (absolute, X)
                    Bus.setData(INC(Bus.getData(GetAbsXAddr({ wrap = it }))), GetAbsXAddr({ wrap = it }))
                    Cycles = 7.uint
                    PC = (PC + 3).ushort
                }
                else -> // Any other opcode
                    throw IllegalArgumentException("Ungültiger Opcode")
            }
        }
        else { Cycles-- }
    }
    //endregion
}