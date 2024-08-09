package team.ktusers.jam.item

import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import net.minestom.server.tag.TagSerializer
import team.ktusers.jam.AdventureNbt

val ItemTag = Tag.Structure("custom_item", TagSerializer.COMPOUND)

fun ItemStack.getCustomItemData(): JamItem? = this
    .getTag(ItemTag)
    ?.let { AdventureNbt.decodeFromCompound<JamItem>(it) }