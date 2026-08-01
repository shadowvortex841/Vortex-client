if (event.getPacket() instanceof ClientboundSetEntityMotionPacket) {
    ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) event.getPacket();
    if (motionPacket.getId() != mc.player.getId()) return;
    if (!rollChance()) return;
    event.cancel();

    // convert packet ints to actual velocity (adjust divisor to match your mappings)
    double motionX = motionPacket.getXa() / 8000.0;
    double motionY = motionPacket.getYa() / 8000.0;
    double motionZ = motionPacket.getZa() / 8000.0;

    double horizFactor = 1.0 - (horizontal.getValue() / 100.0);
    double vertFactor  = 1.0 - (vertical.getValue() / 100.0);

    mc.player.setDeltaMovement(
        motionX * horizFactor,
        motionY * vertFactor,
        motionZ * horizFactor
    );
    return;
}
