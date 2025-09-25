package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.core.Managers;
import thunder.hack.setting.impl.SettingGroup;

public class ACDetector extends Module {
    public final Setting<SettingGroup> checksGroup = new Setting<>("Flags", new SettingGroup(false, 0));
    private final Setting<Boolean> speedCheck = new Setting<>("Speedk", true).addToGroup(checksGroup);
    private final Setting<Boolean> reachCheck = new Setting<>("Reach", true).addToGroup(checksGroup);
    private final Setting<Boolean> timerCheck = new Setting<>("Timer", true).addToGroup(checksGroup);
    private final Setting<Boolean> simulationCheck = new Setting<>("Simulation", true).addToGroup(checksGroup);
    private final Setting<Boolean> velocityCheck = new Setting<>("Velocity", true).addToGroup(checksGroup);
    private final Setting<Boolean> badPacketsCheck = new Setting<>("BadPackets", true).addToGroup(checksGroup);
    private final Setting<Boolean> fastBreakCheck = new Setting<>("FastBreak", true).addToGroup(checksGroup);
    private final Setting<Boolean> flyCheck = new Setting<>("Fly", true).addToGroup(checksGroup);
    private final Setting<Boolean> noFallCheck = new Setting<>("NoFall", true).addToGroup(checksGroup);
    private final Setting<Boolean> jesusCheck = new Setting<>("Jesus", true).addToGroup(checksGroup);
    private final Setting<Boolean> inventoryMoveCheck = new Setting<>("InventoryMove", true).addToGroup(checksGroup);
    private final Setting<Boolean> notfoundcheck = new Setting<>("NotFoundChecks", true);
    public final Setting<Boolean> notify = new Setting<>("Notify", true);
    public final Setting<Boolean> song = new Setting<>("Song", false);
    private final Setting<Boolean> antiFakeFlag = new Setting<>("AntiFakeFlag", false);
    private Vec3d lastPos = Vec3d.ZERO;
    private Vec3d lastServerPos = Vec3d.ZERO;
    private BlockPos lastBlockPos;
    private long lastBlockBreakTime;
    private double lastFallDistance = 0;
    private int airTicks = 0;
    private boolean gotHit = false;
    private int speedVerbose = 0;
    private int reachVerbose = 0;
    private int timerVerbose = 0;
    private int simulationVerbose = 0;
    private int velocityVerbose = 0;
    private int badPacketsVerbose = 0;
    private int fastBreakVerbose = 0;
    private int flyVerbose = 0;
    private int noFallVerbose = 0;
    private int jesusVerbose = 0;
    private int inventoryMoveVerbose = 0;
    private boolean enderPearlThrown = false;
    private long lastPearlTime = 0;
    private long lastVelocityTick = -1;
    private boolean expectingVelocity = false;
    private long lastTimerCheckTime = System.currentTimeMillis();
    private int clientTicks = 0;
    private boolean wasKnockedByFireball = false;
    private long lastFireballTime = 0;
    private long lastPPLPacketTime = 0;

