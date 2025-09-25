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

public class Newlocity extends Module {
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
    private int hitStreak = 0;
    private long lastHitTime = 0;
    private boolean smartFreeze = true;
    private int freezeTicks = 0;

    public Newlocity() {
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

        // Сброс счетчиков каждые 10 секунд
        if (System.currentTimeMillis() - lastDuelTime > 10000) {
            duelCounter = 0;
            hitStreak = 0;
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

        // Улучшенная логика GrimFreeze
        if (mode.getValue() == Mode.GrimFreeze && isFrozen && e.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
            // Умная логика - не всегда отменяем пакеты
            if (shouldCancelPositionPacket()) {
                e.cancel();
            }
        }

        if (e.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
            duelCounter++;
            ccCooldown = 2 + random.nextInt(3);
        }
    }

    private void handleVelocityPacket(PacketEvent.Receive e, EntityVelocityUpdateS2CPacket pac) {
        if (!onlyAura.getValue() || ModuleManager.aura.isEnabled()) {
            switch (mode.getValue()) {
                case Cancel -> {
                    float variationX = 0.8f + random.nextFloat() * 0.4f;
                    float variationY = 0.7f + random.nextFloat() * 0.6f;
                    float variationZ = 0.8f + random.nextFloat() * 0.4f;

                    setVelocityMotion(pac,
                            (int) (pac.getVelocityX() * variationX),
                            (int) (pac.getVelocityY() * variationY),
                            (int) (pac.getVelocityZ() * variationZ));
                }
                case GrimFreeze -> {
                    // Улучшенная логика для избежания флагов на 12 ударе
                    hitStreak++;
                    lastHitTime = System.currentTimeMillis();
                    
                    // Адаптивная логика в зависимости от серии ударов
                    boolean shouldFreeze = shouldApplyGrimFreeze();
                    
                    if (shouldFreeze) {
                        e.cancel();
                        startSmartFreeze();
                    } else {
                        // Иногда не отменяем velocity для избежания детекции
                        float reductionFactor = 0.1f + random.nextFloat() * 0.3f; // 10-40% от оригинального velocity
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
                    float variation = 0.05f + random.nextFloat() * 0.2f;
                    setExplosionMotion(explosion,
                            explosion.getPlayerVelocityX() * variation,
                            explosion.getPlayerVelocityY() * variation,
                            explosion.getPlayerVelocityZ() * variation);
                }
                case GrimFreeze -> {
                    setExplosionMotion(explosion, 0, 0, 0);
                    if (duelCounter < 2 || random.nextFloat() > 0.2f) {
                        startFreeze();
                    }
                }
            }
        }
    }

    private void startFreeze() {
        isFrozen = true;
        freezeStartTime = System.currentTimeMillis() - random.nextInt(500);
    }
    
    private void startSmartFreeze() {
        isFrozen = true;
        freezeStartTime = System.currentTimeMillis();
        freezeTicks = 3 + random.nextInt(5); // 3-7 тиков заморозки
    }
    
    // Умная логика для определения, нужно ли применять GrimFreeze
    private boolean shouldApplyGrimFreeze() {
        // Если серия ударов меньше 3, всегда применяем
        if (hitStreak < 3) {
            return true;
        }
        
        // Если серия ударов 3-8, применяем с высокой вероятностью
        if (hitStreak <= 8) {
            return random.nextFloat() > 0.1f; // 90% шанс
        }
        
        // Если серия ударов 9-11, применяем с меньшей вероятностью
        if (hitStreak <= 11) {
            return random.nextFloat() > 0.3f; // 70% шанс
        }
        
        // Если серия ударов 12+, применяем редко для избежания детекции
        if (hitStreak <= 15) {
            return random.nextFloat() > 0.6f; // 40% шанс
        }
        
        // Если серия очень длинная, применяем очень редко
        return random.nextFloat() > 0.8f; // 20% шанс
    }
    
    // Умная логика для отмены позиционных пакетов
    private boolean shouldCancelPositionPacket() {
        // Не отменяем первые 2 пакета
        if (duelCounter < 2) {
            return true;
        }
        
        // Адаптивная логика в зависимости от серии ударов
        if (hitStreak < 5) {
            return random.nextFloat() > 0.05f; // 95% шанс отмены
        } else if (hitStreak < 10) {
            return random.nextFloat() > 0.15f; // 85% шанс отмены
        } else if (hitStreak < 15) {
            return random.nextFloat() > 0.3f; // 70% шанс отмены
        } else {
            return random.nextFloat() > 0.5f; // 50% шанс отмены
        }
    }

    @Override
    public void onUpdate() {
        if (shouldPause()) return;

        if (mode.getValue() == Mode.GrimFreeze) {
            // Умная заморозка с тиками
            if (isFrozen) {
                if (freezeTicks > 0) {
                    freezeTicks--;
                    // Принудительно обнуляем velocity во время заморозки
                    mc.player.setVelocity(0, 0, 0);
                } else {
                    isFrozen = false;
                }
            }
            
            // Сброс серии ударов если прошло много времени
            if (System.currentTimeMillis() - lastHitTime > 3000) {
                hitStreak = Math.max(0, hitStreak - 1);
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (mode.getValue() == Mode.GrimFreeze && ModuleManager.aura.isEnabled()) {
            if (Aura.target != null && mc.player.hurtTime > 0) {
                // Умная логика заморозки в зависимости от серии ударов
                if (shouldApplyGrimFreeze()) {
                    startSmartFreeze();
                }
            }
        }
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