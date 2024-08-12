package team.ktusers.jam.game

import team.ktusers.jam.generated.PaletteColor

class TeamInventory {
    private val collectedColors: MutableSet<PaletteColor> = mutableSetOf()

    fun isCollected(color: PaletteColor): Boolean = collectedColors.contains(color)

    fun collectPaletteColor(color: PaletteColor) {
        collectedColors.add(color)
    }
}