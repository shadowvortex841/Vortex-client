package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class VelocityModule extends Module {
    
    public static final VelocityModule INSTANCE = new VelocityModule();

    // Settings (0.0 to 1.0)
    public float horizontal = 0.0f;   // 0% horizontal KB taken
    public float vertical = 0.0f;     // 0% vertical KB taken
    public float chance = 1.0f;       // 100% chance to apply reduction
    
    // Delay settings to bypass anticheat desync checks
    public int delayTicks = 3;        // Delay packets by 3 ticks to simulate lag/desync
    
    // Queue to hold delayed packets
    private final ConcurrentLinkedQueue<DelayedPacket<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private int currentTick = 0;

    public VelocityModule() {
        super("Velocity", "Reduces knockback bypassing anticheats", Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        // 1. Intercept Entity Motion Packet (Player KB)
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                if (ThreadLocalRandom.current().nextFloat() <= chance) {
                    event.cancel();
                    packetQueue.add(new DelayedPacket<>(motionPacket, currentTick + delayTicks));
                }
            }
        }
        
        // 2. Intercept Explosion Packet (TNT/Crystal KB)
        if (event.getPacket() instanceof ClientboundExplodePacket explodePacket) {
            if (ThreadLocalRandom.current().nextFloat() <= chance) {
                event.cancel();
                packetQueue.add(new DelayedPacket<>(explodePacket, currentTick + delayTicks));
            }
        }
    }

    /**
     * Called every client tick to process queued packets.
     */
    public void onTick() {
        currentTick++;
        
        if (packetQueue.isEmpty()) return;

        packetQueue.removeIf(delayed -> {
            if (currentTick >= delayed.targetTick) {
                if (delayed.packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
                    applyMotionPacket(motionPacket);
                } else if (delayed.packet instanceof ClientboundExplodePacket explodePacket) {
                    applyExplosionPacket(explodePacket);
                }
                return true;
            }
            return false;
        });
    }

    private void applyMotionPacket(ClientboundSetEntityMotionPacket packet) {
        if (mc.player == null) return;
        
        // 1.21+ Mojang mappings: getMovement() returns Vec3 directly
        Vec3 movement = packet.getMovement();
        if (movement != null) {
            double vX = movement.x;
            double vY = movement.y;
            double vZ = movement.z;

            mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().add(vX * horizontal, vY * vertical, vZ * horizontal)
            );
        }
    }

    private void applyExplosionPacket(ClientboundExplodePacket packet) {
        if (mc.player == null) return;
        
        // 1.21+ Mojang mappings: Record component accessor is playerKnockback() returning Optional<Vec3>
        Optional<Vec3> knockbackOpt = packet.playerKnockback();
        if (knockbackOpt.isPresent()) {
            Vec3 knockback = knockbackOpt.get();
            double vX = knockback.x;
            double vY = knockback.y;
            double vZ = knockback.z;

            mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().add(vX * horizontal, vY * vertical, vZ * horizontal)
            );
        }
    }

    private static class DelayedPacket<T> {
        final T packet;
        final int targetTick;

        DelayedPacket(T packet, int targetTick) {
            this.packet = packet;
            this.targetTick = targetTick;
        }
    }
}
