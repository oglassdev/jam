package team.ktusers.jam.util

import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.rowSize
import net.bladehunt.kotstom.extension.set
import net.bladehunt.kotstom.util.EventNodeInventory
import net.kyori.adventure.text.Component
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import kotlin.collections.set

class SimpleGui(inventoryType: InventoryType, title: Component) :
    EventNodeInventory(inventoryType, title) {
    private val rowSize = inventoryType.rowSize
    private val slotHandlers: MutableMap<Int, (InventoryPreClickEvent) -> Unit> = hashMapOf()

    init {
        eventNode().listen<InventoryPreClickEvent> { event ->
            event.isCancelled = true
            slotHandlers[event.slot]?.invoke(event)
        }
    }

    fun remove(slot: Int) {
        this[slot] = ItemStack.AIR
        slotHandlers.remove(slot)
    }

    fun getHandler(slot: Int): ((InventoryPreClickEvent) -> Unit)? = slotHandlers[slot]

    fun set(
        slot: Int,
        itemStack: ItemStack,
        onPreClick: ((InventoryPreClickEvent) -> Unit)? = null
    ) {
        this[slot] = itemStack

        if (onPreClick == null) return
        slotHandlers[slot] = onPreClick
    }

    fun at(x: Int, y: Int): Int = rowSize * y + x
}
