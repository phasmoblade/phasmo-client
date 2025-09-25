package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.core.Managers;

import java.util.Random;

public class Disabler extends Module {
    
    // Основные настройки
    public final Setting<SettingGroup> main = new Setting<>("Main", new SettingGroup(false, 0));
    public final Setting<Boolean> enableDisabler = new Setting<>("EnableDisabler", true).addToGroup(main);
    public final Setting<DisablerMode> disablerMode = new Setting<>("DisablerMode", DisablerMode.STEALTH, v -> enableDisabler.getValue()).addToGroup(main);
    public final Setting<Boolean> showNotifications = new Setting<>("ShowNotifications", true, v -> enableDisabler.getValue()).addToGroup(main);
    public final Setting<Boolean> debugMode = new Setting<>("DebugMode", false, v -> enableDisabler.getValue()).addToGroup(main);
    
    // Настройки обхода проверок
    public final Setting<SettingGroup> bypasses = new Setting<>("Bypasses", new SettingGroup(false, 0));
    public final Setting<Boolean> bypassTimer = new Setting<>("BypassTimer", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassSimulation = new Setting<>("BypassSimulation", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassAimDuplicate = new Setting<>("BypassAimDuplicate", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassAimModulo360 = new Setting<>("BypassAimModulo360", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassGroundSpoof = new Setting<>("BypassGroundSpoof", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassNoFall = new Setting<>("BypassNoFall", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassMovement = new Setting<>("BypassMovement", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassReach = new Setting<>("BypassReach", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassNoSlow = new Setting<>("BypassNoSlow", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassFlight = new Setting<>("BypassFlight", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassBadPackets = new Setting<>("BypassBadPackets", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassAim = new Setting<>("BypassAim", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassSpeed = new Setting<>("BypassSpeed", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    public final Setting<Boolean> bypassVelocity = new Setting<>("BypassVelocity", true, v -> enableDisabler.getValue()).addToGroup(bypasses);
    
    // Настройки пакетов
    public final Setting<SettingGroup> packets = new Setting<>("Packets", new SettingGroup(false, 0));
    public final Setting<Integer> packetDelay = new Setting<>("PacketDelay", 200, 50, 1000, v -> enableDisabler.getValue()).addToGroup(packets);
    public final Setting<Boolean> randomizeDelay = new Setting<>("RandomizeDelay", true, v -> enableDisabler.getValue()).addToGroup(packets);
    public final Setting<Float> randomizationFactor = new Setting<>("RandomizationFactor", 0.3f, 0.1f, 0.8f, v -> enableDisabler.getValue() && randomizeDelay.getValue()).addToGroup(packets);
    
    // Поля для работы
    private long lastPacketTime = 0;
    private long lastMovementTime = 0;
    private long lastRotationTime = 0;
    private boolean isActive = false;
    private Random random = new Random();
    private int successfulBypasses = 0;
    private int failedBypasses = 0;
    private long lastNotificationTime = 0;
    
    // Поля для движения
    private boolean isMoving = false;
    private long lastMovementCheck = 0;
    
    public Disabler() {
        super("Disabler", Category.MISC);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        isActive = true;
        successfulBypasses = 0;
        failedBypasses = 0;
        lastPacketTime = System.currentTimeMillis();
        lastMovementTime = System.currentTimeMillis();
        lastRotationTime = System.currentTimeMillis();
        lastMovementCheck = System.currentTimeMillis();
        
        if (showNotifications.getValue()) {
            Managers.NOTIFICATION.publicity("Disabler", "Активирован режим: " + disablerMode.getValue().name(), 5000, Notification.Type.INFO);
        }
        
        if (debugMode.getValue()) {
            sendMessage("Disabler активирован в режиме: " + disablerMode.getValue().name());
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        isActive = false;
        
        if (showNotifications.getValue()) {
            Managers.NOTIFICATION.publicity("Disabler", "Деактивирован. Успешных обходов: " + successfulBypasses, 5000, Notification.Type.INFO);
        }
        
        if (debugMode.getValue()) {
            sendMessage("Disabler деактивирован. Статистика: Успешно: " + successfulBypasses + ", Неудачно: " + failedBypasses);
        }
    }
    
    @EventHandler
    public void onUpdate(EventSync event) {
        if (!isActive || mc.player == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Обновляем состояние движения
        updateMovementState();
        
        // Обновляем статистику
        updateStatistics();
    }
    
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!isActive || mc.player == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Обход Timer проверок
        if (bypassTimer.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (currentTime - lastPacketTime < getPacketDelay()) {
                return; // Блокируем пакет
            }
            lastPacketTime = currentTime;
            successfulBypasses++;
        }
        
        // Обход Simulation проверок
        if (bypassSimulation.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (currentTime - lastMovementTime < getPacketDelay() * 2) {
                return; // Блокируем пакет
            }
            lastMovementTime = currentTime;
            successfulBypasses++;
        }
        
        // Обход AimDuplicateLook проверок
        if (bypassAimDuplicate.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                if (currentTime - lastRotationTime < getPacketDelay()) {
                    return; // Блокируем пакет
                }
                lastRotationTime = currentTime;
                successfulBypasses++;
            }
        }
        
        // Обход AimModulo360 проверок
        if (bypassAimModulo360.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                if (currentTime - lastRotationTime < getPacketDelay() * 2) {
                    return; // Блокируем пакет
                }
                lastRotationTime = currentTime;
                successfulBypasses++;
            }
        }
        
        // Обход GroundSpoof проверок
        if (bypassGroundSpoof.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (currentTime - lastMovementTime < getPacketDelay() * 3) {
                return; // Блокируем пакет
            }
            lastMovementTime = currentTime;
            successfulBypasses++;
        }
        
        // Обход NoFall проверок
        if (bypassNoFall.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (mc.player.fallDistance > 0) {
                if (currentTime - lastMovementTime < getPacketDelay()) {
                    return; // Блокируем пакет при падении
                }
                lastMovementTime = currentTime;
            }
            successfulBypasses++;
        }
        
        // Обход Movement проверок
        if (bypassMovement.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (currentTime - lastMovementTime < 50) {
                return; // Блокируем пакет
            }
            lastMovementTime = currentTime;
            successfulBypasses++;
        }
        
        // Обход Reach проверок
        if (bypassReach.getValue() && event.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket) {
            successfulBypasses++;
        }
        
        // Обход NoSlow проверок
        if (bypassNoSlow.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            successfulBypasses++;
        }
        
        // Обход Flight проверок
        if (bypassFlight.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            successfulBypasses++;
        }
        
        // Обход BadPackets проверок
        if (bypassBadPackets.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            successfulBypasses++;
        }
        
        // Обход Aim проверок
        if (bypassAim.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                successfulBypasses++;
            }
        }
        
        // Обход Speed проверок
        if (bypassSpeed.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            successfulBypasses++;
        }
        
        // Обход Velocity проверок
        if (bypassVelocity.getValue() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            successfulBypasses++;
        }
    }
    
    private void updateMovementState() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastMovementCheck > 100) { // Проверяем каждые 100мс
            // Простая проверка движения
            isMoving = mc.player.input.movementForward != 0 || 
                      mc.player.input.movementSideways != 0 ||
                      mc.player.input.jumping || mc.player.input.sneaking;
            lastMovementCheck = currentTime;
        }
    }
    
    private int getPacketDelay() {
        int baseDelay = packetDelay.getValue();
        if (randomizeDelay.getValue()) {
            float factor = randomizationFactor.getValue();
            int variation = (int) (baseDelay * factor);
            return baseDelay + random.nextInt(variation * 2) - variation;
        }
        return baseDelay;
    }
    
    private void updateStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime > 10000) { // Каждые 10 секунд
            if (debugMode.getValue()) {
                sendMessage("Статистика Disabler: Успешно: " + successfulBypasses + ", Неудачно: " + failedBypasses + 
                           ", Движение: " + (isMoving ? "Да" : "Нет"));
            }
            lastNotificationTime = currentTime;
        }
    }
    
    // Геттеры для статистики
    public int getSuccessfulBypasses() {
        return successfulBypasses;
    }
    
    public int getFailedBypasses() {
        return failedBypasses;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean isMoving() {
        return isMoving;
    }
    
    // Перечисление режимов
    public enum DisablerMode {
        AGGRESSIVE("Агрессивный"),
        STEALTH("Скрытый");
        
        private final String displayName;
        
        DisablerMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}