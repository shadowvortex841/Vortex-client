package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class VelocityModule extends Module {

    private final Setting<Integer> horizontal = num("Horizontal", 0, 0, 100);
    private final Setting<Integer> vertical   = num("Vertical", 0, 0, 100);
    private final Setting<Integer> chance     = num("Chance", 100, 0, 100);

    public VelocityModule() {
        super("Velocity", "Reduces knockback from entities and explosions", Category.PLAYER);
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) {
            return;
        }

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() != mc.player.getId()) {
                return;
            }
            if (!rollChance()) {
                return;
            }

            event.cancel();

            double horizFactor = 1.0 - (horizontal.getValue() / 100.0);
            double vertFactor  = 1.0 - (vertical.getValue() / 100.0);

            mc.player.setDeltaMovement(
                motionPacket.getXa() * horizFactor,
                motionPacket.getYa() * vertFactor,
                motionPacket.getZa() * horizFactor
            );
            return;
        }

        if (event.getPacket() instanceof ClientboundExplodePacket) {
            if (rollChance()) {
                event.cancel();
            }
        }
    }

    private boolean rollChance() {
        int chanceValue = chance.getValue();
        if (chanceValue >= 100) {
            return true;
        }
        if (chanceValue <= 0) {
            return false;
        }
        return Math.random() * 100.0 < chanceValue;
    }
}
