package team.ktusers.jam.game

import team.ktusers.jam.generated.PaletteColor

class TeamInventory {
    val colors: MutableSet<PaletteColor> = mutableSetOf()

    var blueFragments: Int = 0
    
    var orangeFragments: Int = 0
}