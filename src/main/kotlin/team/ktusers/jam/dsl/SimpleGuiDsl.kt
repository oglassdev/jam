package team.ktusers.jam.dsl

import net.kyori.adventure.text.Component
import net.minestom.server.inventory.InventoryType
import team.ktusers.jam.util.SimpleGui

inline fun simpleGui(
    inventoryType: InventoryType,
    title: Component,
    block: SimpleGui.() -> Unit
): SimpleGui = SimpleGui(inventoryType, title).apply(block)
