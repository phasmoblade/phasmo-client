package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.utility.render.TextureStorage;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.events.impl.EventPlayerJump;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.gui.notification.Notification;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Criticals;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.animation.CaptureMark;
import thunder.hack.utility.render.animation.PhasmoMark;
import thunder.hack.utility.render.animation.SkullMark;
import thunder.hack.utility.render.animation.RoundedMark;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class NewAura extends Module {
    // Основные настройки из Aura
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride",false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f,v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    
    // AI Aura настройки из AuraAI
    public final Setting<SettingGroup> aiGroup = new Setting<>("AI Settings", new SettingGroup(false, 0));
    public final Setting<Float> rotationSmoothness = new Setting<>("Rotation Smoothness", 0.0f, -1.0f, 3.0f).addToGroup(aiGroup);
    public final Setting<Float> rotationSpeed = new Setting<>("Rotation Speed", 0.0f, -5.0f, 5.0f).addToGroup(aiGroup);
    public final Setting<SprintMode> sprintMode = new Setting<>("Sprint Mode", SprintMode.Default).addToGroup(aiGroup);
    public final Setting<Boolean> dropSprint = new Setting<>("DropSprint", true, v -> sprintMode.getValue() != SprintMode.Default).addToGroup(aiGroup);
    public final Setting<Boolean> returnSprint = new Setting<>("ReturnSprint", true, v -> sprintMode.getValue() != SprintMode.Default).addToGroup(aiGroup);
    
    // ESP настройки
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHackV2);
    public final Setting<SettingGroup> espGroup = new Setting<>("ESPSettings", new SettingGroup(false, 0), v -> esp.getValue() == ESP.ThunderHackV2);
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 100, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Integer> espFactor = new Setting<>("ESPFactor", 8, 1, 100, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> espShaking = new Setting<>("ESPShaking", 1.8f, 1.5f, 100f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> espAmplitude = new Setting<>("ESPAmplitude", 3f, 0.1f, 100f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    
    // Ghost настройки из AuraAI
    public final Setting<Float> ghostSize = new Setting<>("Ghost Size", 0.2f, 0.1f, 2.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> ghostRadius = new Setting<>("Ghost Radius", 0.75f, 0.1f, 3.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> ghostSpeed = new Setting<>("Ghost Speed", 20.0f, 1.0f, 100.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Integer> ghostCount = new Setting<>("Ghost Count", 3, 1, 10, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    
    // Red on Impact
    public final Setting<Boolean> redOnImpact = new Setting<>("Red On Impact", true, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> hitColorDuration = new Setting<>("Hit Color Duration", 350.0f, 100.0f, 2000.0f, v -> esp.getValue() == ESP.ThunderHackV2 && redOnImpact.getValue()).addToGroup(espGroup);
    
    // Alpha animation
    public final Setting<Float> alphaAnimation = new Setting<>("Alpha Animation", 350.0f, 100.0f, 1000.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> maxAlpha = new Setting<>("Max Alpha", 255.0f, 50.0f, 255.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> alphaFactor = new Setting<>("Alpha Factor", 3.0f, 0.1f, 10.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> minSizeFactor = new Setting<>("Min Size Factor", 0.3f, 0.1f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    
    // Physics
    public final Setting<Float> springConstant = new Setting<>("Spring Constant", 0.1f, 0.01f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> dampingFactor = new Setting<>("Damping Factor", 0.8f, 0.1f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> attractionStrength = new Setting<>("Attraction Strength", 0.05f, 0.01f, 0.5f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    
    // MoveFix настройки из AuraAI
    public final Setting<SettingGroup> moveFixGroup = new Setting<>("MoveFix", new SettingGroup(false, 0));
    public final Setting<MoveFix> moveFix = new Setting<>("MoveFix", MoveFix.Off).addToGroup(moveFixGroup);
    public final Setting<Float> moveFixSpeed = new Setting<>("MoveFix Speed", 0.1f, 0.01f, 1.0f, v -> moveFix.getValue() != MoveFix.Off).addToGroup(moveFixGroup);
    public final Setting<Float> moveFixRange = new Setting<>("MoveFix Range", 3.0f, 1.0f, 10.0f, v -> moveFix.getValue() != MoveFix.Off).addToGroup(moveFixGroup);
    
    // Остальные настройки из Aura
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false); // AI Aura не нуждается в видимой наводке
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);

    // Переменные
    public static Entity target;
    public static Entity auraAITarget;
    
    // AI Aura переменные
    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private float currentYaw = 0f;
    private float currentPitch = 0f;
    private boolean isSprinting = false;
    private long lastAttackTime = 0;
    
    // MoveFix переменные
    public float fixRotation;
    private float prevYaw, prevPitch;
    
    // Ghost particles
    private long lastHitTime = 0;
    private long redImpactStartTime = 0;
    
    // Остальные переменные из Aura
    public float pitchAcceleration = 1f;
    private Vec3d rotationPoint = Vec3d.ZERO;
    private Vec3d rotationMotion = Vec3d.ZERO;
    private int hitTicks;
    private int trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer();
    private final Timer pauseTimer = new Timer();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    public NewAura() {
        super("AuraAI", Category.COMBAT);
    }

    // Enums
    public enum ESP {
        Off, ThunderHack, NurikZapen, PhasmoZapen, Skull, Rounded, CelkaPasta, ThunderHackV2
    }

    public enum SprintMode {
        Default,
        Packet,
        ResetAfterHit
    }

    public enum MoveFix {
        Off,
        Focused,
        Free
    }

    public enum WallsBypass {
        Off, V1, V2, V3
    }

    public enum RayTrace {
        OFF, OnlyTarget, AllEntities
    }

    public enum Sort {
        LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV
    }

    public enum Switch {
        Normal, None, Silent
    }


    private float getRange(){
        return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
    }
    
    private float getWallRange(){
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue();
    }

    public void onRender3D(MatrixStack stack) {
        if (target != null && esp.getValue() == ESP.ThunderHackV2) {
            renderCustomGhosts(stack, target);
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled()) return; // ВАЖНО: Проверяем включен ли модуль!
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            Entity entity = Criticals.getEntity(packet);
            if (entity != null && entity instanceof LivingEntity) {
                lastHitTime = System.currentTimeMillis();
                redImpactStartTime = System.currentTimeMillis();
                lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        // Decrease hit ticks
        if (hitTicks > 0) hitTicks--;
        
        // Find target
        auraAITarget = findTarget();
        target = auraAITarget;
        
        if (auraAITarget != null) {
            // AI Aura logic
            updateAITargeting();
            handleSprint();
        }
        
        // MoveFix
        if (moveFix.getValue() != MoveFix.Off) {
            fix(event);
        }
        
        // Original Aura logic
        auraLogic();
    }

    @EventHandler
    public void onJump(EventPlayerJump event) {
        if (moveFix.getValue() != MoveFix.Off) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(EventPlayerTravel event) {
        if (moveFix.getValue() != MoveFix.Off) {
            modifyVelocity(event);
        }
    }

    @EventHandler
    public void onKeyInput(EventKeyboardInput event) {
        if (moveFix.getValue() != MoveFix.Off) {
            fixMove();
        }
    }

    @Override
    public void onEnable() {
        target = null;
        auraAITarget = null;
        lookingAtHitbox = false;
        rotationPoint = Vec3d.ZERO;
        rotationMotion = Vec3d.ZERO;
        delayTimer.reset();
    }

    @Override
    public void onDisable() {
        target = null;
        auraAITarget = null;
    }

    // AI Aura methods
    private void updateAITargeting() {
        if (auraAITarget == null) return;
        
        // Check if target is in range
        double distance = mc.player.distanceTo(auraAITarget);
        if (distance > attackRange.getValue()) return;
        
        // Check walls bypass
        if (wallsBypass.getValue() != WallsBypass.Off && distance > wallRange.getValue()) {
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d targetPos = auraAITarget.getPos().add(0, auraAITarget.getHeight() / 2f, 0);
            
            BlockHitResult hitResult = mc.world.raycast(new RaycastContext(playerPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hitResult.getType() != HitResult.Type.MISS) {
                return; // Blocked by wall
            }
        }
        
        // Calculate target angles
        Vec3d targetPos = auraAITarget.getPos().add(0, auraAITarget.getHeight() / 2f, 0);
        Vec3d playerPos = mc.player.getEyePos();
        
        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;
        
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));
        
        // Smooth rotation
        float smoothness = rotationSmoothness.getValue();
        float speed = rotationSpeed.getValue();
        
        if (smoothness > 0) {
            float deltaYaw = wrapDegrees(targetYaw - currentYaw);
            float deltaPitch = targetPitch - currentPitch;
            
            currentYaw += deltaYaw * (smoothness * 0.1f);
            currentPitch += deltaPitch * (smoothness * 0.1f);
        } else {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
        }
        
        // Apply speed
        if (speed != 0) {
            float speedFactor = 1f + (speed * 0.1f);
            currentYaw *= speedFactor;
            currentPitch *= speedFactor;
        }
        
        // AI Aura не поворачивает камеру - нейросеть управляет наведением
        // Логика наведения остается для внутренних расчетов
        
        // Совместимость с Aura.java - наводка только если clientLook включен
        if (clientLook.getValue()) {
            mc.player.setYaw(currentYaw);
            mc.player.setPitch(currentPitch);
        }
    }

    private void handleSprint() {
        if (sprintMode.getValue() == SprintMode.Default) return;
        
        boolean shouldSprint = mc.player.distanceTo(auraAITarget) <= attackRange.getValue();
        
        if (sprintMode.getValue() == SprintMode.Packet) {
            if (shouldSprint && !isSprinting) {
                if (returnSprint.getValue()) {
                    mc.player.setSprinting(true);
                    isSprinting = true;
                }
            } else if (!shouldSprint && isSprinting) {
                if (dropSprint.getValue()) {
                    mc.player.setSprinting(false);
                    isSprinting = false;
                }
            }
        } else if (sprintMode.getValue() == SprintMode.ResetAfterHit) {
            if (shouldSprint) {
                if (returnSprint.getValue()) {
                    mc.player.setSprinting(true);
                    isSprinting = true;
                }
            } else if (System.currentTimeMillis() - lastAttackTime > 1000) {
                if (dropSprint.getValue()) {
                    mc.player.setSprinting(false);
                    isSprinting = false;
                }
            }
        }
    }

    // MoveFix methods
    private void modifyVelocity(EventPlayerTravel event) {
        if (moveFix.getValue() == MoveFix.Off) return;
        
        Vec3d velocity = event.getmVec();
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        
        if (speed > moveFixSpeed.getValue()) {
            float factor = moveFixSpeed.getValue() / speed;
            event.setmVec(new Vec3d(velocity.x * factor, velocity.y, velocity.z * factor));
        }
    }

    private void fix(PlayerUpdateEvent event) {
        if (moveFix.getValue() == MoveFix.Off) return;
        applyMoveFix();
    }

    private void fixMove() {
        if (moveFix.getValue() == MoveFix.Off) return;
        applyMoveFix();
    }
    
    private void applyMoveFix() {
        if (auraAITarget != null) {
            double deltaX = auraAITarget.getX() - mc.player.getX();
            double deltaZ = auraAITarget.getZ() - mc.player.getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            if (distance <= moveFixRange.getValue()) {
                float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
                float targetPitch = (float) (-Math.atan2(auraAITarget.getY() - mc.player.getY(), distance) * 180.0 / Math.PI);
                
                // MoveFix не поворачивает камеру - только для внутренних расчетов
                // Нейросеть управляет наведением
                
                prevYaw = mc.player.getYaw();
                prevPitch = mc.player.getPitch();
            }
        }
    }


    // Original Aura methods (simplified)
    public void auraLogic() {
        if (!haveWeapon()) {
            target = null;
            return;
        }
        
        if (target == null) return;
        
        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue())
            mc.player.jump();
        
        boolean readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        calcRotations(autoCrit());
        
        if (readyForAttack) {
            if (shieldBreaker(false))
                return;
            
            boolean[] playerState = preAttack();
            if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD))
                attack();
            
            postAttack(playerState[0], playerState[1]);
        }
    }

    private boolean haveWeapon() {
        Item handItem = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            boolean hasValidWeapon = handItem instanceof SwordItem || handItem instanceof AxeItem || handItem instanceof TridentItem || handItem instanceof MaceItem;
            
            if (switchMode.getValue() == Switch.None) {
                return hasValidWeapon;
            } else {
                return hasValidWeapon || (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found() || InventoryUtility.getMaceHotBar().found());
            }
        }
        return true;
    }

    private boolean skipRayTraceCheck() {
        return true; // AI Aura не нуждается в ray trace проверках
    }

    public void attack() {
        ModuleManager.criticals.doCrit();
        int prevSlot = switchMethod();
        mc.interactionManager.attackEntity(mc.player, target);
        swingHand();
        hitTicks = getHitTicks();
        if (prevSlot != -1)
            InventoryUtility.switchTo(prevSlot);
    }

    private boolean[] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking)
            sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));

        boolean sprint = Core.serverSprint;
        if (sprint && dropSprint.getValue())
            disableSprint();

        return new boolean[]{blocking, sprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (sprint && returnSprint.getValue() && dropSprint.getValue())
            enableSprint();

        if (block)
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
    }

    private void disableSprint() {
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }

    private void enableSprint() {
        mc.player.setSprinting(true);
        mc.options.sprintKey.setPressed(true);
        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
    }

    private int switchMethod() {
        int prevSlot = -1;
        SearchInvResult swordResult = InventoryUtility.getSwordHotBar();
        if (swordResult.found() && switchMode.getValue() != Switch.None) {
            if (switchMode.getValue() == Switch.Silent)
                prevSlot = mc.player.getInventory().selectedSlot;
            swordResult.switchTo();
        }
        return prevSlot;
    }

    private int getHitTicks() {
        return oldDelay.getValue().isEnabled() ? 1 + (int) (20f / random(minCPS.getValue(), maxCPS.getValue())) : 11;
    }

    private void swingHand() {
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean autoCrit() {
        if (hitTicks > 0) return false;
        if (mc.player.isUsingItem() && pauseWhileEating.getValue()) return false;
        if (getAttackCooldown() < 0.9f && !oldDelay.getValue().isEnabled()) return false;
        
        if (!mc.options.jumpKey.isPressed() && !onlySpace.getValue() && !autoJump.getValue())
            return true;
        
        return !mc.player.isOnGround() && mc.player.fallDistance > 0.15f;
    }

    private float getAttackCooldown() {
        return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + 0.5f) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
    }

    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * ThunderHack.TICK_TIMER * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f)));
    }

    private boolean shieldBreaker(boolean instant) {
        int axeSlot = InventoryUtility.getAxe().slot();
        if (axeSlot == -1) return false;
        if (!shieldBreaker.getValue()) return false;
        if (!(target instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) target).isUsingItem() && !instant) return false;
        if (((PlayerEntity) target).getOffHandStack().getItem() != Items.SHIELD && ((PlayerEntity) target).getMainHandStack().getItem() != Items.SHIELD)
            return false;

        if (axeSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.interactionManager.attackEntity(mc.player, target);
            swingHand();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, target);
            swingHand();
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
        hitTicks = 10;
        return true;
    }

    private void calcRotations(boolean ready) {
        // AI Aura не нуждается в ручных ротациях - нейросеть управляет поворотом
        lookingAtHitbox = true;
    }

    public Entity findTarget() {
        List<LivingEntity> first_stage = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (skipEntity(ent)) continue;
            if (!(ent instanceof LivingEntity)) continue;
            first_stage.add((LivingEntity) ent);
        }

        return switch (sort.getValue()) {
            case LowestDistance ->
                    first_stage.stream().min(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
            case HighestDistance ->
                    first_stage.stream().max(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
            case FOV -> first_stage.stream().min(Comparator.comparing(this::getFOVAngle)).orElse(null);
            case LowestHealth ->
                    first_stage.stream().min(Comparator.comparing(e -> (e.getHealth() + e.getAbsorptionAmount()))).orElse(null);
            case HighestHealth ->
                    first_stage.stream().max(Comparator.comparing(e -> (e.getHealth() + e.getAbsorptionAmount()))).orElse(null);
            default -> first_stage.stream().min(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
        };
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent)) return true;
        if (ent.isDead() || !entity.isAlive()) return true;
        if (entity instanceof ArmorStandEntity) return true;
        if (entity instanceof CatEntity) return true;
        if (!InteractionUtility.isVecInFOV(ent.getPos(), fov.getValue())) return true;

        if (entity instanceof PlayerEntity player) {
            if (player == mc.player || Managers.FRIEND.isFriend(player))
                return true;
            if (player.isCreative())
                return true;
            if (player.isInvisible())
                return true;
        }

        return mc.player.distanceTo(entity) > getRange();
    }

    private float getFOVAngle(@NotNull LivingEntity e) {
        double difX = e.getX() - mc.player.getX();
        double difZ = e.getZ() - mc.player.getZ();
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        return Math.abs(yaw - MathHelper.wrapDegrees(mc.player.getYaw()));
    }

    // Custom Ghost rendering with all settings
    private void renderCustomGhosts(MatrixStack stack, Entity target) {
        Camera camera = mc.gameRenderer.getCamera();
        
        double tPosX = Render2DEngine.interpolate(target.prevX, target.getX(), Render3DEngine.getTickDelta()) - camera.getPos().x;
        double tPosY = Render2DEngine.interpolate(target.prevY, target.getY(), Render3DEngine.getTickDelta()) - camera.getPos().y;
        double tPosZ = Render2DEngine.interpolate(target.prevZ, target.getZ(), Render3DEngine.getTickDelta()) - camera.getPos().z;
        float iAge = (float) Render2DEngine.interpolate(target.age - 1, target.age, Render3DEngine.getTickDelta());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TextureStorage.firefly);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        boolean canSee = mc.player.canSee(target);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        // Use all our settings
        int length = espLength.getValue();
        int factor = espFactor.getValue();
        float shaking = espShaking.getValue();
        float amplitude = espAmplitude.getValue();
        float ghostSize = this.ghostSize.getValue();
        float ghostRadius = this.ghostRadius.getValue();
        float ghostSpeed = this.ghostSpeed.getValue().floatValue();
        int ghostCount = this.ghostCount.getValue();
        
        // Red on Impact effect
        boolean isRed = redOnImpact.getValue() && (System.currentTimeMillis() - lastHitTime) < hitColorDuration.getValue();
        
        // Alpha animation
        float alpha = maxAlpha.getValue();
        if (alphaAnimation.getValue() > 0) {
            float alphaTime = (System.currentTimeMillis() % (long) alphaAnimation.getValue().floatValue()) / alphaAnimation.getValue();
            alpha = (float) (maxAlpha.getValue() * Math.sin(alphaTime * Math.PI * alphaFactor.getValue()));
        }

        for (int j = 0; j < ghostCount; j++) {
            for (int i = 0; i <= length; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge * ghostSpeed) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = ((float) i / length);
                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                
                // Use ghostRadius for positioning
                matrices.translate(tPosX + Math.cos(radians) * ghostRadius, (tPosY + 1 + sinQuad), tPosZ + Math.sin(radians) * ghostRadius);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                
                // Color with red impact effect
                int color;
                if (isRed) {
                    color = new java.awt.Color(255, 0, 0, (int) alpha).getRGB();
                } else {
                    color = Render2DEngine.applyOpacity(HudEditor.getColor((int) (180 * offset)), offset * alpha / 255f).getRGB();
                }
                
                // Use ghostSize for scaling
                float scale = Math.max(ghostSize * (offset), ghostSize * minSizeFactor.getValue());
                
                buffer.vertex(matrix, -scale, scale, 0).texture(0f, 1f).color(color);
                buffer.vertex(matrix, scale, scale, 0).texture(1f, 1f).color(color);
                buffer.vertex(matrix, scale, -scale, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix, -scale, -scale, 0).texture(0, 0).color(color);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.disableBlend();
    }

}
