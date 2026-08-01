import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class VelocityModule extends Module {

    public static final VelocityModule INSTANCE = new VelocityModule();

    // 0 = no reduction (full vanilla knockback), 100 = fully negated
    private double horizontal = 100;
    private double vertical = 100;
    // 0-100, chance the reduction triggers on any given hit
    private double chance = 100;

    public VelocityModule() {
        super("Velocity", "Removes velocity from explosions and entities", Module.Category.PLAYER);
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() != mc.player.getId()) return;
            if (!rollChance()) return;

            event.cancel();
            mc.player.setDeltaMovement(
                motionPacket.getXa() * (1 - horizontal / 100.0),
                motionPacket.getYa() * (1 - vertical / 100.0),
                motionPacket.getZa() * (1 - horizontal / 100.0)
            );
        } else if (event.getPacket() instanceof ClientboundExplodePacket) {
            if (rollChance()) event.cancel();
        }
    }

    private boolean rollChance() {
        return Math.random() * 100 < chance;
    }

    public double getHorizontal() { return horizontal; }
    public void setHorizontal(double horizontal) { this.horizontal = horizontal; }

    public double getVertical() { return vertical; }
    public void setVertical(double vertical) { this.vertical = vertical; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }
}
