package me.emu6502.emulator.ui.view

import javafx.animation.AnimationTimer
import javafx.scene.canvas.Canvas
import me.emu6502.kotlinutils.toString
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.Text
import me.emu6502.emulator.BasicVT100Engine
import me.emu6502.kotlinutils.vt100.VT100Attribute
import me.emu6502.kotlinutils.vt100.VT100BackgroundColor
import me.emu6502.kotlinutils.vt100.VT100ForegroundColor
import tornadofx.onChange

class BasicVT100View(val cols: Int, val rows: Int): Canvas() {

    private var engine = BasicVT100Engine(cols, rows)

    enum class TermColor(defaultH: Float, defaultS: Float, defaultV: Float,
                      dimH: Float, dimS: Float, dimV: Float,
                      brightH: Float, brightS: Float, brightV: Float) {

        BLACK(0f, 0f, 15f, 0f, 0f, 1f, 0f, 0f, 28f),
        RED(0f, 94f, 67f, 0f, 94f, 42f, 0f, 94f, 75f),
        GREEN(134f, 94f, 67f, 134f, 94f, 42f, 134f, 94f, 75f),
        YELLOW(54f, 94f, 67f, 54f, 94f, 42f, 54f, 94f, 75f),
        BLUE(218f, 94f, 67f, 218f, 94f, 42f, 218f, 94f, 75f),
        MAGENTA(294f, 94f, 67f, 294f, 94f, 42f, 294f, 94f, 75f),
        CYAN(182f, 94f, 67f, 182f, 94f, 42f, 182f, 94f, 75f),
        WHITE(0f, 0f, 85f, 0f, 0f, 66f, 0f, 0f, 99f);

        val default = java.awt.Color.getHSBColor(defaultH / 360, defaultS / 100, defaultV / 100)
        val dim = java.awt.Color.getHSBColor(dimH / 360, dimS / 100, dimV / 100)
        val bright = java.awt.Color.getHSBColor(brightH / 360, brightS / 100, brightV / 100)

        val defaultHexCode by lazy { "#" + default.rgb.and(0xFFFFFF).toString("X6") }
        val dimHexCode by lazy { "#" + dim.rgb.and(0xFFFFFF).toString("X6") }
        val brightHexCode by lazy { "#" + bright.rgb.and(0xFFFFFF).toString("X6") }

        val defaultPaint by lazy { Paint.valueOf(defaultHexCode) }
        val dimPaint by lazy { Paint.valueOf(dimHexCode) }
        val brightPaint by lazy { Paint.valueOf(brightHexCode) }
    }

    val colorMap = hashMapOf<VT100Attribute, TermColor>(
            VT100BackgroundColor.BLACK to TermColor.BLACK,
            VT100BackgroundColor.RED to TermColor.RED,
            VT100BackgroundColor.GREEN to TermColor.GREEN,
            VT100BackgroundColor.YELLOW to TermColor.YELLOW,
            VT100BackgroundColor.BLUE to TermColor.BLUE,
            VT100BackgroundColor.MAGENTA to TermColor.MAGENTA,
            VT100BackgroundColor.CYAN to TermColor.CYAN,
            VT100BackgroundColor.WHITE to TermColor.WHITE,
            VT100ForegroundColor.BLACK to TermColor.BLACK,
            VT100ForegroundColor.RED to TermColor.RED,
            VT100ForegroundColor.GREEN to TermColor.GREEN,
            VT100ForegroundColor.YELLOW to TermColor.YELLOW,
            VT100ForegroundColor.BLUE to TermColor.BLUE,
            VT100ForegroundColor.MAGENTA to TermColor.MAGENTA,
            VT100ForegroundColor.CYAN to TermColor.CYAN,
            VT100ForegroundColor.WHITE to TermColor.WHITE
    )

    private val charWidth: Double
    private val charHeight: Double
    private val font = Font.font("monospaced")

    private var doDraw = true // Set to true after some write to the engine occurred. To reduce load

    private val drawTimer: AnimationTimer = object: AnimationTimer() {
        override fun handle(p0: Long) {
            var performDraw: Boolean = false
            synchronized(doDraw) {
               if(doDraw) {
                   performDraw = true
                   doDraw = false
               }
            }

            if(performDraw)
                draw()
        }
    }

    init {
        val t = Text("X")
        t.font = font
        t.applyCss()
        charWidth = t.boundsInLocal.width
        charHeight = t.boundsInLocal.height

        width = charWidth * cols
        height = charHeight * rows

        translateX = 0.0
        translateY = charHeight

        visibleProperty().onChange { visible ->
            if(visible)
                drawTimer.start()
            else
                drawTimer.stop()
        }
        if(isVisible) drawTimer.start()
    }

    fun draw() {
        //val start = System.currentTimeMillis()

        val ctx = graphicsContext2D
        ctx.font = font

        ctx.fill = if(engine.defaultBackground == null) Color.TRANSPARENT else colorMap[engine.defaultBackground!!]!!.defaultPaint
        if(ctx.fill == Color.TRANSPARENT)
            ctx.clearRect(0.0, 0.0, width, height)
        else
            ctx.fillRect(0.0, 0.0, width, height)

        var y = 0.0
        for (absRow in engine.consoleTop until engine.cells.size) {
            var x = 0.0
            val row = absRow - engine.consoleTop
            for (col in 0 until engine.columns) {
                val cell = engine.cells[absRow][col]

                val selectedBg = cell.cellStyle.backgroundColor ?: engine.defaultBackground
                val paintBg = if(selectedBg == null) Color.TRANSPARENT else colorMap[selectedBg]!!.defaultPaint

                val termFg = colorMap[cell.cellStyle.foregroundColor ?: VT100ForegroundColor.BLACK]!!
                val paintFg = when {
                    cell.cellStyle.dim -> termFg.dimPaint
                    cell.cellStyle.bright -> termFg.brightPaint
                    else -> termFg.defaultPaint
                }

                if(paintBg != Color.TRANSPARENT) {
                    ctx.fill = paintBg
                    ctx.fillRect(x, y + 3, charWidth + 0.5, charHeight + 0.5)
                }
                ctx.fill = paintFg
                ctx.fillText((cell.char ?: ' ').toString(), x, y + charHeight)

                x += charWidth
            }
            y += charHeight
        }

        //println("Update-Time: ${System.currentTimeMillis() - start} ms")
    }

    fun write(text: String) {
        engine.write(text)
        synchronized(doDraw) {
            doDraw = true
        }
    }

}