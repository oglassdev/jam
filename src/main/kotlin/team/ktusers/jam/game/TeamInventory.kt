package team.ktusers.jam.game

import team.ktusers.jam.generated.Palette
import java.awt.Color

class TeamInventory {
    val collectedColors: MutableSet<String> = mutableSetOf("RED", "INDIGO", "GREY")

    fun isCollected(color: Color): Boolean = collectedColors.contains(getPaletteColorAsString(color))

    fun collectPaletteColor(color: Color) {
        collectedColors.add(getPaletteColorAsString(color))
    }
}

fun getPaletteColorFromString(string: String): Color =
    when (string) {
        "RED" -> Palette.RED
        "ORANGE" -> Palette.ORANGE
        "YELLOW" -> Palette.YELLOW
        "GREEN" -> Palette.GREEN
        "BLUE" -> Palette.BLUE
        "INDIGO" -> Palette.INDIGO
        "VIOLET" -> Palette.VIOLET
        "GREY" -> Palette.GREY
        else -> throw IllegalArgumentException("Invalid color was found")
    }

fun getPaletteColorAsString(color: Color): String =
    when (color) {
        Palette.RED -> "RED"
        Palette.ORANGE -> "ORANGE"
        Palette.YELLOW -> "YELLOW"
        Palette.GREEN -> "GREEN"
        Palette.BLUE -> "BLUE"
        Palette.INDIGO -> "INDIGO"
        Palette.VIOLET -> "VIOLET"
        Palette.GREY -> "GREY"
        else -> throw IllegalArgumentException("Invalid color was found")
    }