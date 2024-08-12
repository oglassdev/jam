// GENERATED!!! DO NOT EDIT
package team.ktusers.jam.generated

import java.awt.Color
import kotlin.Int
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.format.TextColor

@Serializable
public enum class PaletteColor(
  public val color: Color,
  public val textColor: TextColor,
) {
  RED(0xff0000),
  ORANGE(0xff6400),
  YELLOW(0xffc832),
  GREEN(0x00ff00),
  BLUE(0x0019ff),
  INDIGO(0x4b0082),
  VIOLET(0x8f00ff),
  GREY(0x646464),
  BLACK(0x19181c),
  NONE(0x000000),
  ;

  private constructor(colorInt: Int) : this(Color(colorInt), TextColor.color(colorInt))

  public companion object {
    public fun fromColor(color: Color): PaletteColor? = entries.firstOrNull { it.color == color }
  }
}
