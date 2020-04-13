package me.emu6502.emulator

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class DirectBitmap(val width: Int, val height: Int) {

    val image: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    operator fun get(x: Int, y: Int) = Color(image.getRGB(x, y)) // Should also do alpha
    operator fun set(x: Int, y: Int, color: Color) = image.setRGB(x, y, color.rgb)

    @Throws(IOException::class)
    fun save(fileName: String) = ImageIO.write(image, "bmp", File(fileName))
}