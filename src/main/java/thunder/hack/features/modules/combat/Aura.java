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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.gui.notification.Notification;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.animation.CaptureMark;
import thunder.hack.utility.render.animation.PhasmoMark;
import thunder.hack.utility.render.animation.SkullMark;
import thunder.hack.utility.render.animation.RoundedMark;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import org.joml.Matrix4f;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.features.modules.client.Rotations;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride",false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f,v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Integer> interactTicks = new Setting<>("InteractTicks", 3, 1, 10, v -> rotationMode.getValue() == Mode.Interact);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);
    
    // Multipoint настройки из AuraAI
    public final Setting<Boolean> multipointAim = new Setting<>("Multipoint Aim", true);
    public final Setting<AimPoint> aimPoint = new Setting<>("Aim Point", AimPoint.Random, v -> multipointAim.getValue());
    public final Setting<Boolean> randomizeAimPoint = new Setting<>("Randomize Aim Point", true, v -> multipointAim.getValue() && aimPoint.getValue() == AimPoint.Random);
    public final Setting<Integer> aimPointChangeFrequency = new Setting<>("Aim Point Change Frequency", 60, 20, 200, v -> multipointAim.getValue() && randomizeAimPoint.getValue());

    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack);
    public final Setting<SettingGroup> espGroup = new Setting<>("ESPSettings", new SettingGroup(false, 0), v -> esp.is(ESP.ThunderHackV2));
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 40, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Integer> espFactor = new Setting<>("ESPFactor", 8, 1, 20, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> espShaking = new Setting<>("ESPShaking", 1.8f, 1.5f, 10f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> espAmplitude = new Setting<>("ESPAmplitude", 3f, 0.1f, 8f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    
    // Дополнительные настройки ThunderHackV2 ESP из AuraAI
    public final Setting<Float> ghostSize = new Setting<>("Ghost Size", 0.2f, 0.1f, 2.0f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> ghostRadius = new Setting<>("Ghost Radius", 0.75f, 0.1f, 3.0f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> ghostSpeed = new Setting<>("Ghost Speed", 2.0f, 0.1f, 10.0f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Integer> ghostCount = new Setting<>("Ghost Count", 3, 1, 10, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    
    // Red on Impact
    public final Setting<Boolean> redOnImpact = new Setting<>("Red On Impact", true, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> hitColorDuration = new Setting<>("Hit Color Duration", 350.0f, 100.0f, 2000.0f, v -> esp.is(ESP.ThunderHackV2) && redOnImpact.getValue()).addToGroup(espGroup);
    
    // Alpha Animation
    public final Setting<Boolean> enableAlphaAnimation = new Setting<>("Enable Alpha Animation", false, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> alphaAnimation = new Setting<>("Alpha Animation", 350.0f, 100.0f, 1000.0f, v -> esp.is(ESP.ThunderHackV2) && enableAlphaAnimation.getValue()).addToGroup(espGroup);
    public final Setting<Float> maxAlpha = new Setting<>("Max Alpha", 255.0f, 50.0f, 255.0f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> alphaFactor = new Setting<>("Alpha Factor", 3.0f, 0.1f, 10.0f, v -> esp.is(ESP.ThunderHackV2) && enableAlphaAnimation.getValue()).addToGroup(espGroup);
    public final Setting<Float> minSizeFactor = new Setting<>("Min Size Factor", 0.3f, 0.1f, 1.0f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);

    /*   ADVANCED   */
    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> randomHitDelay = new Setting<>("RandomHitDelay", false).addToGroup(advanced);
    public final Setting<Boolean> pauseInInventory = new Setting<>("PauseInInventory", true).addToGroup(advanced);
    public final Setting<Boolean> dropSprint = new Setting<>("DropSprint", true).addToGroup(advanced);
    public final Setting<Boolean> returnSprint = new Setting<>("ReturnSprint", true, v -> dropSprint.getValue()).addToGroup(advanced);
    
    // Улучшенная система спринта из AuraAI
    public final Setting<SprintMode> sprintMode = new Setting<>("Sprint Mode", SprintMode.Off);
    public final Setting<Boolean> sprintResetAfterHit = new Setting<>("Sprint Reset After Hit", true, v -> sprintMode.getValue() == SprintMode.ResetAfterHit);
    public final Setting<Float> sprintResetDelay = new Setting<>("Sprint Reset Delay (ms)", 100f, 50f, 500f, v -> sprintMode.getValue() == SprintMode.ResetAfterHit && sprintResetAfterHit.getValue());
    
    // FOV визуализация
    public final Setting<Boolean> showFOV = new Setting<>("ShowFOV", false);
    public final Setting<Integer> fovColor = new Setting<>("FOV Color", 0xFF00FF00, v -> showFOV.getValue());
    
    // MoveFix настройки (зависят от Rotations.java)
    public final Setting<Boolean> enableMoveFix = new Setting<>("Enable MoveFix", true);
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Boolean> grimRayTrace = new Setting<>("GrimRayTrace", true).addToGroup(advanced);
    public final Setting<Boolean> unpressShield = new Setting<>("UnpressShield", true).addToGroup(advanced);
    public final Setting<Boolean> deathDisable = new Setting<>("DisableOnDeath", true).addToGroup(advanced);
    public final Setting<Boolean> tpDisable = new Setting<>("TPDisable", false).addToGroup(advanced);
    public final Setting<Boolean> pullDown = new Setting<>("FastFall", false).addToGroup(advanced);
    public final Setting<Boolean> onlyJumpBoost = new Setting<>("OnlyJumpBoost", false, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<Float> pullValue = new Setting<>("PullValue", 3f, 0f, 20f, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand).addToGroup(advanced);
    public final Setting<Resolver> resolver = new Setting<>("Resolver", Resolver.Advantage).addToGroup(advanced);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20, v -> resolver.is(Resolver.BackTrack)).addToGroup(advanced);
    public final Setting<Boolean> resolverVisualisation = new Setting<>("ResolverVisualisation", false, v -> !resolver.is(Resolver.Off)).addToGroup(advanced);
    public final Setting<AccelerateOnHit> accelerateOnHit = new Setting<>("AccelerateOnHit", AccelerateOnHit.Off).addToGroup(advanced);
    public final Setting<Integer> minYawStep = new Setting<>("MinYawStep", 65, 1, 180).addToGroup(advanced);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(advanced);
    public final Setting<Float> aimedPitchStep = new Setting<>("AimedPitchStep", 1f, 0f, 90f).addToGroup(advanced);
    public final Setting<Float> maxPitchStep = new Setting<>("MaxPitchStep", 8f, 1f, 90f).addToGroup(advanced);
    public final Setting<Float> pitchAccelerate = new Setting<>("PitchAccelerate", 1.65f, 1f, 10f).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Float> attackBaseTime = new Setting<>("AttackBaseTime", 0.5f, 0f, 2f).addToGroup(advanced);
    public final Setting<Integer> attackTickLimit = new Setting<>("AttackTickLimit", 11, 0, 20).addToGroup(advanced);
    public final Setting<Float> critFallDistance = new Setting<>("CritFallDistance", 0f, 0f, 1f).addToGroup(advanced);


    /*   TARGETS   */
    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targets);
    public final Setting<Boolean> Mobs = new Setting<>("Mobs", true).addToGroup(targets);
    public final Setting<Boolean> Animals = new Setting<>("Animals", true).addToGroup(targets);
    public final Setting<Boolean> Villagers = new Setting<>("Villagers", true).addToGroup(targets);
    public final Setting<Boolean> Slimes = new Setting<>("Slimes", true).addToGroup(targets);
    public final Setting<Boolean> hostiles = new Setting<>("Hostiles", true).addToGroup(targets);
    public final Setting<Boolean> onlyAngry = new Setting<>("OnlyAngryHostiles", true, v -> hostiles.getValue()).addToGroup(targets);
    public final Setting<Boolean> Projectiles = new Setting<>("Projectiles", true).addToGroup(targets);
    public final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvisibleEntities", false).addToGroup(targets);
    public final Setting<Boolean> ignoreNamed = new Setting<>("IgnoreNamed", false).addToGroup(targets);
    public final Setting<Boolean> ignoreTeam = new Setting<>("IgnoreTeam", false).addToGroup(targets);
    public final Setting<Boolean> ignoreCreative = new Setting<>("IgnoreCreative", true).addToGroup(targets);
    public final Setting<Boolean> ignoreNaked = new Setting<>("IgnoreNaked", false).addToGroup(targets);
    public final Setting<Boolean> ignoreShield = new Setting<>("AttackShieldingEntities", true).addToGroup(targets);

    /*   AUTOMACE   */
    public final Setting<SettingGroup> autoMace = new Setting<>("AutoMace", new SettingGroup(false, 0));
    public final Setting<Boolean> enableAutoMace = new Setting<>("EnableAutoMace", false).addToGroup(autoMace);
    public final Setting<AutoMaceMode> autoMaceMode = new Setting<>("AutoMaceMode", AutoMaceMode.LITE, v -> enableAutoMace.getValue()).addToGroup(autoMace);
    public final Setting<Float> minHeight = new Setting<>("MinHeight", 3.0f, 0.5f, 10.0f, v -> enableAutoMace.getValue()).addToGroup(autoMace);
    public final Setting<Float> maxDistance = new Setting<>("MaxDistance", 4.0f, 1.0f, 8.0f, v -> enableAutoMace.getValue()).addToGroup(autoMace);
    public final Setting<Boolean> onlyWhenFalling = new Setting<>("OnlyWhenFalling", true, v -> enableAutoMace.getValue()).addToGroup(autoMace);
    public final Setting<Boolean> returnToSword = new Setting<>("ReturnToSword", true, v -> enableAutoMace.getValue()).addToGroup(autoMace);
    public final Setting<Integer> maceHoldTime = new Setting<>("MaceHoldTime", 1000, 100, 5000, v -> enableAutoMace.getValue()).addToGroup(autoMace);

    // Настройки для режима Strong
    public final Setting<Float> strongMinHeight = new Setting<>("StrongMinHeight", 5.0f, 1.0f, 10.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Float> strongMaxDistance = new Setting<>("StrongMaxDistance", 3.0f, 1.0f, 8.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Integer> strongSwitchDelay = new Setting<>("StrongSwitchDelay", 500, 200, 2000, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Integer> strongAttackDelay = new Setting<>("StrongAttackDelay", 300, 100, 1000, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Boolean> strongRandomizeTiming = new Setting<>("StrongRandomizeTiming", true, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);

    // Дополнительные настройки для максимальной легитности
    public final Setting<Boolean> strongHumanBehavior = new Setting<>("StrongHumanBehavior", true, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Float> strongMissChance = new Setting<>("StrongMissChance", 0.15f, 0.0f, 0.5f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongHumanBehavior.getValue()).addToGroup(autoMace);
    public final Setting<Integer> strongMaxAttacksPerSession = new Setting<>("StrongMaxAttacks", 1, 1, 3, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Boolean> strongLookAtTarget = new Setting<>("StrongLookAtTarget", false, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Float> strongLookSpeed = new Setting<>("StrongLookSpeed", 0.1f, 0.1f, 1.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongLookAtTarget.getValue()).addToGroup(autoMace);
    public final Setting<Boolean> strongPauseOnMovement = new Setting<>("StrongPauseOnMovement", true, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);

    // Дополнительные настройки плавности наводки
    public final Setting<Boolean> smoothAiming = new Setting<>("SmoothAiming", true, v -> enableAutoMace.getValue() && strongLookAtTarget.getValue()).addToGroup(autoMace);
    public final Setting<Float> aimSmoothness = new Setting<>("AimSmoothness", 0.8f, 0.1f, 2.0f, v -> enableAutoMace.getValue() && strongLookAtTarget.getValue() && smoothAiming.getValue()).addToGroup(autoMace);
    public final Setting<Boolean> adaptiveSpeed = new Setting<>("AdaptiveSpeed", true, v -> enableAutoMace.getValue() && strongLookAtTarget.getValue() && smoothAiming.getValue()).addToGroup(autoMace);
    public final Setting<Float> jitterIntensity = new Setting<>("JitterIntensity", 0.3f, 0.0f, 1.0f, v -> enableAutoMace.getValue() && strongLookAtTarget.getValue() && smoothAiming.getValue()).addToGroup(autoMace);

    public static Entity target;

    // AutoMace переменные
    private int previousSlot = -1;
    private boolean wasUsingMace = false;
    private long maceSwitchTime = 0;
    private boolean maceAttackDone = false;
    
    // Strong Mode переменные
    private long lastSwitchTime = 0;
    private long lastMovementTime = 0;
    private int strongAttackCount = 0;
    private boolean wasMoving = false;
    private boolean strongModeReady = false;
    private long lastMaceAttackTime = 0;
    
    // Look variables
    private float currentLookYaw = 0f;
    private float currentLookPitch = 0f;
    private float targetLookYaw = 0f;
    private float targetLookPitch = 0f;
    private long lastLookUpdate = 0;
    private boolean shouldMissNext = false;

    public float rotationYaw;
    public float rotationPitch;
    public float pitchAcceleration = 1f;

    private Vec3d rotationPoint = Vec3d.ZERO;
    private Vec3d rotationMotion = Vec3d.ZERO;

    private int hitTicks;
    private int trackticks;
    private boolean lookingAtHitbox;
    
    // Multipoint переменные
    private AimPoint currentAimPoint = AimPoint.Random;
    private int aimPointChangeCounter = 0;
    private long lastAimPointChange = 0;
    
    // Sprint переменные
    private long lastHitTime = 0;
    private boolean wasSprintingBeforeHit = false;
    
    // Переменные для ESP анимации
    private long lastRenderTime = 0;
    private float lastAlpha = 255f;

    private final Timer delayTimer = new Timer();
    private final Timer pauseTimer = new Timer();

    public Box resolvedBox;
    static boolean wasTargeted = false;

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    private float getRange(){
        return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
    }
    private float getWallRange(){
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue();
    }

    public void auraLogic() {
        if (!haveWeapon()) {
            target = null;
            return;
        }

        handleKill();
        updateTarget();

        if (target == null) {
            return;
        }

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue())
            mc.player.jump();

        boolean readyForAttack;

        if (grimRayTrace.getValue()) {
            readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
            calcRotations(autoCrit());
        } else {
            calcRotations(autoCrit());
            readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        }

        if (readyForAttack) {
            if (shieldBreaker(false))
                return;

            boolean[] playerState = preAttack();
            if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD) || ignoreShield.getValue())
                attack();

            postAttack(playerState[0], playerState[1]);
        }
    }

    private boolean haveWeapon() {
        Item handItem = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            if (switchMode.getValue() == Switch.None) {
                return handItem instanceof SwordItem || handItem instanceof AxeItem || handItem instanceof TridentItem;
            } else {
                return (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found());
            }
        }
        return true;
    }

    private boolean skipRayTraceCheck() {
        return rotationMode.getValue() == Mode.None || rayTrace.getValue() == RayTrace.OFF
                || rotationMode.is(Mode.Grim)
                || (rotationMode.is(Mode.Interact) && (interactTicks.getValue() <= 1
                || mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext()));
    }

    public void attack() {
        // Sprint логика перед атакой
        handleSprintBeforeAttack();
        
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        int prevSlot = switchMethod();
        mc.interactionManager.attackEntity(mc.player, target);
        Criticals.cancelCrit = false;
        swingHand();
        hitTicks = getHitTicks();
        if (prevSlot != -1)
            InventoryUtility.switchTo(prevSlot);
            
        // Отслеживание удара для Red On Impact
        lastHitTime = System.currentTimeMillis();
            
        // Sprint логика после атаки
        handleSprintAfterAttack();
    }

    private boolean @NotNull [] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue())
            sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));

        boolean sprint = Core.serverSprint;
        if (sprint && dropSprint.getValue())
            disableSprint();

        if (rotationMode.is(Mode.Grim))
            sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));

        return new boolean[]{blocking, sprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (sprint && returnSprint.getValue() && dropSprint.getValue())
            enableSprint();

        if (block && unpressShield.getValue())
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));

        if (rotationMode.is(Mode.Grim))
            sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
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

    public void resolvePlayers() {
        if (resolver.not(Resolver.Off))
            for (PlayerEntity player : mc.world.getPlayers())
                if (player instanceof OtherClientPlayerEntity)
                    ((IOtherClientPlayerEntity) player).resolve(resolver.getValue());
    }

    public void restorePlayers() {
        if (resolver.not(Resolver.Off))
            for (PlayerEntity player : mc.world.getPlayers())
                if (player instanceof OtherClientPlayerEntity)
                    ((IOtherClientPlayerEntity) player).releaseResolver();
    }

    public void handleKill() {
        if (target instanceof LivingEntity && (((LivingEntity) target).getHealth() <= 0 || ((LivingEntity) target).isDead()))
            Managers.NOTIFICATION.publicity("Aura", isRu() ? "Цель успешно нейтрализована!" : "Target successfully neutralized!", 3, Notification.Type.SUCCESS);
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
        return oldDelay.getValue().isEnabled() ? 1 + (int) (20f / random(minCPS.getValue(), maxCPS.getValue())) : (shouldRandomizeDelay() ? (int) MathUtility.random(11, 13) : attackTickLimit.getValue());
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000))
            return;

        if (mc.player.isUsingItem() && pauseWhileEating.getValue())
            return;
        if(pauseBaritone.getValue() && ThunderHack.baritone){
            boolean isTargeted = (target != null);
            if (isTargeted && !wasTargeted) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
                wasTargeted = true;
            } else if (!isTargeted && wasTargeted) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
                wasTargeted = false;
            }
        }

        resolvePlayers();
        auraLogic();
        
        // AutoMace logic
        handleAutoMace();
        
        restorePlayers();
        hitTicks--;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000))
            return;

        if (mc.player.isUsingItem() && pauseWhileEating.getValue())
            return;

        if (!haveWeapon())
            return;

        if (target != null && rotationMode.getValue() != Mode.None && rotationMode.getValue() != Mode.Grim) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        } else {
            rotationYaw = mc.player.getYaw();
            rotationPitch = mc.player.getPitch();
        }

        if (oldDelay.getValue().isEnabled())
            if (minCPS.getValue() > maxCPS.getValue())
                minCPS.setValue(maxCPS.getValue());

        if (target != null && pullDown.getValue() && (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) || !onlyJumpBoost.getValue()))
            mc.player.addVelocity(0f, -pullValue.getValue() / 1000f, 0f);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof PlayerInteractEntityC2SPacket pie && Criticals.getInteractType(pie) != Criticals.InteractType.ATTACK && target != null)
            e.cancel();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status)
            if (status.getStatus() == 30 && status.getEntity(mc.world) != null && target != null && status.getEntity(mc.world) == target)
                Managers.NOTIFICATION.publicity("Aura", isRu() ? ("Успешно сломали щит игроку " + target.getName().getString()) : ("Succesfully destroyed " + target.getName().getString() + "'s shield"), 2, Notification.Type.SUCCESS);

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && tpDisable.getValue())
            disable(isRu() ? "Отключаю из-за телепортации!" : "Disabling due to tp!");

        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3 && pac.getEntity(mc.world) == mc.player && deathDisable.getValue())
            disable(isRu() ? "Отключаю из-за смерти!" : "Disabling due to death!");

        /*
        if (resolver.is(Resolver.BackTrack) && e.getPacket() instanceof CommonPingS2CPacket ping && target != null) {
            Managers.ASYNC.run(() -> mc.executeSync(() -> ping.apply(mc.getNetworkHandler())), backTicks.getValue() * 25L);
            e.cancel();
        }*/
    }

    @Override
    public void onEnable() {
        target = null;
        lookingAtHitbox = false;
        rotationPoint = Vec3d.ZERO;
        
        // Сброс переменных AutoMace
        previousSlot = -1;
        wasUsingMace = false;
        maceSwitchTime = 0;
        maceAttackDone = false;
        lastSwitchTime = 0;
        lastMovementTime = 0;
        strongAttackCount = 0;
        wasMoving = false;
        strongModeReady = false;
        lastMaceAttackTime = 0;
        currentLookYaw = 0f;
        currentLookPitch = 0f;
        targetLookYaw = 0f;
        targetLookPitch = 0f;
        lastLookUpdate = 0;
        shouldMissNext = false;
        rotationMotion = Vec3d.ZERO;
        rotationYaw = mc.player.getYaw();
        rotationPitch = mc.player.getPitch();
        delayTimer.reset();
    }

    private boolean autoCrit() {
        boolean reasonForSkipCrit =
                !smartCrit.getValue().isEnabled()
                        || mc.player.getAbilities().flying
                        || (mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled())
                        || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                        || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                        || Managers.PLAYER.isInWeb();

        if (hitTicks > 0)
            return false;

        if (pauseInInventory.getValue() && Managers.PLAYER.inInventory)
            return false;

        if (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled())
            return false;

        if (ModuleManager.criticals.isEnabled() && ModuleManager.criticals.mode.is(Criticals.Mode.Grim))
            return true;

        boolean mergeWithTargetStrafe = !ModuleManager.targetStrafe.isEnabled() || !ModuleManager.targetStrafe.jump.getValue();
        boolean mergeWithSpeed = !ModuleManager.speed.isEnabled() || mc.player.isOnGround();

        if (!mc.options.jumpKey.isPressed() && mergeWithTargetStrafe && mergeWithSpeed && !onlySpace.getValue() && !autoJump.getValue())
            return true;

        if (mc.player.isInLava() || mc.player.isSubmergedInWater())
            return true;

        if (!mc.options.jumpKey.isPressed() && isAboveWater())
            return true;

        // я хз почему оно не критует когда фд больше 1.14
        if (mc.player.fallDistance > 1 && mc.player.fallDistance < 1.14)
            return false;

        if (!reasonForSkipCrit)
            return !mc.player.isOnGround() && mc.player.fallDistance > (shouldRandomizeFallDistance() ? MathUtility.random(0.15f, 0.7f) : critFallDistance.getValue());
        return true;
    }

    private boolean shieldBreaker(boolean instant) { //todo - Actual value of parameter 'instant' is always 'false'
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

    private void swingHand() {
        switch (attackHand.getValue()) {
            case OffHand -> mc.player.swingHand(Hand.OFF_HAND);
            case MainHand -> mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public boolean isAboveWater() {
        return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER;
    }

    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * ThunderHack.TICK_TIMER * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f)));
    }

    public float getAttackCooldown() {
        return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
    }

    private void updateTarget() {
        Entity candidat = findTarget();

        if (target == null) {
            target = candidat;
            return;
        }

        if (sort.getValue() == Sort.FOV || !lockTarget.getValue())
            target = candidat;

        if (candidat instanceof ProjectileEntity)
            target = candidat;

        if (skipEntity(target))
            target = null;
    }

    private void calcRotations(boolean ready) {
        if (ready) {
            trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext() ? 1 : interactTicks.getValue());
        } else if (trackticks > 0) {
            trackticks--;
        }

        if (target == null)
            return;

        // Multipoint логика
        if (multipointAim.getValue()) {
            updateAimPoint();
        }

        Vec3d targetVec;

        if (mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) targetVec = target.getEyePos();
        else if (multipointAim.getValue()) {
            targetVec = getMultipointTargetPosition();
        } else {
            targetVec = getLegitLook(target);
        }

        if (targetVec == null)
            return;

        pitchAcceleration = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange() + aimRange.getValue(), getRange() + aimRange.getValue(), rayTrace.getValue())
                ? aimedPitchStep.getValue() : pitchAcceleration < maxPitchStep.getValue() ? pitchAcceleration * pitchAccelerate.getValue() : maxPitchStep.getValue();

        float delta_yaw = wrapDegrees((float) wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - mc.player.getZ(), (targetVec.x - mc.player.getX()))) - 90) - rotationYaw) + (wallsBypass.is(WallsBypass.V2) && !ready && !mc.player.canSee(target) ? 20 : 0);
        float delta_pitch = ((float) (-Math.toDegrees(Math.atan2(targetVec.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((targetVec.x - mc.player.getX()), 2) + Math.pow(targetVec.z - mc.player.getZ(), 2))))) - rotationPitch);

        float yawStep = rotationMode.getValue() != Mode.Track ? 360f : random(minYawStep.getValue(), maxYawStep.getValue());
        float pitchStep = rotationMode.getValue() != Mode.Track ? 180f : Managers.PLAYER.ticksElytraFlying > 5 ? 180 : (pitchAcceleration + random(-1f, 1f));


        if (ready)
            switch (accelerateOnHit.getValue()) {
                case Yaw -> yawStep = 180f;
                case Pitch -> pitchStep = 90f;
                case Both -> {
                    yawStep = 180f;
                    pitchStep = 90f;
                }
            }

        if (delta_yaw > 180)
            delta_yaw = delta_yaw - 180;

        float deltaYaw = MathHelper.clamp(MathHelper.abs(delta_yaw), -yawStep, yawStep);

        float deltaPitch = MathHelper.clamp(delta_pitch, -pitchStep, pitchStep);

        float newYaw = rotationYaw + (delta_yaw > 0 ? deltaYaw : -deltaYaw);
        float newPitch = MathHelper.clamp(rotationPitch + deltaPitch, -90.0F, 90.0F);

        double gcdFix = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;

        if (trackticks > 0 || rotationMode.getValue() == Mode.Track) {
            rotationYaw = (float) (newYaw - (newYaw - rotationYaw) % gcdFix);
            rotationPitch = (float) (newPitch - (newPitch - rotationPitch) % gcdFix);
        } else if (rotationMode.getValue() == Mode.GrimSmooth) {
            // GrimSmooth: плавная но быстрая ротация
            float grimSmoothness = 0.15f; // Плавность
            float grimSpeed = 2.5f; // Скорость
            
            rotationYaw += delta_yaw * grimSmoothness * grimSpeed;
            rotationPitch += delta_pitch * grimSmoothness * grimSpeed;
            
            // Ограничиваем pitch
            rotationPitch = MathHelper.clamp(rotationPitch, -90.0F, 90.0F);
        } else {
            rotationYaw = mc.player.getYaw();
            rotationPitch = mc.player.getPitch();
        }

        // Коррекция через Rotations.java (как в AuraAI.java)
        if (enableMoveFix.getValue()) {
            ModuleManager.rotations.fixRotation = rotationYaw;
        } else {
            ModuleManager.rotations.fixRotation = Float.NaN; // Отключаем MoveFix
        }
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public void onRender3D(MatrixStack stack) {
        if (!haveWeapon() || target == null)
            return;

        // FOV визуализация
        if (showFOV.getValue()) {
            renderFOV(stack);
        }

        if ((resolver.is(Resolver.BackTrack) || resolverVisualisation.getValue()) && resolvedBox != null)
            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(resolvedBox, HudEditor.getColor(0), 1));

        switch (esp.getValue()) {
            case CelkaPasta -> Render3DEngine.drawOldTargetEsp(stack, target);
            case NurikZapen -> CaptureMark.render(target);
            case PhasmoZapen -> PhasmoMark.render(target);
            case Skull -> SkullMark.render(target);
            case Rounded -> RoundedMark.render(target);
            case ThunderHackV2 -> renderCustomGhosts(stack, target);
            case ThunderHack -> Render3DEngine.drawTargetEsp(stack, target);
        }

        if (clientLook.getValue() && rotationMode.getValue() != Mode.None) {
            mc.player.setYaw((float) Render2DEngine.interpolate(mc.player.prevYaw, rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.setPitch((float) Render2DEngine.interpolate(mc.player.prevPitch, rotationPitch, Render3DEngine.getTickDelta()));
        }
    }

    @Override
    public void onDisable() {
        target = null;
        
        // Сброс переменных AutoMace
        previousSlot = -1;
        wasUsingMace = false;
        maceSwitchTime = 0;
        maceAttackDone = false;
        lastSwitchTime = 0;
        lastMovementTime = 0;
        strongAttackCount = 0;
        wasMoving = false;
        strongModeReady = false;
        lastMaceAttackTime = 0;
        currentLookYaw = 0f;
        currentLookPitch = 0f;
        targetLookYaw = 0f;
        targetLookPitch = 0f;
        lastLookUpdate = 0;
        shouldMissNext = false;
    }

    public float getSquaredRotateDistance() {
        float dst = getRange();
        dst += aimRange.getValue();
        if ((mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) && target != null) dst += 4f;
        if (ModuleManager.strafe.isEnabled()) dst += 4f;
        if (rotationMode.getValue() != Mode.Track || rayTrace.getValue() == RayTrace.OFF)
            dst = getRange();

        return dst * dst;
    }

    /*
     * Эта хуеверть основанна на приципе "DVD Logo"
     * У нас есть точка и "коробка" (хитбокс цели)
     * Точка летает внутри коробки и отталкивается от стенок с рандомной скоростью и легким джиттером
     * Также выбирает лучшую дистанцию для удара, то есть считает не от центра до центра, а от наших глаз до достигаемых точек хитбокса цели
     * Со стороны не сильно заметно что ты играешь с киллкой, в отличие от аур семейства Wexside
     */

    public Vec3d getLegitLook(Entity target) {

        float minMotionXZ = 0.003f;
        float maxMotionXZ = 0.03f;

        float minMotionY = 0.001f;
        float maxMotionY = 0.03f;

        double lenghtX = target.getBoundingBox().getLengthX();
        double lenghtY = target.getBoundingBox().getLengthY();
        double lenghtZ = target.getBoundingBox().getLengthZ();


        // Задаем начальную скорость точки
        if (rotationMotion.equals(Vec3d.ZERO))
            rotationMotion = new Vec3d(random(-0.05f, 0.05f), random(-0.05f, 0.05f), random(-0.05f, 0.05f));

        rotationPoint = rotationPoint.add(rotationMotion);

        // Сталкиваемся с хитбоксом по X
        if (rotationPoint.x >= (lenghtX - 0.05) / 2f)
            rotationMotion = new Vec3d(-random(minMotionXZ, maxMotionXZ), rotationMotion.getY(), rotationMotion.getZ());

        // Сталкиваемся с хитбоксом по Y
        if (rotationPoint.y >= lenghtY)
            rotationMotion = new Vec3d(rotationMotion.getX(), -random(minMotionY, maxMotionY), rotationMotion.getZ());

        // Сталкиваемся с хитбоксом по Z
        if (rotationPoint.z >= (lenghtZ - 0.05) / 2f)
            rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), -random(minMotionXZ, maxMotionXZ));

        // Сталкиваемся с хитбоксом по -X
        if (rotationPoint.x <= -(lenghtX - 0.05) / 2f)
            rotationMotion = new Vec3d(random(minMotionXZ, 0.03f), rotationMotion.getY(), rotationMotion.getZ());

        // Сталкиваемся с хитбоксом по -Y
        if (rotationPoint.y <= 0.05)
            rotationMotion = new Vec3d(rotationMotion.getX(), random(minMotionY, maxMotionY), rotationMotion.getZ());

        // Сталкиваемся с хитбоксом по -Z
        if (rotationPoint.z <= -(lenghtZ - 0.05) / 2f)
            rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), random(minMotionXZ, maxMotionXZ));

        // Добавляем джиттер
        rotationPoint.add(random(-0.03f, 0.03f), 0f, random(-0.03f, 0.03f));

        if (!mc.player.canSee(target)) {
            // Если мы используем обход ударов через стену V1 и наша цель за стеной, то целимся в верхушку хитбокса т.к. матриксу поебать
            if (Objects.requireNonNull(wallsBypass.getValue()) == WallsBypass.V1) {
                return target.getPos().add(random(-0.15, 0.15), lenghtY, random(-0.15, 0.15));
            }
        }

        float[] rotation;

        // Если мы перестали смотреть на цель
        if (!Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue())) {
            float[] rotation1 = Managers.PLAYER.calcAngle(target.getPos().add(0, target.getEyeHeight(target.getPose()) / 2f, 0));

            // Проверяем видимость центра игрока
            if (PlayerUtility.squaredDistanceFromEyes(target.getPos().add(0, target.getEyeHeight(target.getPose()) / 2f, 0)) <= attackRange.getPow2Value()
                    && Managers.PLAYER.checkRtx(rotation1[0], rotation1[1], getRange(), 0, rayTrace.getValue())) {
                // наводим на центр
                rotationPoint = new Vec3d(random(-0.1f, 0.1f), target.getEyeHeight(target.getPose()) / (random(1.8f, 2.5f)), random(-0.1f, 0.1f));
            } else {
                // Сканим хитбокс на видимую точку
                float halfBox = (float) (lenghtX / 2f);

                for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05f) {
                    for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05f) {
                        for (float y1 = 0.05f; y1 <= target.getBoundingBox().getLengthY(); y1 += 0.15f) {

                            Vec3d v1 = new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1);

                            // Скипаем, если вне досягаемости
                            if (PlayerUtility.squaredDistanceFromEyes(v1) > attackRange.getPow2Value()) continue;

                            rotation = Managers.PLAYER.calcAngle(v1);
                            if (Managers.PLAYER.checkRtx(rotation[0], rotation[1], getRange(), 0, rayTrace.getValue())) {
                                // Наводимся, если видим эту точку
                                rotationPoint = new Vec3d(x1, y1, z1);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return target.getPos().add(rotationPoint);
    }

    public boolean isInRange(Entity target) {

        if (PlayerUtility.squaredDistanceFromEyes(target.getPos().add(0, target.getEyeHeight(target.getPose()), 0)) > getSquaredRotateDistance() + 4) {
            return false;
        }

        float[] rotation;
        float halfBox = (float) (target.getBoundingBox().getLengthX() / 2f);

        // уменьшил частоту выборки
        for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.15f) {
            for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.15f) {
                for (float y1 = 0.05f; y1 <= target.getBoundingBox().getLengthY(); y1 += 0.25f) {
                    if (PlayerUtility.squaredDistanceFromEyes(new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1)) > getSquaredRotateDistance())
                        continue;

                    rotation = Managers.PLAYER.calcAngle(new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1));
                    if (Managers.PLAYER.checkRtx(rotation[0], rotation[1], (float) Math.sqrt(getSquaredRotateDistance()), getWallRange(), rayTrace.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Entity findTarget() {
        List<LivingEntity> first_stage = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if ((ent instanceof ShulkerBulletEntity || ent instanceof FireballEntity)
                    && ent.isAlive()
                    && isInRange(ent)
                    && Projectiles.getValue()) {
                return ent;
            }
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

            case LowestDurability -> first_stage.stream().min(Comparator.comparing(e -> {
                        float v = 0;
                        for (ItemStack armor : e.getArmorItems())
                            if (armor != null && !armor.getItem().equals(Items.AIR)) {
                                v += ((armor.getMaxDamage() - armor.getDamage()) / (float) armor.getMaxDamage());
                            }
                        return v;
                    }
            )).orElse(null);

            case HighestDurability -> first_stage.stream().max(Comparator.comparing(e -> {
                        float v = 0;
                        for (ItemStack armor : e.getArmorItems())
                            if (armor != null && !armor.getItem().equals(Items.AIR)) {
                                v += ((armor.getMaxDamage() - armor.getDamage()) / (float) armor.getMaxDamage());
                            }
                        return v;
                    }
            )).orElse(null);
        };
    }

    private boolean skipEntity(Entity entity) {
        if (isBullet(entity)) return false;
        if (!(entity instanceof LivingEntity ent)) return true;
        if (ent.isDead() || !entity.isAlive()) return true;
        if (entity instanceof ArmorStandEntity) return true;
        if (entity instanceof CatEntity) return true;
        if (skipNotSelected(entity)) return true;
        if (!InteractionUtility.isVecInFOV(ent.getPos(), fov.getValue())) return true;

        if (entity instanceof PlayerEntity player) {
            if (ModuleManager.antiBot.isEnabled() && AntiBot.bots.contains(entity))
                return true;
            if (player == mc.player || Managers.FRIEND.isFriend(player))
                return true;
            if (player.isCreative() && ignoreCreative.getValue())
                return true;
            if (player.getArmor() == 0 && ignoreNaked.getValue())
                return true;
            if (player.isInvisible() && ignoreInvisible.getValue())
                return true;
            if (player.getTeamColorValue() == mc.player.getTeamColorValue() && ignoreTeam.getValue() && mc.player.getTeamColorValue() != 16777215)
                return true;
        }

        return !isInRange(entity) || (entity.hasCustomName() && ignoreNamed.getValue());
    }

    private boolean isBullet(Entity entity) {
        return (entity instanceof ShulkerBulletEntity || entity instanceof FireballEntity)
                && entity.isAlive()
                && PlayerUtility.squaredDistanceFromEyes(entity.getPos()) < getSquaredRotateDistance()
                && Projectiles.getValue();
    }

    private boolean skipNotSelected(Entity entity) {
        if (entity instanceof SlimeEntity && !Slimes.getValue()) return true;
        if (entity instanceof HostileEntity he) {
            if (!hostiles.getValue())
                return true;

            if (onlyAngry.getValue())
                return !he.isAngryAt(mc.player);
        }

        if (entity instanceof PlayerEntity && !Players.getValue()) return true;
        if (entity instanceof VillagerEntity && !Villagers.getValue()) return true;
        if (entity instanceof MobEntity && !Mobs.getValue()) return true;
        return entity instanceof AnimalEntity && !Animals.getValue();
    }

    private float getFOVAngle(@NotNull LivingEntity e) {
        double difX = e.getX() - mc.player.getX();
        double difZ = e.getZ() - mc.player.getZ();
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        return Math.abs(yaw - MathHelper.wrapDegrees(mc.player.getYaw()));
    }

    public void pause() {
        pauseTimer.reset();
    }

    private boolean shouldRandomizeDelay() {
        return randomHitDelay.getValue() && (mc.player.isOnGround() || mc.player.fallDistance < 0.12f || mc.player.isSwimming() || mc.player.isFallFlying());
    }

    private boolean shouldRandomizeFallDistance() {
        return randomHitDelay.getValue() && !shouldRandomizeDelay();
    }

    public static class Position {
        private double x, y, z;
        private int ticks;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public boolean shouldRemove() {
            return ticks++ > ModuleManager.aura.backTicks.getValue();
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;

        }
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

    public enum Resolver {
        Off, Advantage, Predictive, BackTrack
    }
    
    public enum AimPoint {
        Head, Chest, Legs, Random
    }
    
    public enum SprintMode {
        Off, Default, ResetAfterHit, Legit, Packet
    }

    public enum Mode {
        Interact, Track, Grim, GrimSmooth, None
    }

    public enum AttackHand {
        MainHand, OffHand, None
    }

    public enum ESP {
        Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, PhasmoZapen, Skull, Rounded
    }

    public enum AccelerateOnHit {
        Off, Yaw, Pitch, Both
    }

    public enum WallsBypass {
        Off, V1, V2
    }
    
    // Multipoint методы из AuraAI
    private void updateAimPoint() {
        if (!multipointAim.getValue()) return;
        
        if (aimPoint.getValue() == AimPoint.Random && randomizeAimPoint.getValue()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAimPointChange > aimPointChangeFrequency.getValue()) {
                currentAimPoint = getRandomAimPoint();
                lastAimPointChange = currentTime;
            }
        } else {
            currentAimPoint = aimPoint.getValue();
        }
    }
    
    private AimPoint getRandomAimPoint() {
        AimPoint[] points = {AimPoint.Head, AimPoint.Chest, AimPoint.Legs};
        return points[(int) (Math.random() * points.length)];
    }
    
    private Vec3d getMultipointTargetPosition() {
        if (target == null) return null;
        
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        
        switch (currentAimPoint) {
            case Head:
                y += target.getEyeHeight(target.getPose());
                break;
            case Chest:
                y += target.getEyeHeight(target.getPose()) * 0.6;
                break;
            case Legs:
                y += target.getEyeHeight(target.getPose()) * 0.2;
                break;
            case Random:
                // Уже выбрано в updateAimPoint
                break;
        }
        
        return new Vec3d(x, y, z);
    }
    
    // Sprint методы из AuraAI
    private void handleSprintBeforeAttack() {
        if (sprintMode.getValue() == SprintMode.Off) return;
        
        switch (sprintMode.getValue()) {
            case ResetAfterHit:
                if (sprintResetAfterHit.getValue()) {
                    wasSprintingBeforeHit = mc.player.isSprinting();
                    if (wasSprintingBeforeHit) {
                        disableSprint();
                    }
                }
                break;
            case Legit:
                if (!mc.player.isSprinting() && mc.player.getMovementSpeed() > 0.1f) {
                    enableSprint();
                }
                break;
            case Packet:
                if (!mc.player.isSprinting()) {
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                }
                break;
        }
    }
    
    private void handleSprintAfterAttack() {
        if (sprintMode.getValue() == SprintMode.Off) return;
        
        lastHitTime = System.currentTimeMillis();
        
        if (sprintMode.getValue() == SprintMode.ResetAfterHit && sprintResetAfterHit.getValue()) {
            if (wasSprintingBeforeHit) {
                new Thread(() -> {
                    try {
                        Thread.sleep(sprintResetDelay.getValue().longValue());
                        if (mc.player != null) {
                            enableSprint();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    // FOV визуализация из AuraAI
    private void renderFOV(MatrixStack stack) {
        if (mc.player == null) return;
        
        double playerX = mc.player.getX();
        double playerY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        double playerZ = mc.player.getZ();
        
        float yaw = mc.player.getYaw();
        
        // Рисуем FOV конус
        int segments = 32;
        double radius = getRange();
        double fovAngle = 60.0; // FOV угол
        
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(yaw - fovAngle/2 + (fovAngle * i / segments));
            double angle2 = Math.toRadians(yaw - fovAngle/2 + (fovAngle * (i + 1) / segments));
            
            double x1 = playerX + Math.cos(angle1) * radius;
            double z1 = playerZ + Math.sin(angle1) * radius;
            double x2 = playerX + Math.cos(angle2) * radius;
            double z2 = playerZ + Math.sin(angle2) * radius;
            
            // Рисуем линии FOV
            Render3DEngine.drawLine(
                new Vec3d(x1, playerY, z1),
                new Vec3d(x2, playerY, z2),
                new java.awt.Color(fovColor.getValue())
            );
        }
    }
    
    // Custom Ghost rendering с настройками из AuraAI
    private void renderCustomGhosts(MatrixStack stack, Entity target) {
        if (target == null || mc.player == null || mc.world == null) return;

        try {
            Camera camera = mc.gameRenderer.getCamera();
            if (camera == null) return;

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

            // Red on Impact effect - безопасная проверка
            boolean isRed = false;
            if (redOnImpact.getValue() && lastHitTime > 0) {
                long timeSinceHit = System.currentTimeMillis() - lastHitTime;
                isRed = timeSinceHit < hitColorDuration.getValue();
            }

            // Alpha animation - полностью исправлено мигание с стабилизацией
            float alpha = maxAlpha.getValue();
            if (enableAlphaAnimation.getValue() && alphaAnimation.getValue() > 0) {
                try {
                    float alphaTime = (System.currentTimeMillis() % (long) alphaAnimation.getValue().floatValue()) / alphaAnimation.getValue();
                    // Используем очень плавную анимацию с минимальными колебаниями
                    float animationFactor = (float) (0.7f + 0.3f * Math.sin(alphaTime * Math.PI * alphaFactor.getValue() * 0.5f));
                    alpha = maxAlpha.getValue() * animationFactor;
                    alpha = Math.max(100, Math.min(255, alpha)); // Минимальная прозрачность 100, чтобы не мигало
                } catch (Exception e) {
                    alpha = maxAlpha.getValue();
                }
            }

            // Стабилизация alpha для предотвращения мигания
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRenderTime < 16) { // Ограничиваем до 60 FPS
                alpha = lastAlpha; // Используем предыдущее значение
            } else {
                lastAlpha = alpha;
                lastRenderTime = currentTime;
            }

            for (int j = 0; j < ghostCount; j++) {
                for (int i = 0; i <= length; i++) {
                    // Используем настройку ghostSpeed для плавной анимации
                    double radians = Math.toRadians((((float) i / 1.5f + iAge * ghostSpeed) * factor + (j * 120)) % (factor * 360));
                    double sinQuad = Math.sin(Math.toRadians(iAge * ghostSpeed + i * (j + 1)) * amplitude) / shaking;

                    float offset = ((float) i / length);
                    MatrixStack matrices = new MatrixStack();
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

                    // Use ghostRadius for positioning
                    matrices.translate(tPosX + Math.cos(radians) * ghostRadius, (tPosY + 1 + sinQuad), tPosZ + Math.sin(radians) * ghostRadius);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                    Matrix4f matrix = matrices.peek().getPositionMatrix();

                    // Color with red impact effect - безопасная проверка
                    int color;
                    try {
                        if (isRed) {
                            int alphaInt = Math.max(0, Math.min(255, (int) alpha));
                            color = new java.awt.Color(255, 0, 0, alphaInt).getRGB();
                        } else {
                            color = Render2DEngine.applyOpacity(HudEditor.getColor((int) (180 * offset)), offset * alpha / 255f).getRGB();
                        }
                    } catch (Exception e) {
                        color = 0xFFFFFFFF; // Белый цвет по умолчанию
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
        } catch (Exception e) {
            // В случае ошибки просто отключаем рендеринг                                                                  
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    // ========== AUTOMACE LOGIC ==========

    private boolean shouldUseMace() {
        if (!enableAutoMace.getValue() || target == null) return false;

        // ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ БЕЗОПАСНОСТИ
        // Проверяем, что игрок действительно падает (не просто стоит)
        if (mc.player.fallDistance < 1.0f) return false;

        // Проверяем, что игрок не в воде или лаве
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return false;

        // Проверяем, что игрок не ездит на лошади/свинье
        if (mc.player.hasVehicle()) return false;

        // Проверяем, что игрок не использует элитры
        if (mc.player.isFallFlying()) return false;

        // Выбираем параметры в зависимости от режима
        float currentMinHeight = autoMaceMode.getValue() == AutoMaceMode.STRONG ? strongMinHeight.getValue() : minHeight.getValue();
        float currentMaxDistance = autoMaceMode.getValue() == AutoMaceMode.STRONG ? strongMaxDistance.getValue() : maxDistance.getValue();

        // Проверяем, что игрок выше цели на минимальную высоту
        double heightDifference = mc.player.getY() - target.getY();
        if (heightDifference < currentMinHeight) return false;

        // Проверяем дистанцию до цели
        double distance = mc.player.distanceTo(target);
        if (distance > currentMaxDistance) return false;

        // Проверяем, что у игрока есть булава
        SearchInvResult maceResult = InventoryUtility.getMaceHotBar();
        if (!maceResult.found()) return false;

        // Проверяем, что игрок падает (если включено)
        if (onlyWhenFalling.getValue() && mc.player.getVelocity().y > 0.1) return false;

        // Дополнительные проверки для режима Strong
        if (autoMaceMode.getValue() == AutoMaceMode.STRONG) {
            // Проверяем задержки для более легитного поведения
            long currentTime = System.currentTimeMillis();

            // Минимальная задержка между переключениями
            if (currentTime - lastSwitchTime < strongSwitchDelay.getValue()) return false;

            // Проверяем, не движется ли игрок (если включено)
            if (strongPauseOnMovement.getValue() && isPlayerMoving() &&
                currentTime - lastMovementTime < 500) return false;

            // Ограничиваем количество атак за сессию
            if (strongAttackCount >= strongMaxAttacksPerSession.getValue()) return false;
        } else {
            // Дополнительные проверки для LITE режима
            long currentTime = System.currentTimeMillis();

            // Минимальная задержка между переключениями для LITE режима
            if (currentTime - lastSwitchTime < 500) return false; // 500мс задержка
        }

        return true;
    }

    private boolean isPlayerMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0 ||
               mc.player.input.jumping || mc.player.input.sneaking;
    }

    private void updateHumanBehavior() {
        if (!strongHumanBehavior.getValue() || autoMaceMode.getValue() != AutoMaceMode.STRONG) return;

        // Отслеживаем движение игрока
        boolean currentlyMoving = isPlayerMoving();
        if (currentlyMoving && !wasMoving) {
            lastMovementTime = System.currentTimeMillis();
        }
        wasMoving = currentlyMoving;

        // Обновляем взгляд на цель
        if (strongLookAtTarget.getValue() && target != null && wasUsingMace) {
            updateLookAtTarget();
        }
    }

    private void updateLookAtTarget() {
        if (target == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLookUpdate < 50) return; // Обновляем не чаще 20 раз в секунду

        // Вычисляем углы для взгляда на цель
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
        Vec3d playerPos = mc.player.getPos().add(0, mc.player.getHeight() / 2, 0);
        Vec3d delta = targetPos.subtract(playerPos);

        targetLookYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        targetLookPitch = (float) Math.toDegrees(Math.atan2(-delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        // Нормализуем углы
        targetLookYaw = MathHelper.wrapDegrees(targetLookYaw);
        targetLookPitch = MathHelper.clamp(targetLookPitch, -90f, 90f);

        // Плавная наводка
        if (smoothAiming.getValue()) {
            float smoothFactor = aimSmoothness.getValue() * strongLookSpeed.getValue();

            // Адаптивная скорость
            if (adaptiveSpeed.getValue()) {
                float distance = (float) mc.player.distanceTo(target);
                smoothFactor *= Math.min(1.0f, distance / 3.0f);
            }

            // Добавляем джиттер для более человеческого поведения
            if (jitterIntensity.getValue() > 0) {
                float jitterYaw = (float) (Math.random() - 0.5) * jitterIntensity.getValue() * 2f;
                float jitterPitch = (float) (Math.random() - 0.5) * jitterIntensity.getValue() * 2f;
                targetLookYaw += jitterYaw;
                targetLookPitch += jitterPitch;
            }

            currentLookYaw += (targetLookYaw - currentLookYaw) * smoothFactor;
            currentLookPitch += (targetLookPitch - currentLookPitch) * smoothFactor;
        } else {
            currentLookYaw = targetLookYaw;
            currentLookPitch = targetLookPitch;
        }

        // Ограничиваем pitch
        currentLookPitch = MathHelper.clamp(currentLookPitch, -90f, 90f);

        // Обновляем rotationYaw/rotationPitch вместо прямого изменения mc.player
        // Это предотвращает конфликты с onSync()
        rotationYaw = currentLookYaw;
        rotationPitch = currentLookPitch;

        lastLookUpdate = currentTime;
    }

    private boolean shouldMissAttack() {
        if (!strongHumanBehavior.getValue()) return false;

        // Всегда промахиваемся, если решили промахнуться
        if (shouldMissNext) return true;

        // Дополнительная рандомизация промахов
        return Math.random() < strongMissChance.getValue();
    }

    private boolean hasClearLineOfSight(Entity target) {
        if (target == null) return false;
        
        // Проверяем, что между игроком и целью нет блоков
        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getBoundingBox().getCenter();
        
        // Raycast для проверки препятствий
        HitResult hitResult = mc.world.raycast(new RaycastContext(
            start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        ));
        
        // Если raycast попал в цель или не попал ни во что, значит линия видимости чистая
        return hitResult.getType() == HitResult.Type.MISS || 
               (hitResult.getType() == HitResult.Type.ENTITY && 
                hitResult instanceof EntityHitResult && 
                ((EntityHitResult) hitResult).getEntity() == target);
    }

    private void handleAutoMace() {
        if (!enableAutoMace.getValue()) return;

        boolean shouldUse = shouldUseMace();
        long currentTime = System.currentTimeMillis();

        if (autoMaceMode.getValue() == AutoMaceMode.LITE) {
            handleLiteMode(shouldUse, currentTime);
        } else {
            handleStrongMode(shouldUse, currentTime);
        }
    }

    private void handleLiteMode(boolean shouldUse, long currentTime) {
        if (shouldUse && !wasUsingMace) {
            // Переключаемся на булаву
            SearchInvResult maceResult = InventoryUtility.getMaceHotBar();
            if (maceResult.found() && target != null) {
                // ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ БЕЗОПАСНОСТИ ДЛЯ LITE РЕЖИМА
                
                // Проверяем, что игрок действительно падает с достаточной скоростью
                if (mc.player.getVelocity().y > -0.1) return; // Слишком медленное падение
                
                // Проверяем, что цель не слишком далеко
                double distance = mc.player.distanceTo(target);
                if (distance > maxDistance.getValue() * 1.2f) return; // +20% запас
                
                // Проверяем, что цель жива и не в креативе
                if (target instanceof PlayerEntity) {
                    PlayerEntity targetPlayer = (PlayerEntity) target;
                    if (targetPlayer.isCreative() || targetPlayer.isSpectator()) return;
                }
                
                // Проверяем, что у нас есть достаточно места для атаки
                if (!hasClearLineOfSight(target)) return;
                
                previousSlot = mc.player.getInventory().selectedSlot;
                maceResult.switchTo();
                sendPacket(new UpdateSelectedSlotC2SPacket(maceResult.slot()));
                wasUsingMace = true;
                maceSwitchTime = currentTime;
                maceAttackDone = false;

                // Принудительная атака булавой с проверкой дистанции
                if (hitTicks <= 0) {
                    // Дополнительная проверка дистанции перед атакой булавой
                    if (distance <= getRange()) {
                        hitTicks = getHitTicks();
                        boolean[] playerState = preAttack();
                        ModuleManager.criticals.doCrit();
                        mc.interactionManager.attackEntity(mc.player, target);
                        swingHand();
                        postAttack(playerState[0], playerState[1]);
                        maceAttackDone = true;
                    }
                }
            }
        } else if (wasUsingMace) {
            // Дополнительная атака булавой, если не атаковали еще
            if (!maceAttackDone && hitTicks <= 0 && target != null) {
                // Дополнительная проверка дистанции перед атакой булавой
                double distance = mc.player.distanceTo(target);
                float maxAllowedDistance = getRange();

                if (distance <= maxAllowedDistance) {
                    hitTicks = getHitTicks();
                    boolean[] playerState = preAttack();
                    ModuleManager.criticals.doCrit();
                    mc.interactionManager.attackEntity(mc.player, target);
                    swingHand();
                    postAttack(playerState[0], playerState[1]);
                    maceAttackDone = true;
                }
            }

            // Проверяем, нужно ли вернуться к мечу
            boolean shouldReturn = false;

            if (returnToSword.getValue()) {
                // Возвращаемся к мечу только если:
                // 1. Прошло достаточно времени (maceHoldTime)
                // 2. ИЛИ условия больше не выполняются И прошло минимум 500мс
                if (currentTime - maceSwitchTime >= maceHoldTime.getValue()) {
                    shouldReturn = true;
                } else if (!shouldUse && currentTime - maceSwitchTime >= 500) {
                    shouldReturn = true;
                }
            }

            if (shouldReturn && previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
                sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
                previousSlot = -1;
                wasUsingMace = false;
                maceSwitchTime = 0;
                maceAttackDone = false;
            }
        }
    }

    private void handleStrongMode(boolean shouldUse, long currentTime) {
        // ЛЕГИТНЫЙ Strong режим - как в оригинальном Aura.java

        // Обновляем человеческое поведение
        updateHumanBehavior();

        // Проверяем, нужно ли приостановить из-за движения
        if (strongPauseOnMovement.getValue() && isPlayerMoving() &&
            currentTime - lastMovementTime < 1000) { // Увеличили задержку
            return;
        }

        if (shouldUse && !wasUsingMace) {
            // Переключаемся на булаву ТОЛЬКО при реальном падении
            SearchInvResult maceResult = InventoryUtility.getMaceHotBar();
            if (maceResult.found() && target != null && mc.player.fallDistance > 2.0f) {
                // ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ БЕЗОПАСНОСТИ ДЛЯ STRONG РЕЖИМА
                
                // Проверяем, что игрок падает с достаточной скоростью
                if (mc.player.getVelocity().y > -0.2) return; // Слишком медленное падение
                
                // Проверяем, что цель не в креативе или спектаторе
                if (target instanceof PlayerEntity) {
                    PlayerEntity targetPlayer = (PlayerEntity) target;
                    if (targetPlayer.isCreative() || targetPlayer.isSpectator()) return;
                }
                
                // Проверяем, что у нас есть чистая линия видимости
                if (!hasClearLineOfSight(target)) return;
                
                // Проверяем, что цель не слишком далеко
                double distance = mc.player.distanceTo(target);
                if (distance > strongMaxDistance.getValue() * 1.1f) return; // +10% запас
                
                previousSlot = mc.player.getInventory().selectedSlot;

                // ОЧЕНЬ медленное переключение для обхода детекции
                int switchDelay = strongSwitchDelay.getValue() * 2; // Удвоили задержку

                // Рандомизация задержки если включено
                if (strongRandomizeTiming.getValue()) {
                    switchDelay = (int) (switchDelay * (0.7 + Math.random() * 0.6)); // ±30% рандомизация
                }

                if (currentTime - lastSwitchTime >= switchDelay) {
                    maceResult.switchTo();
                    sendPacket(new UpdateSelectedSlotC2SPacket(maceResult.slot()));
                    wasUsingMace = true;
                    maceSwitchTime = currentTime;
                    lastSwitchTime = currentTime;
                    maceAttackDone = false;
                    strongModeReady = true;
                    strongAttackCount = 0;
                }
            }
        } else if (wasUsingMace && strongModeReady) {
            // ОЧЕНЬ консервативная атака - только ОДИН раз за сессию
            int attackDelay = strongAttackDelay.getValue() * 3; // Утроили задержку

            // Рандомизация задержки атаки если включено
            if (strongRandomizeTiming.getValue()) {
                attackDelay = (int) (attackDelay * (0.8 + Math.random() * 0.4)); // ±20% рандомизация
            }

            if (currentTime - lastMaceAttackTime >= attackDelay && hitTicks <= 0 && target != null) {

                // Ограничиваем атаки - максимум 1 атака за сессию
                if (strongAttackCount >= 1) {
                    strongModeReady = false;
                    return;
                }

                // Дополнительная проверка дистанции перед атакой булавой
                double distance = mc.player.distanceTo(target);
                float maxAllowedDistance = getRange() * 0.8f; // Уменьшили дистанцию на 20%

                if (distance <= maxAllowedDistance) {
                    // ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ ПЕРЕД АТАКОЙ
                    
                    // Проверяем, что цель все еще жива
                    if (target.isRemoved()) {
                        strongModeReady = false;
                        return;
                    }
                    
                    // Проверяем, что у нас все еще есть чистая линия видимости
                    if (!hasClearLineOfSight(target)) {
                        strongModeReady = false;
                        return;
                    }
                    
                    // Проверяем, что игрок все еще падает
                    if (mc.player.getVelocity().y > -0.1) {
                        strongModeReady = false;
                        return;
                    }
                    
                    // Проверяем, не движется ли игрок (если включено)
                    if (strongPauseOnMovement.getValue() && isPlayerMoving()) {
                        return;
                    }
                    
                    // БЕЗОПАСНАЯ атака без критиков
                    hitTicks = getHitTicks();
                    boolean[] playerState = preAttack();
                    
                    // Случайно промахиваемся для легитности
                    if (shouldMissAttack()) {
                        // Имитируем промах, но все равно атакуем
                        swingHand();
                        maceAttackDone = true;
                        lastMaceAttackTime = currentTime;
                        strongAttackCount++;
                        shouldMissNext = false; // Сбрасываем флаг промаха
                        return;
                    }
                    
                    // НЕ используем ModuleManager.criticals.doCrit() - детектируется
                    mc.interactionManager.attackEntity(mc.player, target);
                    swingHand();
                    postAttack(playerState[0], playerState[1]);
                    maceAttackDone = true;
                    lastMaceAttackTime = currentTime;
                    strongAttackCount++;
                } else {
                    // Если дистанция слишком большая, не атакуем
                    strongModeReady = false;
                }
            }

            // БЫСТРОЕ возвращение к мечу - максимум через 1 секунду
            boolean shouldReturn = false;

            if (returnToSword.getValue()) {
                // Очень быстрое возвращение для легитности
                if (currentTime - maceSwitchTime >= 1000) { // Максимум 1 секунда
                    shouldReturn = true;
                } else if (!shouldUse && currentTime - maceSwitchTime >= 500) {
                    shouldReturn = true;
                } else if (strongAttackCount >= 1) { // После 1 атаки сразу возвращаемся
                    shouldReturn = true;
                }
            }

            if (shouldReturn && previousSlot != -1) {
                // Быстрое возвращение
                if (currentTime - lastSwitchTime >= 200) { // Минимальная задержка
                    mc.player.getInventory().selectedSlot = previousSlot;
                    sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
                    previousSlot = -1;
                    wasUsingMace = false;
                    maceSwitchTime = 0;
                    maceAttackDone = false;
                    strongModeReady = false;
                    strongAttackCount = 0;
                    lastSwitchTime = currentTime;
                }
            }
        }
    }

    public enum AutoMaceMode {
        LITE, STRONG
    }

}
