package team.ktusers.jam.util

import net.bladehunt.kotstom.TeamManager
import net.bladehunt.kotstom.extension.editMeta
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.PlayerMeta
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Team

private val NpcTeam =
    TeamManager.createBuilder("npc").nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER).build()

class PlayerNpc(val username: String, val skin: PlayerSkin? = null) :
    LivingEntity(EntityType.PLAYER) {
    class Name(name: Component) : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editMeta<TextDisplayMeta> {
                this.text = name
                setNoGravity(true)
                translation = Vec(0.0, 0.2825, 0.0)
                this.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                scale = Vec(0.9, 0.9, 0.9)
                this
            }
        }
    }

    init {
        this.editMeta<PlayerMeta> {
            setDisplayedSkinParts(127.toByte())
            setNoGravity(true)
            this
        }
        setTeam(NpcTeam)
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

    private var team: Team? = null

    override fun getTeam(): Team? = team

    override fun setTeam(team: Team?) {
        if (this.team == team) return

        if (team == null) {
            this.team?.removeMember(username)
            return
        }
        this.team = team
        team.addMember(username)
    }

    override fun updateNewViewer(player: Player) {
        player.sendPacket(PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry))

        super.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)

        player.sendPacket(PlayerInfoRemovePacket(this.uuid))
    }
}
