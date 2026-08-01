package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class VelocityModule extends Module {

    public static final VelocityModule INSTANCE = new VelocityModule();

    public VelocityModule() {
        // FIXED: Explicitly referencing Module.Category.PLAYER fixes the Category error
        super("Velocity", "Removes velocity from explosions and entities", Module.Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        // FIXED: We must check the ID so we don't freeze other players/mobs when they get hit
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                event.cancel(); 
            }
        }
        
        // Explosion knockback only applies to the player receiving it, so it's safe to fully cancel
        if (event.getPacket() instanceof ClientboundExplodePacket) {
            event.cancel();
        }
    }
}