    public ACDetector() {
        super("AntiCheatFlags", Category.MISC);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;
        clientTicks++;
        if (wasKnockedByFireball && System.currentTimeMillis() - lastFireballTime >= 1000) wasKnockedByFireball = false;
        if (speedCheck.getValue()) checkSpeed();
        if (timerCheck.getValue()) checkTimer();
        if (simulationCheck.getValue()) checkSimulation();
        if (flyCheck.getValue()) checkFly();
        if (noFallCheck.getValue()) checkNoFall();
        if (jesusCheck.getValue()) checkJesus();
        if (inventoryMoveCheck.getValue()) checkInventoryMove();
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (!reachCheck.getValue()) return;
        if (e.getEntity() == null) return;
        String entityStr = e.getEntity().toString().toLowerCase();
        if (entityStr.contains("fireball")) {
            wasKnockedByFireball = true;
            lastFireballTime = System.currentTimeMillis();
            return;
        }
        double distance = mc.player.distanceTo(e.getEntity());
        if (distance > 3.5 && Managers.SERVER.getPing() < 150 && Managers.SERVER.getTPS() > 18) {
            reachVerbose++;
            if (reachVerbose >= 5) {
                String info = String.format("%.5f blocks", distance);
                sendClientGrimFlag("Reach", reachVerbose, info);
                reachVerbose = 0;
            }
        } else {
            reachVerbose = Math.max(reachVerbose - 1, 0);
        }
        if (e.getEntity() instanceof Entity && e.isPre()) {
            gotHit = true;
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            lastPPLPacketTime = System.currentTimeMillis();
            if (enderPearlThrown && (System.currentTimeMillis() - lastPearlTime) < 1000) return;
            if (!antiFakeFlag.getValue()) {
                sendClientGrimFlag("Flag not found", velocityVerbose, "You are flagged but not found flag.");
            }
        }
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getId() == mc.player.getId() && gotHit) {
                expectingVelocity = true;
                lastVelocityTick = mc.player.age;
                gotHit = false;
            }
        }
        if (expectingVelocity) {
            if (mc.player.age - lastVelocityTick > 1) {
                if (mc.player.getVelocity().length() < 0.08 && mc.player.isOnGround()) {
                    velocityVerbose++;
                    if (velocityVerbose >= 3) {
                        String info = String.format("No knockback %.3f", mc.player.getVelocity().length());
                        sendClientGrimFlag("Velocity", velocityVerbose, info);
                        velocityVerbose = 0;
                    }
                } else {
                    velocityVerbose = Math.max(velocityVerbose - 1, 0);
                }
                expectingVelocity = false;
            }
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof PlayerInteractItemC2SPacket packet) {
            if (mc.player.getStackInHand(packet.getHand()).getItem().toString().toLowerCase().contains("ender_pearl")) {
                enderPearlThrown = true;
                lastPearlTime = System.currentTimeMillis();
            }
        }
        if (badPacketsCheck.getValue() && e.getPacket() instanceof PlayerMoveC2SPacket) {
            if (mc.player.getVelocity().lengthSquared() > 15) {
                badPacketsVerbose++;
                if (badPacketsVerbose >= 5) {
                    String info = "Invalid motion state=true";
                    sendClientGrimFlag("BadPackets", badPacketsVerbose, info);
                    badPacketsVerbose = 0;
                }
            } else {
                badPacketsVerbose = Math.max(badPacketsVerbose - 1, 0);
            }
        }
        if (fastBreakCheck.getValue() && e.getPacket() instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                BlockPos pos = packet.getPos();
                BlockState state = mc.world.getBlockState(pos);
                if (state.isAir()) return;
                float hardness = state.getBlock().getHardness();
                if (hardness <= 0) return;
                double expectedBreakTime = hardness * 1.5;
                long breakTime = System.currentTimeMillis() - lastBlockBreakTime;
                if (lastBlockPos != null && lastBlockPos.equals(pos)) {
                    if (breakTime < expectedBreakTime * 0.15 && !mc.player.hasStatusEffect(StatusEffects.HASTE)) {
                        fastBreakVerbose++;
                        if (fastBreakVerbose >= 3) {
                            String info = String.format("%.2fms < %.2fms", (double) breakTime, expectedBreakTime * 0.15);
                            sendClientGrimFlag("FastBreak", fastBreakVerbose, info);
                            fastBreakVerbose = 0;
                        }
                    } else {
                        fastBreakVerbose = Math.max(fastBreakVerbose - 1, 0);
                    }
                }
                lastBlockPos = pos;
                lastBlockBreakTime = System.currentTimeMillis();
            }
        }
    }

    private void checkFly() {
        if (mc.player.isFallFlying() || mc.player.getAbilities().flying || mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            flyVerbose = 0;
            airTicks = 0;
            return;
        }
        boolean inCobweb = mc.world.getBlockState(mc.player.getBlockPos()).getBlock().getTranslationKey().contains("cobweb");
        boolean inWater = mc.player.isTouchingWater();
        boolean onLadder = mc.player.isClimbing();
        if (inCobweb || inWater || onLadder) {
            flyVerbose = 0;
            airTicks = 0;
            return;
        }
        if (!mc.player.isOnGround()) {
            airTicks++;
        } else {
            airTicks = 0;
            flyVerbose = Math.max(flyVerbose - 1, 0);
        }
        if (airTicks > 15) {
            double motionY = mc.player.getVelocity().y;
            boolean invalidMotionY = motionY > 0.42 || motionY == 0;
            boolean noGravity = !mc.player.hasNoGravity();
            if (invalidMotionY && noGravity) {
                flyVerbose++;
                if (flyVerbose >= 5) {
                    String info = "Flying > 15 ticks (" + airTicks + " ticks)";
                    sendClientGrimFlag("Fly", flyVerbose, info);
                    flyVerbose = 0;
                    airTicks = 0;
                }
            } else {
                flyVerbose = Math.max(flyVerbose - 1, 0);
            }
        }
    }

    private void checkNoFall() {
        if (enderPearlThrown && (System.currentTimeMillis() - lastPearlTime < 1000)) return;
        if (wasKnockedByFireball && (System.currentTimeMillis() - lastFireballTime < 1000)) return;
        if (mc.player.isFallFlying() || mc.player.getAbilities().flying || mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            noFallVerbose = 0;
            return;
        }
        if (lastFallDistance > 3.5 && mc.player.fallDistance == 0 && mc.player.isOnGround()) {
            boolean isOnSlime = mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock().getTranslationKey().contains("slime");
            if (!isOnSlime && mc.player.hurtTime <= 0) {
                noFallVerbose++;
                if (noFallVerbose >= 5) {
                    String info = "Fall > 3.5, no damage";
                    sendClientGrimFlag("NoFall", noFallVerbose, info);
                    noFallVerbose = 0;
                }
            } else {
                noFallVerbose = Math.max(noFallVerbose - 1, 0);
            }
        }
        lastFallDistance = mc.player.fallDistance;
    }

    private void checkJesus() {
        if (mc.player.isFallFlying() || mc.player.getAbilities().flying || mc.player.hasVehicle()) {
            jesusVerbose = 0;
            return;
        }
        if (mc.player.isTouchingWater() && !mc.player.isSwimming() && Math.abs(mc.player.getVelocity().y) < 0.005) {
            jesusVerbose++;
            if (jesusVerbose >= 8) {
                String info = "Water Y stable";
                sendClientGrimFlag("Jesus", jesusVerbose, info);
                jesusVerbose = 0;
            }
        } else {
            jesusVerbose = Math.max(jesusVerbose - 1, 0);
        }
    }

    private void checkInventoryMove() {
        if (mc.currentScreen != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
            if (isMoving) {
                inventoryMoveVerbose++;
                if (inventoryMoveVerbose >= 5) {
                    String info = "Moving with inventory open";
                    sendClientGrimFlag("InventoryMove", inventoryMoveVerbose, info);
                    inventoryMoveVerbose = 0;
                }
            } else {
                inventoryMoveVerbose = Math.max(inventoryMoveVerbose - 1, 0);
            }
        } else {
            inventoryMoveVerbose = 0;
        }
    }

    private void checkSpeed() {
        BlockState blockBelow = mc.world.getBlockState(mc.player.getBlockPos().down());
        if (blockBelow.getBlock().getTranslationKey().contains("slime")) {
            speedVerbose = 0;
            lastPos = mc.player.getPos();
            return;
        }
        if (mc.player.fallDistance > 0) {
            speedVerbose = 0;
            lastPos = mc.player.getPos();
            return;
        }
        Vec3d pos = mc.player.getPos();
        double distance = pos.distanceTo(lastPos);
        double maxAllowedSpeed = getMaxAllowedSpeed();
        boolean hasSpeed = mc.player.hasStatusEffect(StatusEffects.SPEED);
        boolean hasJumpBoost = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST);
        boolean hasLevitation = mc.player.hasStatusEffect(StatusEffects.LEVITATION);
        boolean isInKnockback = mc.player.hurtTime > 0;
        boolean isFlying = mc.player.getAbilities().flying;
        boolean isElytraFlying = mc.player.isFallFlying();
        if (!hasSpeed && !hasJumpBoost && !hasLevitation && !isInKnockback && !isFlying && !isElytraFlying) {
            if (distance > maxAllowedSpeed && distance >= 0.710) {
                speedVerbose++;
                if (speedVerbose >= 3) {
                    String info = String.format("%.3f m/s", distance);
                    sendClientGrimFlag("Speed", speedVerbose, info);
                    speedVerbose = 0;
                }
            } else {
                speedVerbose = Math.max(speedVerbose - 1, 0);
            }
        }
        lastPos = pos;
    }

    private void checkTimer() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastTimerCheckTime;
        if (elapsed >= 1000L) {
            float ticksPerSecond = clientTicks * 1000f / elapsed;
            float serverTPS = Managers.SERVER.getTPS();
            if (ticksPerSecond > 55) {
                timerVerbose++;
                if (timerVerbose >= 5) {
                    String info = String.format("%.2f tps (server %.2f)", ticksPerSecond, serverTPS);
                    sendClientGrimFlag("Timer", timerVerbose, info);
                    timerVerbose = 0;
                }
            } else {
                timerVerbose = Math.max(timerVerbose - 1, 0);
            }
            clientTicks = 0;
            lastTimerCheckTime = currentTime;
        }
    }

    private void checkSimulation() {
        BlockState blockBelow = mc.world.getBlockState(mc.player.getBlockPos().down());
        if (blockBelow.getBlock().getTranslationKey().contains("slime")) {
            simulationVerbose = 0;
            lastServerPos = mc.player.getPos();
            return;
        }
        if (enderPearlThrown && (System.currentTimeMillis() - lastPearlTime < 1000)) {
            simulationVerbose = 0;
            lastServerPos = mc.player.getPos();
            return;
        }
        if (wasKnockedByFireball && (System.currentTimeMillis() - lastFireballTime < 1000)) {
            simulationVerbose = 0;
            lastServerPos = mc.player.getPos();
            return;
        }
        Vec3d serverPos = mc.player.getPos();
        double posDiff = serverPos.distanceTo(lastServerPos);
        boolean isInKnockback = mc.player.hurtTime > 0;
        if (mc.player.fallDistance > 0 || isInKnockback) {
            simulationVerbose = 0;
            lastServerPos = serverPos;
            return;
        }
        if (posDiff > 0.75) {
            simulationVerbose++;
            if (simulationVerbose >= 5) {
                String info = String.format("%.3f desync", posDiff);
                sendClientGrimFlag("Simulation", simulationVerbose, info);
                simulationVerbose = 0;
            }
        } else {
            simulationVerbose = Math.max(simulationVerbose - 1, 0);
        }
        lastServerPos = serverPos;
    }

    private double getMaxAllowedSpeed() {
        if (mc.player.isFallFlying() || mc.player.getAbilities().flying) return 999;
        if (mc.player.isOnGround()) {
            if (mc.player.isSprinting()) return 0.3;
            else return 0.25;
        }
        if (mc.player.isSprinting()) return 0.45;
        return 0.38;
    }

    private void sendClientGrimFlag(String type, int verbose, String extraInfo) {
        long currentTime = System.currentTimeMillis();
        if (antiFakeFlag.getValue() && currentTime - lastPPLPacketTime > 3000) return;
        String chatMessage = "§7(ClientSide) §dAntiCheat §8>> §f" + mc.player.getGameProfile().getName() + " §dfailed §f" + type + " (x§c" + verbose + "§f) §7" + extraInfo;
        String notifyMessage = "§dAntiCheat §8>> §f" + mc.player.getGameProfile().getName() + " §dfailed §f" + type + " (x§c" + verbose + "§f) §7" + extraInfo;
        if (notify.getValue()) {
            mc.player.sendMessage(Text.literal(chatMessage), false);
            Managers.NOTIFICATION.publicity("", notifyMessage, 3, Notification.Type.WARNING);
        }
        if (song.getValue()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
        }
    }
}
