package team.ktusers.jam.player

import io.github.togar2.pvp.player.CombatPlayer
import java.util.*
import java.util.function.Function
import net.minestom.server.ServerFlag
import net.minestom.server.collision.PhysicsResult
import net.minestom.server.collision.PhysicsUtils
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.entity.EntityVelocityEvent
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.PotionEffect
import net.minestom.server.utils.chunk.ChunkUtils

class JamPlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) :
    CombatPlayer, Player(uuid, username, playerConnection) {
    private var velocityUpdate = false
    private var previousPhysicsResult: PhysicsResult? = null

    override fun setVelocity(velocity: Vec) {
        val entityVelocityEvent = EntityVelocityEvent(this, velocity)
        EventDispatcher.callCancellable(entityVelocityEvent) {
            this.velocity = entityVelocityEvent.velocity
            this.velocityUpdate = true
        }
    }

    override fun setVelocityNoUpdate(function: Function<Vec?, Vec?>) {
        this.velocity = function.apply(this.velocity)
    }

    override fun sendImmediateVelocityUpdate() {
        if (this.velocityUpdate) {
            this.velocityUpdate = false
            this.sendPacketToViewersAndSelf(this.velocityPacket)
        }
    }

    override fun movementTick() {
        this.gravityTickCount = if (this.onGround) 0 else this.gravityTickCount + 1
        if (this.vehicle == null) {
            val tps = ServerFlag.SERVER_TICKS_PER_SECOND.toDouble()
            var aerodynamics = this.aerodynamics
            if (velocity.y() < 0.0 && this.hasEffect(PotionEffect.SLOW_FALLING)) {
                aerodynamics = aerodynamics.withGravity(0.01)
            }

            val physicsResult =
                PhysicsUtils.simulateMovement(
                    this.position,
                    velocity.div(ServerFlag.SERVER_TICKS_PER_SECOND.toDouble()),
                    this.boundingBox,
                    instance.worldBorder,
                    this.instance,
                    aerodynamics,
                    this.hasNoGravity(),
                    this.hasPhysics,
                    this.onGround,
                    this.isFlying,
                    this.previousPhysicsResult)

            this.previousPhysicsResult = physicsResult
            val finalChunk =
                ChunkUtils.retrieve(this.instance, this.currentChunk, physicsResult.newPosition())
            if (ChunkUtils.isLoaded(finalChunk)) {
                this.velocity = physicsResult.newVelocity().mul(tps)
                this.onGround = physicsResult.isOnGround
                val levitation = this.getEffect(PotionEffect.LEVITATION)
                if (levitation != null) {
                    this.velocity =
                        velocity.withY(
                            (0.05 * (levitation.potion().amplifier() + 1).toDouble() -
                                velocity.y() / tps) * 0.2 * tps)
                }

                this.sendImmediateVelocityUpdate()
            }
        }
    }
}
