package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class VelocityModule extends Module {
    
    public static final VelocityModule INSTANCE = new VelocityModule();

    // Vape-style settings (0.0 to 1.0)
    public float horizontal = 0.0f;   // 0% horizontal KB taken
    public float vertical = 0.0f;     // 0% vertical KB taken
    public float chance = 1.0f;       // 100% chance to apply reduction
    
    // Delay settings to bypass anticheat desync checks
    public int delayTicks = 3;        // Delay packets by 3 ticks to simulate lag/desync
    
    // Queues to hold delayed packets
    private final ConcurrentLinkedQueue<DelayedPacket<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private int currentTick = 0;

    public VelocityModule() {
        super("Velocity", "Reduces knockback bypassing anticheats (Vape style)", Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        // 1. Intercept Entity Motion Packet (Player KB)
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                // Roll the chance. If we fail the chance, let vanilla handle it (legit)
                if (ThreadLocalRandom.current().nextFloat() <= chance) {
                    event.cancel(); // Cancel original instant processing
                    
                    // Queue it for delayed processing
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
     * Called every client tick (usually via your client's tick event manager).
     * Processes the queue and applies the modified packets.
     */
    public void onTick() {
        currentTick++;
        
        if (packetQueue.isEmpty()) return;

        packetQueue.removeIf(delayed -> {
            if (currentTick >= delayed.targetTick) {
                // Roll back the cancellation and apply the packet manually
                if (delayed.packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
                    applyMotionPacket(motionPacket);
                } else if (delayed.packet instanceof ClientboundExplodePacket explodePacket) {
                    applyExplosionPacket(explodePacket);
                }
                return true; // Remove from queue
            }
            return false;
        });
    }

    private void applyMotionPacket(ClientboundSetEntityMotionPacket packet) {
        if (mc.player == null) return;
        
        // Corrected getters for Mojang mappings / modern Fabric
        double vX = packet.getX() / 8000.0;
        double vY = packet.getY() / 8000.0;
        double vZ = packet.getZ() / 8000.0;

        // Scale by our Vape settings
        mc.player.setDeltaMovement(
            mc.player.getDeltaMovement().add(vX * horizontal, vY * vertical, vZ * horizontal)
        );
    }

    private void applyExplosionPacket(ClientboundExplodePacket packet) {
        if (mc.player == null) return;
        
        // Corrected explosion velocity calculation using getPlayerKnockback() or Vec3
        Vec3 knockback = packet.getPlayerKnockback();
        if (knockback != null) {
            double vX = knockback.x;
            double vY = knockback.y;
            double vZ = knockback.z;

            // Scale by our Vape settings
            mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().add(vX * horizontal, vY * vertical, vZ * horizontal)
            );
        }
    }

    // Helper class to store packet data and target tick
    private static class DelayedPacket<T> {
        final T packet;
        final int targetTick;

        DelayedPacket(T packet, int targetTick) {
            this.packet = packet;
            this.targetTick = targetTick;
        }
    }
}
