package me.emu6502.emulator

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class DirectBitmap(val width: Int, val height: Int) {

    val image: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    var updateCounter = 0L

    operator fun get(x: Int, y: Int) = Color(image.getRGB(x, y))
    operator fun set(x: Int, y: Int, color: Color) {
        if(x < 0 || y < 0 || x >= width || y >= height)
            return // Ignore setting out of bound coordinates
        image.setRGB(x, y, color.rgb)
        updateCounter++
    }

    fun createScaled(width: Int, height: Int, scaling: Int = BufferedImage.SCALE_FAST): BufferedImage {
        val scaledImg = image.getScaledInstance(width, height, scaling)
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = bi.createGraphics()
        g2d.drawImage(scaledImg, null, null)
        g2d.dispose()
        return bi
    }

    @Throws(IOException::class)
    fun save(fileName: String) = ImageIO.write(image, "bmp", File(fileName))
}