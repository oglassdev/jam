package team.ktusers.jam.util

import net.bladehunt.kotstom.extension.editMeta
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.PlayerMeta
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket

class PlayerNpc(username: String, skin: PlayerSkin? = null) : LivingEntity(EntityType.PLAYER) {
    init {
        this.editMeta<PlayerMeta> {
            setDisplayedSkinParts(127.toByte())
            this
        }
    }

    private val entry: PlayerInfoUpdatePacket.Entry =
        PlayerInfoUpdatePacket.Entry(
            this.uuid,
            username,
            if (skin != null)
                listOf(
                    PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()))
            else emptyList(),
            false,
            0,
            GameMode.SURVIVAL,
            null,
            null)

    override fun updateNewViewer(player: Player) {
        player.sendPacket(PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry))

        super.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)

        player.sendPacket(PlayerInfoRemovePacket(this.uuid))
    }
}
