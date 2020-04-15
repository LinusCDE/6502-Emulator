package me.emu6502.kotlinutils

class Triple<F, S, T>(val first: F, val second: S, val third: T) {

    operator fun component1() = first
    operator fun component2() = second
    operator fun component3() = third

}