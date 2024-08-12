package team.ktusers.gen

import com.github.ajalt.colormath.model.RGB

object Palette {
    val RED = RGB.from255(255, 0, 0).toOklab()
    val ORANGE = RGB.from255(255, 100, 0).toOklab()
    val YELLOW = RGB.from255(255, 200, 50).toOklab()
    val GREEN = RGB.from255(0, 255, 0).toOklab()
    val BLUE = RGB.from255(0, 25, 255).toOklab()
    val INDIGO = RGB.from255(75, 0, 130).toOklab()
    val VIOLET = RGB.from255(143, 0, 255).toOklab()
    val GREY = RGB.from255(100, 100, 100).toOklab()
    val BLACK = RGB.from255(25, 24, 28).toOklab()
}