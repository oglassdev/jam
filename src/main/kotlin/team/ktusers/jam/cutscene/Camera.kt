package team.ktusers.jam.cutscene

import net.bladehunt.kotstom.extension.editMeta
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta

class Camera : Entity(EntityType.TEXT_DISPLAY) {
    init {
        editMeta<TextDisplayMeta> {
            this.text = Component.empty()
            setNoGravity(true)
            this.transformationInterpolationStartDelta = 0
            this
        }
    }
}