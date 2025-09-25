package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;

import java.lang.reflect.Field;
import java.util.Random;

public class GrimVelocity extends Module {
    public Setting<Boolean> onlyAura = new Setting<>("OnlyDuringAura", false);
    public Setting<Boolean> pauseInWater = new Setting<>("PauseInLiquids", false);
    public Setting<Boolean> explosions = new Setting<>("Explosions", true);
    public Setting<Boolean> cc = new Setting<>("PauseOnFlag", false);
    public Setting<Boolean> fire = new Setting<>("PauseOnFire", false);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Cancel);

    private final Random random = new Random();
    private int ccCooldown;
    private long freezeStartTime;
    private boolean isFrozen;
    private int duelCounter = 0;
    private long lastDuelTime = 0;
    private int velocityPackets = 0;
    private long lastVelocityTime = 0;

    public GrimVelocity() {
        super("GrimVelocity", Module.Category.MOVEMENT);
    }

    // Метод для изменения velocity через рефлексию
    private void setVelocityMotion(EntityVelocityUpdateS2CPacket packet, int motionX, int motionY, int motionZ) {
        try {
            Field velocityX = packet.getClass().getDeclaredField("velocityX");
            Field velocityY = packet.getClass().getDeclaredField("velocityY");
            Field velocityZ = packet.getClass().getDeclaredField("velocityZ");
            velocityX.setAccessible(true);
            velocityY.setAccessible(true);
            velocityZ.setAccessible(true);
            velocityX.setInt(packet, motionX);
            velocityY.setInt(packet, motionY);
            velocityZ.setInt(packet, motionZ);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Метод для изменения explosion motion через рефлексию
    private void setExplosionMotion(ExplosionS2CPacket explosion, float motionX, float motionY, float motionZ) {
        try {
            Field fieldX = explosion.getClass().getDeclaredField("playerVelocityX");
            Field fieldY = explosion.getClass().getDeclaredField("playerVelocityY");
            Field fieldZ = explosion.getClass().getDeclaredField("playerVelocityZ");
            fieldX.setAccessible(true);
            fieldY.setAccessible(true);
            fieldZ.setAccessible(true);
            fieldX.setFloat(explosion, motionX);
            fieldY.setFloat(explosion, motionY);
            fieldZ.setFloat(explosion, motionZ);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        if (shouldPause()) return;

        // Сброс счетчиков каждые 5 секунд
        if (System.currentTimeMillis() - lastDuelTime > 5000) {
            duelCounter = 0;
            velocityPackets = 0;
        }
        lastDuelTime = System.currentTimeMillis();

        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }

        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket pac && pac.getId() == mc.player.getId()) {
            handleVelocityPacket(e, pac);
        } else if (e.getPacket() instanceof ExplosionS2CPacket explosion) {
            handleExplosionPacket(e, explosion);
        }

        // GrimFreeze логика с обходом BadPacketsA - как было при 100 ударах
        if (mode.getValue() == Mode.GrimFreeze && isFrozen && e.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
            // Обход BadPacketsA - почти всегда отменяем
            if (duelCounter < 5 || random.nextFloat() > 0.05f) {
                e.cancel();
            }
        }

        if (e.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
            duelCounter++;
            ccCooldown = 1 + random.nextInt(2);
        }
    }

    private void handleVelocityPacket(PacketEvent.Receive e, EntityVelocityUpdateS2CPacket pac) {
        if (!onlyAura.getValue() || ModuleManager.aura.isEnabled()) {
            velocityPackets++;
            lastVelocityTime = System.currentTimeMillis();
            
            switch (mode.getValue()) {
                case Cancel -> {
                    // Обход KnockbackHandler - используем рандомные множители
                    float variationX = 0.7f + random.nextFloat() * 0.6f; // 0.7-1.3
                    float variationY = 0.6f + random.nextFloat() * 0.8f; // 0.6-1.4
                    float variationZ = 0.7f + random.nextFloat() * 0.6f; // 0.7-1.3

                    setVelocityMotion(pac,
                            (int) (pac.getVelocityX() * variationX),
                            (int) (pac.getVelocityY() * variationY),
                            (int) (pac.getVelocityZ() * variationZ));
                }
                case GrimFreeze -> {
                    // Обход KnockbackHandler - как было при 100 ударах
                    if (velocityPackets < 3 || random.nextFloat() > 0.05f) {
                        e.cancel();
                        startFreeze();
                    } else {
                        // Редко уменьшаем velocity вместо полной отмены
                        float reductionFactor = 0.01f + random.nextFloat() * 0.04f; // 1-5%
                        setVelocityMotion(pac,
                                (int) (pac.getVelocityX() * reductionFactor),
                                (int) (pac.getVelocityY() * reductionFactor),
                                (int) (pac.getVelocityZ() * reductionFactor));
                    }
                }
            }
        }
    }

    private void handleExplosionPacket(PacketEvent.Receive e, ExplosionS2CPacket explosion) {
        if (explosions.getValue()) {
            switch (mode.getValue()) {
                case Cancel -> {
                    // Обход ExplosionHandler - используем рандомные множители
                    float variation = 0.1f + random.nextFloat() * 0.3f; // 0.1-0.4
                    setExplosionMotion(explosion,
                            explosion.getPlayerVelocityX() * variation,
                            explosion.getPlayerVelocityY() * variation,
                            explosion.getPlayerVelocityZ() * variation);
                }
                case GrimFreeze -> {
                    // Обход ExplosionHandler - как было при 100 ударах
                    if (random.nextFloat() > 0.1f) {
                        setExplosionMotion(explosion, 0, 0, 0);
                        startFreeze();
                    } else {
                        // Редко уменьшаем вместо полного обнуления
                        float reductionFactor = 0.005f + random.nextFloat() * 0.015f; // 0.5-2%
                        setExplosionMotion(explosion,
                                explosion.getPlayerVelocityX() * reductionFactor,
                                explosion.getPlayerVelocityY() * reductionFactor,
                                explosion.getPlayerVelocityZ() * reductionFactor);
                    }
                }
            }
        }
    }

    private void startFreeze() {
        isFrozen = true;
        freezeStartTime = System.currentTimeMillis();
    }

    @Override
    public void onUpdate() {
        if (shouldPause()) return;

        if (mode.getValue() == Mode.GrimFreeze) {
            // Заморозка как было при 100 ударах
            if (isFrozen && System.currentTimeMillis() - freezeStartTime >= (300 + random.nextInt(500))) {
                isFrozen = false;
            }
            
            // Сброс счетчиков если прошло много времени
            if (System.currentTimeMillis() - lastVelocityTime > 2000) {
                velocityPackets = Math.max(0, velocityPackets - 1);
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (mode.getValue() == Mode.GrimFreeze && ModuleManager.aura.isEnabled()) {
            if (Aura.target != null && mc.player.hurtTime > 0) {
                // Логика заморозки как было при 100 ударах
                if (random.nextFloat() > 0.05f) { // 95% шанс
                    startFreeze();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Сброс счетчиков при отключении
        velocityPackets = 0;
        isFrozen = false;
    }

    private boolean shouldPause() {
        return (mc.player.isTouchingWater() || mc.player.isInLava()) && pauseInWater.getValue() ||
                mc.player.isOnFire() && fire.getValue() && mc.player.getFireTicks() > 0;
    }

    public enum Mode {
        Cancel,
        GrimFreeze
    }
}