package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.render.Camera;
import java.util.List;
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
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Rotations;
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
import static thunder.hack.utility.math.MathUtility.random;

public class AuraAI extends Module {
    // Основные настройки из Aura
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride",false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f,v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Boolean> grimBypass = new Setting<>("GrimBypass", true, v -> wallsBypass.getValue() == WallsBypass.V3);
    public final Setting<Boolean> smartRaycast = new Setting<>("SmartRaycast", true, v -> wallsBypass.getValue() == WallsBypass.V3);
    public final Setting<Boolean> predictionBypass = new Setting<>("PredictionBypass", true, v -> wallsBypass.getValue() == WallsBypass.V3);
    public final Setting<Boolean> packetOrderBypass = new Setting<>("PacketOrderBypass", true, v -> wallsBypass.getValue() == WallsBypass.V3);
    // AI Aura настройки из AuraAI
    public final Setting<SettingGroup> aiGroup = new Setting<>("AI Settings", new SettingGroup(false, 0));
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180).addToGroup(aiGroup); // FOV для атаки (заменяет Max Attack Angle)
    public final Setting<Float> rotationSmoothness = new Setting<>("Rotation Smoothness", 0.02f, -1.0f, 3.0f).addToGroup(aiGroup);
    public final Setting<Float> rotationSpeed = new Setting<>("Rotation Speed", 0.0f, -5.0f, 5.0f).addToGroup(aiGroup);

    // ShowFOV настройки
    public final Setting<Boolean> showFOV = new Setting<>("ShowFOV", false).addToGroup(aiGroup);
    public final Setting<Integer> fovColor = new Setting<>("FOV Color", 0xFF00FF00, v -> showFOV.getValue()).addToGroup(aiGroup);
    public final Setting<Float> fovLineWidth = new Setting<>("FOV Line Width", 2.0f, 0.5f, 5.0f, v -> showFOV.getValue()).addToGroup(aiGroup);
    public final Setting<Boolean> fovFill = new Setting<>("FOV Fill", false, v -> showFOV.getValue()).addToGroup(aiGroup);
    public final Setting<Integer> fovFillColor = new Setting<>("FOV Fill Color", 0x2000FF00, v -> showFOV.getValue() && fovFill.getValue()).addToGroup(aiGroup);


    public final Setting<SprintMode> sprintMode = new Setting<>("Sprint Mode", SprintMode.Default).addToGroup(aiGroup);
    public final Setting<Boolean> dropSprint = new Setting<>("DropSprint", true, v -> sprintMode.getValue() != SprintMode.Default && sprintMode.getValue() != SprintMode.Off).addToGroup(aiGroup);
    public final Setting<Boolean> returnSprint = new Setting<>("ReturnSprint", true, v -> sprintMode.getValue() != SprintMode.Default && sprintMode.getValue() != SprintMode.Off).addToGroup(aiGroup);
    public final Setting<Long> attackDelayMs = new Setting<>("Attack Delay (ms)", 50L, 10L, 150L, v -> sprintMode.getValue() == SprintMode.Packet || sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Long> generalAttackDelay = new Setting<>("General Attack Delay (ms)", 50L, 10L, 200L).addToGroup(aiGroup);

    // Дополнительные настройки для режимов спринта
    public final Setting<Long> sprintResetDelay = new Setting<>("Sprint Reset Delay (ms)", 200L, 50L, 1000L, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Long> sprintDropDelay = new Setting<>("Sprint Drop Delay (ms)", 50L, 10L, 200L, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Boolean> usePacketOnly = new Setting<>("Use Packet Only", true, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);

    // Настройки для Packet режима (оптимизированы для Grim серверов)
    public final Setting<Float> packetSprintRange = new Setting<>("Packet Sprint Range", 2.8f, 1.0f, 5.0f, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);
    public final Setting<Integer> packetSprintDelay = new Setting<>("Packet Sprint Delay (ms)", 150, 10, 500, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);
    public final Setting<Boolean> packetAggressiveMode = new Setting<>("Packet Aggressive Mode", false, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);
    public final Setting<Boolean> packetSmartTiming = new Setting<>("Packet Smart Timing", true, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);
    public final Setting<Float> packetSprintDuration = new Setting<>("Packet Sprint Duration (ms)", 800f, 200f, 1500f, v -> sprintMode.getValue() == SprintMode.Packet).addToGroup(aiGroup);

    // Настройки для ResetAfterHit режима (оптимизированы для Grim серверов)
    public final Setting<Float> resetSprintRange = new Setting<>("Reset Sprint Range", 2.2f, 1.0f, 4.0f, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Boolean> resetSmartReset = new Setting<>("Reset Smart Reset", true, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Boolean> resetComboMode = new Setting<>("Reset Combo Mode", false, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Integer> resetMaxHits = new Setting<>("Reset Max Hits", 2, 1, 10, v -> sprintMode.getValue() == SprintMode.ResetAfterHit && resetComboMode.getValue()).addToGroup(aiGroup);
    public final Setting<Float> resetComboMultiplier = new Setting<>("Reset Combo Multiplier", 1.3f, 1.0f, 3.0f, v -> sprintMode.getValue() == SprintMode.ResetAfterHit && resetComboMode.getValue()).addToGroup(aiGroup);
    public final Setting<Boolean> resetSprintReturn = new Setting<>("Reset Sprint Return", true, v -> sprintMode.getValue() == SprintMode.ResetAfterHit).addToGroup(aiGroup);
    public final Setting<Long> resetSprintReturnDelay = new Setting<>("Reset Sprint Return Delay (ms)", 200L, 50L, 1000L, v -> sprintMode.getValue() == SprintMode.ResetAfterHit && resetSprintReturn.getValue()).addToGroup(aiGroup);

    // Настройки для легитного режима спринта
    public final Setting<Float> legitSprintRange = new Setting<>("Legit Sprint Range", 2.8f, 1.0f, 4.0f, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Integer> legitSprintDelay = new Setting<>("Legit Sprint Delay (ms)", 150, 50, 500, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Float> legitSprintChance = new Setting<>("Legit Sprint Chance", 0.85f, 0.1f, 1.0f, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Boolean> legitRandomizeTiming = new Setting<>("Legit Randomize Timing", true, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Boolean> legitPauseOnMovement = new Setting<>("Legit Pause On Movement", true, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Boolean> legitUseNaturalSprint = new Setting<>("Legit Use Natural Sprint", true, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Float> legitSprintDuration = new Setting<>("Legit Sprint Duration (ms)", 800f, 200f, 2000f, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);
    public final Setting<Boolean> legitSmoothTransition = new Setting<>("Legit Smooth Transition", true, v -> sprintMode.getValue() == SprintMode.Legit).addToGroup(aiGroup);

    // Multipoint настройки
    public final Setting<Boolean> multipointAim = new Setting<>("Multipoint Aim", true).addToGroup(aiGroup);
    public final Setting<AimPoint> aimPoint = new Setting<>("Aim Point", AimPoint.Random, v -> multipointAim.getValue()).addToGroup(aiGroup);
    public final Setting<Boolean> randomizeAimPoint = new Setting<>("Randomize Aim Point", true, v -> multipointAim.getValue() && aimPoint.getValue() == AimPoint.Random).addToGroup(aiGroup);
    public final Setting<Integer> aimPointChangeFrequency = new Setting<>("Aim Point Change Frequency", 60, 20, 200, v -> multipointAim.getValue() && randomizeAimPoint.getValue()).addToGroup(aiGroup);
    
    // SpookyTime Bypass настройки
    public final Setting<Boolean> spookyTimeBypass = new Setting<>("SpookyTime Bypass", false).addToGroup(aiGroup);
    public final Setting<Boolean> smoothRotation = new Setting<>("Smooth Rotation", true, v -> spookyTimeBypass.getValue()).addToGroup(aiGroup);
    public final Setting<Float> smoothSpeed = new Setting<>("Smooth Speed", 0.3f, 0.1f, 1.0f, v -> spookyTimeBypass.getValue() && smoothRotation.getValue()).addToGroup(aiGroup);
    public final Setting<Boolean> adaptiveSnapMode = new Setting<>("Adaptive Snap Mode", true, v -> spookyTimeBypass.getValue()).addToGroup(aiGroup);
    public final Setting<Integer> snapFrequency = new Setting<>("Snap Frequency", 20, 10, 50, v -> spookyTimeBypass.getValue() && adaptiveSnapMode.getValue()).addToGroup(aiGroup);
    public final Setting<Integer> snapDuration = new Setting<>("Snap Duration (ms)", 100, 50, 300, v -> spookyTimeBypass.getValue() && adaptiveSnapMode.getValue()).addToGroup(aiGroup);

    // Targets настройки
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

    // Resolver настройки
    public final Setting<Aura.Resolver> resolver = new Setting<>("Resolver", Aura.Resolver.Advantage);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20, v -> resolver.getValue() == Aura.Resolver.BackTrack);
    public final Setting<Boolean> resolverVisualisation = new Setting<>("ResolverVisualisation", false, v -> resolver.getValue() != Aura.Resolver.Off);

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
    public final Setting<Float> ghostSpeed = new Setting<>("Ghost Speed", 2.0f, 0.1f, 10.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Integer> ghostCount = new Setting<>("Ghost Count", 3, 1, 10, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);

    // Red on Impact
    public final Setting<Boolean> redOnImpact = new Setting<>("Red On Impact", true, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> hitColorDuration = new Setting<>("Hit Color Duration", 350.0f, 100.0f, 2000.0f, v -> esp.getValue() == ESP.ThunderHackV2 && redOnImpact.getValue()).addToGroup(espGroup);

    // Alpha animation
    public final Setting<Boolean> enableAlphaAnimation = new Setting<>("Enable Alpha Animation", false, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> alphaAnimation = new Setting<>("Alpha Animation", 350.0f, 100.0f, 1000.0f, v -> esp.getValue() == ESP.ThunderHackV2 && enableAlphaAnimation.getValue()).addToGroup(espGroup);
    public final Setting<Float> maxAlpha = new Setting<>("Max Alpha", 255.0f, 50.0f, 255.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> alphaFactor = new Setting<>("Alpha Factor", 3.0f, 0.1f, 10.0f, v -> esp.getValue() == ESP.ThunderHackV2 && enableAlphaAnimation.getValue()).addToGroup(espGroup);
    public final Setting<Float> minSizeFactor = new Setting<>("Min Size Factor", 0.3f, 0.1f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    

    // Physics
    public final Setting<Float> springConstant = new Setting<>("Spring Constant", 0.1f, 0.01f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> dampingFactor = new Setting<>("Damping Factor", 0.8f, 0.1f, 1.0f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);
    public final Setting<Float> attractionStrength = new Setting<>("Attraction Strength", 0.05f, 0.01f, 0.5f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(espGroup);


    // Остальные настройки из Aura
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> randomizeCriticals = new Setting<>("RandomizeCriticals", true).addToGroup(smartCrit);
    public final Setting<Integer> critSkipChance = new Setting<>("CritSkipChance", 3, 1, 10, v -> randomizeCriticals.getValue()).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> unpressShield = new Setting<>("UnpressShield", false);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.1f, 1.0f);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);

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

    // Настройки для плавной наводки при падении с булавой (Strong режим) - ОТКЛЮЧЕНО по умолчанию
    public final Setting<Boolean> strongFallingAim = new Setting<>("Strong Falling Aim", false, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG).addToGroup(autoMace);
    public final Setting<Float> fallingAimSpeed = new Setting<>("Falling Aim Speed", 0.3f, 0.1f, 2.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongFallingAim.getValue()).addToGroup(autoMace);
    public final Setting<Float> fallingAimSmoothness = new Setting<>("Falling Aim Smoothness", 0.5f, 0.1f, 3.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongFallingAim.getValue()).addToGroup(autoMace);
    public final Setting<Float> fallingAimRange = new Setting<>("Falling Aim Range", 3.0f, 2.0f, 6.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongFallingAim.getValue()).addToGroup(autoMace);
    public final Setting<Boolean> fallingAimPrediction = new Setting<>("Falling Aim Prediction", false, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongFallingAim.getValue()).addToGroup(autoMace);
    public final Setting<Float> fallingAimPredictionTime = new Setting<>("Falling Aim Prediction Time", 0.2f, 0.1f, 1.0f, v -> enableAutoMace.getValue() && autoMaceMode.getValue() == AutoMaceMode.STRONG && strongFallingAim.getValue() && fallingAimPrediction.getValue()).addToGroup(autoMace);

    // Переменные
    public static Entity target;
    public static Entity auraAITarget;

    // Основные переменные
    private boolean justEnabled = false;
    private int attackCount = 0;
    

    // Основные переменные
    private boolean isSprinting = false;
    private long lastAttackTime = 0;
    private int critSkipCounter = 0;
    private int nextCritSkip = 0;
    private long lastGrimCheck = 0;
    private boolean grimCanSprint = true;
    private int grimBypassCounter = 0;
    private long lastPacketTime = 0;
    private int packetOrderCounter = 0;

    // Переменные для обхода Grim AC
    private boolean grimHungerCheck = false;
    private boolean grimSneakingCheck = false;
    private boolean grimUsingItemCheck = false;
    private boolean grimBlindnessCheck = false;
    private boolean grimWallCollisionCheck = false;
    private boolean grimGlidingCheck = false;
    private boolean grimWaterCheck = false;
    private long lastGrimCheckTime = 0;

    // Ghost particles
    private long lastHitTime = 0;
    private long redImpactStartTime = 0;

    // Attack delay
    private final long attackDelay = 50; // 50ms задержка между атаками (20 CPS максимум)

    // ESP stabilization
    private float lastAlpha = 255f;
    private long lastRenderTime = 0;

    // UltraSmoothRotation
    private long lastRotationUpdate = 0;

    // Переменные для наводки
    public float rotationYaw;
    public float rotationPitch;

    // Переменные для плавной ротации
    private float targetYaw;
    private float targetPitch;
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    // Rotation Point переменные
    private Vec3d rotationPoint = Vec3d.ZERO;
    private Vec3d rotationMotion = Vec3d.ZERO;

    // Strong Mode переменные
    private long lastSwitchTime = 0;
    private long lastMovementTime = 0;
    private int strongAttackCount = 0;
    private boolean wasMoving = false;
    private boolean strongModeReady = false;
    private long lastMaceAttackTime = 0;

    // AutoMace переменные
    private int previousSlot = -1;
    private boolean wasUsingMace = false;
    private long maceSwitchTime = 0;
    private boolean maceAttackDone = false;

    // Look variables
    private float currentLookYaw = 0f;
    private float currentLookPitch = 0f;
    private float targetLookYaw = 0f;
    private float targetLookPitch = 0f;
    private long lastLookUpdate = 0;

    // Miss variables
    private boolean shouldMissNext = false;
    private long lastMissTime = 0;

    // Reset variables
    private int resetHitCount = 0;
    private long lastResetHitTime = 0;
    private boolean resetSprintActive = false;
    private long resetSprintStartTime = 0;
    private boolean resetComboActive = false;

    // Grim state variables
    private boolean grimWasTouchingWater = false;
    private boolean grimWasEyeInWater = false;
    private boolean grimWasGliding = false;
    private boolean grimWasFlying = false;
    private boolean grimWasSprinting = false;
    private long grimLastSprintStart = 0;
    private int grimSprintStartCount = 0;

    // Packet Sprint переменные
    private boolean packetSprintActive = false;
    private long lastPacketAttackTime = 0;

    // Legit Sprint переменные
    private long lastLegitSprintTime = 0;
    private long legitSprintStartTime = 0;
    private boolean legitSprintActive = false;
    private boolean legitSprintPaused = false;
    private long lastLegitMovementTime = 0;
    private float legitSprintProgress = 0f;
    private boolean legitSprintCooldown = false;
    private long lastLegitSprintCooldown = 0;
    
    // SpookyTime Sprint Reset переменные
    private boolean spookySprintResetActive = false;
    private long spookySprintResetStartTime = 0;
    
    // Multipoint переменные
    private AimPoint currentAimPoint = AimPoint.Head;
    private int aimPointChangeCounter = 0;
    private long lastAimPointChange = 0;
    
    // SpookyTime Bypass переменные
    private int spookyAttackCount = 0;
    private boolean isSnappingUp = false;
    private long snapStartTime = 0;
    private float snapStartYaw = 0f;
    private float snapStartPitch = 0f;
    private int attacksBeforeSnap = 20;
    private boolean isResettingSprint = false;
    private long lastSprintResetTime = 0;

    // Falling Aim переменные
    private boolean fallingAimActive = false;
    private float fallingAimTargetYaw = 0f;
    private float fallingAimTargetPitch = 0f;
    private float fallingAimCurrentYaw = 0f;
    private float fallingAimCurrentPitch = 0f;
    
    private long fallingAimStartTime = 0;
    private Vec3d fallingAimTargetPos = Vec3d.ZERO;
    private Vec3d fallingAimTargetVelocity = Vec3d.ZERO;

    // Основные переменные из Aura
    private int hitTicks;
    private int trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer();
    private final Timer pauseTimer = new Timer();

    public Box resolvedBox;
    static boolean wasTargeted = false;

    public AuraAI() {
        super("AuraAI", Category.COMBAT);
    }

    // Enums
    public enum ESP {
        Off, ThunderHack, NurikZapen, PhasmoZapen, Skull, Rounded, CelkaPasta, ThunderHackV2
    }

    public enum SprintMode {
        Default,
        Off,
        Packet,
        ResetAfterHit,
        Legit,
        SpookyTime
    }
    


    public enum WallsBypass {
        Off, V1, V2, V3
    }

    public enum AutoMaceMode {
        LITE, STRONG
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

    public enum AimPoint {
        Head, Chest, Legs, Random
    }
    



    private float getRange(){
        return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
    }

    private float getWallRange(){
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue();
    }

    public void onRender3D(MatrixStack stack) {
        if (auraAITarget != null && esp.getValue() != ESP.Off) {
            try {
                switch (esp.getValue()) {
                    case CelkaPasta -> Render3DEngine.drawOldTargetEsp(stack, auraAITarget);
                    case NurikZapen -> CaptureMark.render(auraAITarget);
                    case PhasmoZapen -> PhasmoMark.render(auraAITarget);
                    case Skull -> SkullMark.render(auraAITarget);
                    case Rounded -> RoundedMark.render(auraAITarget);
                    case ThunderHackV2 -> renderCustomGhosts(stack, auraAITarget);
                    case ThunderHack -> Render3DEngine.drawTargetEsp(stack, auraAITarget);
                }
            } catch (Exception e) {
                // Fallback к простому рендерингу
                Render3DEngine.renderGhosts(espLength.getValue(), espFactor.getValue(), espShaking.getValue(), espAmplitude.getValue(), auraAITarget);
            }
        }

        // Рендеринг FOV круга
        if (showFOV.getValue()) {
            renderFOVCircle(stack);
        }
        
        // Рендеринг BackTrack резольвера
        if (resolverVisualisation.getValue() && resolver.getValue() == Aura.Resolver.BackTrack && auraAITarget instanceof PlayerEntity player) {
            renderBackTrackResolver(stack, player);
        }
    }

    public void onRender2D(MatrixStack stack) {
        // Рендеринг FOV индикатора на экране (временно отключен из-за отсутствия методов)
        // if (showFOV.getValue()) {
        //     renderFOVIndicator(stack);
        // }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        try {
            if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
                Entity entity = Criticals.getEntity(packet);
                if (entity != null && entity instanceof LivingEntity) {
                    lastHitTime = System.currentTimeMillis();
                    redImpactStartTime = System.currentTimeMillis();
                    lastAttackTime = System.currentTimeMillis();
                }
            }
            
            // BackTrack логика (как в Aura.java)
            if (resolver.getValue() == Aura.Resolver.BackTrack && auraAITarget != null) {
                // Grim BackTrack обходы
                if (shouldApplyGrimBackTrack()) {
                    // Дополнительная рандомизация для Grim
                    if (Math.random() < 0.1f) {
                        return; // Пропускаем пакет
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки в packet handling
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Decrease hit ticks
        if (hitTicks > 0) hitTicks--;

        // Update target with LockTarget logic
        updateTarget();

        if (auraAITarget != null) {
            // BackTrack - применяем резолвер (как в Aura.java)
            resolvePlayers();
            
            // AI Aura logic
            updateAITargeting();
            handleSprint();
            
            // SpookyTime Bypass
            handleSpookyTimeBypass();
            
            // BackTrack - освобождаем резолвер (как в Aura.java)
            restorePlayers();
        }

        // AutoMace logic
        handleAutoMace();

        // Original Aura logic
        auraLogic();
    }


    @Override
    public void onEnable() {
        target = null;
        auraAITarget = null;
        lookingAtHitbox = false;
        rotationPoint = Vec3d.ZERO;
        rotationMotion = Vec3d.ZERO;
        delayTimer.reset();

        // Инициализация рандомизации критов
        critSkipCounter = 0;
        nextCritSkip = 1 + (int)(Math.random() * critSkipChance.getValue());

    }

    @Override
    public void onDisable() {
        // Resolver - освобождаем цель при отключении
        if (resolver.getValue() != Aura.Resolver.Off && auraAITarget instanceof PlayerEntity player) {
            ((IOtherClientPlayerEntity) player).releaseResolver();
        }
        
        target = null;
        auraAITarget = null;
    }

    // AI Aura methods
    private void updateAITargeting() {
        if (auraAITarget == null) return;

        // Check if target is in aim range
        double distance = mc.player.distanceTo(auraAITarget);
        if (distance > aimRange.getValue()) return;

        // Check if target is in attack range
        if (distance > attackRange.getValue()) return;

        // Check walls bypass
        if (wallsBypass.getValue() != WallsBypass.Off && distance > wallRange.getValue()) {
            if (!canAttackThroughWalls(distance)) {
                return; // Blocked by wall or Grim AC
            }
        }

        // Calculate target angles - ТОЧНАЯ НАВОДКА НА ХИТБОКС
        Vec3d targetPos = getTargetPosition(); // Используем точный хитбокс
        if (targetPos == null) return;

        Vec3d playerPos = mc.player.getEyePos();

        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Точные углы наведения на хитбокс
        targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        // Плавная ротация
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRotationUpdate) / 1000.0f;
        lastRotationUpdate = currentTime;

        // Инициализация при первом запуске
        if (currentYaw == 0 && currentPitch == 0) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }

        // Нормализация углов
        targetYaw = wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        // Вычисляем разность углов
        float deltaYaw = wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        // Стандартная ротация
        float targetDistance = (float) mc.player.distanceTo(auraAITarget);
        float baseSpeed = Math.max(0.005f, rotationSmoothness.getValue() * 0.1f);
        float speedMultiplier = Math.max(0.1f, 1.0f + rotationSpeed.getValue() * 0.5f);
        float finalSpeed = baseSpeed * speedMultiplier;
        float maxRotationSpeed = 0.6f;
        finalSpeed = Math.min(maxRotationSpeed, finalSpeed);
        float smoothFactor = Math.min(1.0f, finalSpeed * deltaTime * 50.0f);

        currentYaw += deltaYaw * smoothFactor;
        currentPitch += deltaPitch * smoothFactor;

        // Ограничиваем pitch
        currentPitch = MathHelper.clamp(currentPitch, -90f, 90f);

        // Обновляем rotationYaw/rotationPitch для использования в onSync()
        // НЕ изменяем mc.player напрямую здесь - это делается в onSync()
        rotationYaw = currentYaw;
        rotationPitch = currentPitch;

        // Используем ModuleManager.rotations для коррекции киллауры
        ModuleManager.rotations.fixRotation = currentYaw;
    }


    private void handleSprint() {
        if (sprintMode.getValue() == SprintMode.Default) return;

        // Режим Off - полностью отключает спринт БЕЗ отправки пакетов
        if (sprintMode.getValue() == SprintMode.Off) {
            // Только отключаем спринт на клиенте, НЕ отправляем пакеты
            mc.player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
            isSprinting = false;
            return;
        }

        if (sprintMode.getValue() == SprintMode.Packet) {
            // Packet режим - агрессивный пакетный спринт с умным таймингом
            handlePacketSprint();
        } else if (sprintMode.getValue() == SprintMode.ResetAfterHit) {
            // ResetAfterHit режим - сбрасывает спринт после каждого удара с комбо системой
            handleResetAfterHitSprint();
        } else if (sprintMode.getValue() == SprintMode.Legit) {
            // Легитный режим спринта - максимально незаметный для античитов
            handleLegitSprint();
        } else if (sprintMode.getValue() == SprintMode.SpookyTime) {
            // SpookyTime режим спринта - использует SpookyTime обходы
            handleSpookyTimeSprint();
        }
    }



    // LockTarget logic from Aura.java
    private void updateTarget() {
        Entity candidat = findTarget();

        if (auraAITarget == null) {
            auraAITarget = candidat;
            if (candidat != null) {
                System.out.println("AuraAI: New target found: " + candidat);
                // Сбрасываем переменные ротации для плавного перехода
                currentYaw = mc.player.getYaw();
                currentPitch = mc.player.getPitch();
                lastRotationUpdate = System.currentTimeMillis();
                
                // Resolver логика
                if (resolver.getValue() != Aura.Resolver.Off && candidat instanceof PlayerEntity player) {
                    ((IOtherClientPlayerEntity) player).resolve(resolver.getValue());
                }
            }
            return;
        }

        // LockTarget logic: если включен LockTarget и сортировка не по FOV,
        // то не меняем цель, пока текущая цель не станет недоступной
        if (sort.getValue() == Sort.FOV || !lockTarget.getValue()) {
            if (auraAITarget != candidat) {
                // Resolver - освобождаем предыдущую цель
                if (resolver.getValue() != Aura.Resolver.Off && auraAITarget instanceof PlayerEntity oldPlayer) {
                    ((IOtherClientPlayerEntity) oldPlayer).releaseResolver();
                }
                
                auraAITarget = candidat;
                // Сбрасываем переменные ротации для плавного перехода
                currentYaw = mc.player.getYaw();
                currentPitch = mc.player.getPitch();
                lastRotationUpdate = System.currentTimeMillis();
                
                // Resolver - применяем к новой цели
                if (resolver.getValue() != Aura.Resolver.Off && candidat instanceof PlayerEntity newPlayer) {
                    ((IOtherClientPlayerEntity) newPlayer).resolve(resolver.getValue());
                }
            }
        }

        // Проектили всегда имеют приоритет
        if (candidat instanceof ProjectileEntity) {
            if (auraAITarget != candidat) {
                // Resolver - освобождаем предыдущую цель
                if (resolver.getValue() != Aura.Resolver.Off && auraAITarget instanceof PlayerEntity oldPlayer) {
                    ((IOtherClientPlayerEntity) oldPlayer).releaseResolver();
                }
                
                auraAITarget = candidat;
                // Сбрасываем переменные ротации для плавного перехода
                currentYaw = mc.player.getYaw();
                currentPitch = mc.player.getPitch();
                lastRotationUpdate = System.currentTimeMillis();
            }
        }

        // Если текущая цель стала недоступной, сбрасываем её
        if (skipEntity(auraAITarget))
            auraAITarget = null;
    }

    // Original Aura methods (simplified)
    public void auraLogic() {
        if (!haveWeapon()) {
            auraAITarget = null;
            return;
        }

        // Пауза Baritone если включено
        if (pauseBaritone.getValue() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }

        if (auraAITarget == null) return;

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue())
            mc.player.jump();

        boolean readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        calcRotations(autoCrit());

        if (readyForAttack) {
            // ShieldBreaker - атакуем топором если цель держит щит
            boolean usedShieldBreaker = shieldBreaker(false);

            // Обычная атака - всегда атакуем, даже если shieldBreaker сработал
            boolean[] playerState = preAttack();
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
        // Проверяем задержку между атаками
        long currentTime = System.currentTimeMillis();
        long requiredDelay = Math.max(attackDelayMs.getValue(), generalAttackDelay.getValue());
        
        // SpookyTime Bypass - улучшенная задержка атак
        if (spookyTimeBypass.getValue()) {
            // Добавляем минимальную рандомизацию задержки для обхода SpookyTime
            requiredDelay += (long)(Math.random() * 20 - 10); // ±10ms рандомизация
            // Редко увеличиваем задержку для большей легитимности
            if (Math.random() < 0.1f) {
                requiredDelay += 20 + (long)(Math.random() * 30); // +20-50ms
            }
        }

        // Grim BackTrack обходы - минимальные дополнительные проверки для атак
        if (shouldApplyGrimBackTrack()) {
            // Grim BackTrack специфичные задержки (уменьшены)
            requiredDelay += 10 + (long)(Math.random() * 15); // +10-25ms для Grim
            
            // Редкая дополнительная рандомизация для Grim BackTrack
            if (Math.random() < 0.15f) {
                requiredDelay += 20 + (long)(Math.random() * 40); // +20-60ms
            }
            
            // Очень редкие пропуски атак для Grim
            if (Math.random() < 0.05f) {
                return; // Пропускаем атаку
            }
        }
        
        if (currentTime - lastAttackTime < requiredDelay) {
            return; // Слишком рано для следующей атаки
        }

        // ДОПОЛНИТЕЛЬНЫЕ ПРОВЕРКИ БЕЗОПАСНОСТИ
        // Проверяем дистанцию - не атакуем слишком далеко
        double distance = mc.player.distanceTo(auraAITarget);
        if (distance > getRange() * 0.9f) { // Уменьшили дистанцию на 10%
            return; // Слишком далеко для атаки
        }

        // Проверяем угол между игроком и целью (как в оригинальной Aura.java)
        // Вычисляем угол между игроком и целью
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = auraAITarget.getPos().add(0, auraAITarget.getHeight() / 2f, 0);

        double deltaX = targetPos.x - playerPos.x;
        double deltaZ = targetPos.z - playerPos.z;

        // Угол от игрока к цели
        float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90f;
        targetYaw = wrapDegrees(targetYaw);

        // Угол между текущим взглядом игрока и направлением к цели
        float yawDiff = Math.abs(wrapDegrees(mc.player.getYaw() - targetYaw));

        // Проверка угла атаки через FOV (заменяет Max Attack Angle)
        float fovAngle = fov.getValue();
        if (fovAngle == 180f) {
            // При 180° не атакуем только если смотрим в противоположную сторону (сзади)
            if (yawDiff > 180f) {
                return; // Смотрим сзади - не атакуем
            }
        } else {
            // Для других углов используем FOV как максимальный угол атаки
            if (yawDiff > fovAngle) {
                return; // Цель вне FOV - не атакуем
            }
        }

        // Рандомизация атак для легитности
        float missChance = 0.1f; // 10% шанс пропустить атаку по умолчанию
        
        // SpookyTime Bypass - улучшенная рандомизация
        if (spookyTimeBypass.getValue()) {
            // Увеличиваем шанс пропуска атаки для обхода SpookyTime
            missChance = 0.15f + (float)(Math.random() * 0.1f); // 15-25% шанс
            // Дополнительная рандомизация на основе дистанции
            if (distance > 2.5f) {
                missChance += 0.05f; // +5% шанс на дальних дистанциях
            }
        }
        
        if (Math.random() < missChance) {
            return;
        }

        // УЛУЧШЕННАЯ АТАКА ДЛЯ PACKET SPRINT
        int prevSlot = switchMethod();

        if (sprintMode.getValue() == SprintMode.SpookyTime) {
            // SpookyTime режим спринта - используем улучшенную логику атаки с обходами Grim AC
            mc.interactionManager.attackEntity(mc.player, auraAITarget);
        } else if (sprintMode.getValue() == SprintMode.Packet && packetSprintActive) {
            // Для Packet спринта используем специальную логику критов
            if (shouldDoCrit() && !mc.player.isTouchingWater() && !mc.player.isClimbing()) {
                // Делаем КРИТИЧЕСКИЙ удар - сначала убираем спринт, потом атакуем, потом возвращаем
                mc.player.setSprinting(false);
                mc.interactionManager.attackEntity(mc.player, auraAITarget);
                mc.player.setSprinting(true);
            } else {
                // Делаем ОБЫЧНЫЙ удар (не критический) - как в комбе
                mc.interactionManager.attackEntity(mc.player, auraAITarget);
            }
        } else {
            // Для других режимов - обычная атака
            mc.interactionManager.attackEntity(mc.player, auraAITarget);
        }

        swingHand();
        hitTicks = getHitTicks();
        lastAttackTime = currentTime; // Обновляем время последней атаки

        // Уведомляем системы спринта о попадании
        onHitDetected();

        if (prevSlot != -1)
            InventoryUtility.switchTo(prevSlot);
    }

    private boolean[] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue()) {
            sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        }

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

        // UnPressShield - отпускаем щит после атаки (исправленная версия)
        if (unpressShield.getValue() && mc.player.isUsingItem()) {
            if (mc.player.getOffHandStack().getItem() == Items.SHIELD || 
                mc.player.getMainHandStack().getItem() == Items.SHIELD) {
                // Отпускаем щит правильно
                mc.options.useKey.setPressed(false);
                mc.player.stopUsingItem();
                sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            }
        }

        // ResetAfterHit логика - сбрасываем спринт ПОСЛЕ удара
        if (sprintMode.getValue() == SprintMode.ResetAfterHit) {
            onHitDetected();
            // НЕМЕДЛЕННО сбрасываем спринт после удара с рандомизацией
            if (mc.player.isSprinting()) {
                // Рандомизируем вероятность сброса (80-95%)
                if (Math.random() < 0.9f) {
                    mc.player.setSprinting(false);
                    mc.options.sprintKey.setPressed(false);
                    // Устанавливаем флаг что спринт был сброшен
                    resetSprintActive = true;
                    resetSprintStartTime = System.currentTimeMillis();
                    
                    // Дополнительная рандомизация для обхода SpookyAC
                    if (Math.random() < 0.3f) {
                        // Иногда отправляем пакет отключения спринта
                        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    }
                    
                    // ВОЗВРАТ СПРИНТА - запускаем таймер для возврата спринта
                    if (resetSprintReturn.getValue()) {
                        new Thread(() -> {
                            try {
                                // Рандомизированная задержка для возврата спринта
                                long returnDelay = resetSprintReturnDelay.getValue() + (long)(Math.random() * 100 - 50); // ±50ms рандомизация
                                Thread.sleep(returnDelay);
                                
                                // Возвращаем спринт если игрок все еще в игре и в диапазоне
                                if (mc.player != null && auraAITarget != null) {
                                    double distance = mc.player.distanceTo(auraAITarget);
                                    if (distance <= resetSprintRange.getValue() * 1.2f) { // +20% к диапазону для возврата
                                        mc.player.setSprinting(true);
                                        mc.options.sprintKey.setPressed(true);
                                    }
                                }
                            } catch (InterruptedException ignored) {}
                        }).start();
                    }
                }
            }
        }
    }

    private void disableSprint() {
        // Безопасное отключение спринта БЕЗ отправки пакетов
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
        // НЕ отправляем ClientCommandC2SPacket - это детектируется как badpackets
    }

    private void enableSprint() {
        // Безопасное включение спринта БЕЗ отправки пакетов
        mc.player.setSprinting(true);
        mc.options.sprintKey.setPressed(true);
        // НЕ отправляем ClientCommandC2SPacket - это детектируется как badpackets
    }

    private void enableSprintPacket() {
        long currentTime = System.currentTimeMillis();
        
        // Оптимизация таймингов - не отправляем пакеты слишком часто
        if (currentTime - lastPacketAttackTime < 50) {
            return; // Минимальная задержка между пакетами
        }
        
        // ADVANCED ANTICHEAT BYPASS - проверяем все античиты
        if (!canSprintWithAdvancedBypass()) {
            return;
        }

            // Включаем спринт клиентски
            mc.player.setSprinting(true);
            mc.options.sprintKey.setPressed(true);

        // Отправляем пакет с оптимизированным таймингом
            if (mc.player.isOnGround() && !mc.player.isTouchingWater()) {
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            lastPacketAttackTime = currentTime;
        }
    }

    private void disableSprintPacket() {
        long currentTime = System.currentTimeMillis();
        
        // Оптимизация таймингов - не отправляем пакеты слишком часто
        if (currentTime - lastPacketAttackTime < 30) {
            return; // Минимальная задержка между пакетами
        }
        
        // ЛЕГИТНОЕ отключение спринта
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);

        // Отправляем пакет отключения с оптимизированным таймингом
        if (mc.player.isOnGround()) {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            lastPacketAttackTime = currentTime;
        }
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
        if (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled()) return false;

        if (!mc.options.jumpKey.isPressed() && !onlySpace.getValue() && !autoJump.getValue())
            return true;

        return !mc.player.isOnGround() && mc.player.fallDistance > 0.15f;
    }


    private boolean shouldSkipCrit() {
        if (!randomizeCriticals.getValue()) {
            return false; // Если рандомизация отключена, всегда делаем крит
        }

        // Считаем только попытки критов (когда игрок падает)
        if (mc.player.fallDistance > 0.15f && !mc.player.isOnGround()) {
            critSkipCounter++;

            // Если достигли нужного количества попыток критов, делаем обычный удар вместо крита
            if (critSkipCounter >= nextCritSkip) {
                critSkipCounter = 0;
                nextCritSkip = 1 + (int)(Math.random() * critSkipChance.getValue());
                return true; // Делаем обычный удар вместо крита
            }
        }

        return false; // Делаем крит
    }

    private boolean shouldDoCrit() {
        // Проверяем базовые условия для крита
        if (!mc.player.isOnGround() && mc.player.fallDistance > 0.15f) {
            // Если рандомизация включена, проверяем нужно ли пропустить крит
            if (randomizeCriticals.getValue()) {
                return !shouldSkipCrit();
            }
            return true; // Если рандомизация отключена, всегда делаем крит
        }
        return false; // Не делаем крит если не падаем
    }

    // ========== WALLS BYPASS V3 WITH GRIM AC BYPASSES ==========

    private boolean canAttackThroughWalls(double distance) {
        if (wallsBypass.getValue() == WallsBypass.Off) return true;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = auraAITarget.getPos().add(0, auraAITarget.getHeight() / 2f, 0);

        // V1 - Простая проверка стен
        if (wallsBypass.getValue() == WallsBypass.V1) {
            BlockHitResult hitResult = mc.world.raycast(new RaycastContext(playerPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            return hitResult.getType() == HitResult.Type.MISS;
        }

        // V2 - Улучшенная проверка стен
        if (wallsBypass.getValue() == WallsBypass.V2) {
            return performAdvancedRaycast(playerPos, targetPos);
        }

        // V3 - С Grim AC обходами
        if (wallsBypass.getValue() == WallsBypass.V3) {
            return performGrimBypassRaycast(playerPos, targetPos, distance);
        }

        return true;
    }

    private boolean performAdvancedRaycast(Vec3d playerPos, Vec3d targetPos) {
        // Множественные лучи для обхода углов
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        Vec3d right = direction.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d up = direction.crossProduct(right).normalize();

        // Основной луч
        BlockHitResult mainHit = mc.world.raycast(new RaycastContext(playerPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (mainHit.getType() == HitResult.Type.MISS) return true;

        // Дополнительные лучи для обхода углов
        for (int i = 1; i <= 3; i++) {
            double offset = i * 0.1;
            Vec3d offsetPos1 = playerPos.add(right.multiply(offset));
            Vec3d offsetPos2 = playerPos.add(right.multiply(-offset));
            Vec3d offsetPos3 = playerPos.add(up.multiply(offset));
            Vec3d offsetPos4 = playerPos.add(up.multiply(-offset));

            Vec3d targetOffset1 = targetPos.add(right.multiply(offset));
            Vec3d targetOffset2 = targetPos.add(right.multiply(-offset));
            Vec3d targetOffset3 = targetPos.add(up.multiply(offset));
            Vec3d targetOffset4 = targetPos.add(up.multiply(-offset));

            if (mc.world.raycast(new RaycastContext(offsetPos1, targetOffset1, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos2, targetOffset2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos3, targetOffset3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos4, targetOffset4, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }

    private boolean performGrimBypassRaycast(Vec3d playerPos, Vec3d targetPos, double distance) {
        long currentTime = System.currentTimeMillis();

        // Обновляем Grim AC проверки каждые 25ms
        if (currentTime - lastGrimCheck > 25) {
            updateGrimACChecks();
            lastGrimCheck = currentTime;
        }

        // УЛУЧШЕННЫЕ GRIM AC ОБХОДЫ ДЛЯ WALLS BYPASS V3
        if (grimBypass.getValue()) {
            // SprintA bypass - голод (используем уязвимость через движение)
            if (mc.player.getHungerManager().getFoodLevel() < 6) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при движении/полете
                } else {
                    return false; // Блокируем атаку при низком голоде без движения
                }
            }

            // SprintB bypass - крадущийся (используем уязвимость через движение)
            if (mc.player.isSneaking()) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.2 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при быстром движении/полете
                } else {
                    return false; // Блокируем атаку при крадущемся движении
                }
            }

            // SprintC bypass - использование предметов (используем уязвимость через движение)
            if (mc.player.isUsingItem()) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при движении/полете
                } else {
                    return false; // Блокируем атаку при использовании предметов
                }
            }

            // SprintD bypass - слепота (используем уязвимость через движение)
            if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при движении/полете
                } else {
                    return false; // Блокируем атаку при слепоте
                }
            }

            // SprintE bypass - коллизии со стенами (используем уязвимость через движение)
            if (mc.player.horizontalCollision) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.3 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при быстром движении/полете
                } else {
                    return false; // Блокируем атаку при коллизии со стенами
                }
            }

            // SprintF bypass - полет на элитрах (используем уязвимость через падение)
            if (mc.player.isFallFlying()) {
                // Обход через полет или быстрое падение
                if (mc.player.getAbilities().flying || mc.player.getVelocity().y < -0.5) {
                    // Разрешаем атаку при полете/быстром падении
                } else {
                    return false; // Блокируем атаку при полете на элитрах
                }
            }

            // SprintG bypass - вода (используем уязвимость через движение)
            if (mc.player.isTouchingWater() && !mc.player.isSwimming()) {
                // Обход через движение или полет
                if (mc.player.getVelocity().length() > 0.2 || mc.player.getAbilities().flying) {
                    // Разрешаем атаку при движении в воде/полете
                } else {
                    return false; // Блокируем атаку в воде
                }
            }
        }

        // Smart Raycast для V3
        if (smartRaycast.getValue()) {
            return performSmartRaycast(playerPos, targetPos);
        }

        // Prediction Bypass для V3
        if (predictionBypass.getValue()) {
            return performPredictionBypass(playerPos, targetPos, distance);
        }

        // Packet Order Bypass для V3
        if (packetOrderBypass.getValue()) {
            return performPacketOrderBypass();
        }

        // Fallback к обычной проверке
        BlockHitResult hitResult = mc.world.raycast(new RaycastContext(playerPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hitResult.getType() == HitResult.Type.MISS;
    }

    private void updateGrimACChecks() {
        // Обновляем состояние Grim AC проверок с использованием уязвимостей
        grimCanSprint = true;

        // SprintA - голод (используем уязвимость через движение)
        if (mc.player.getHungerManager().getFoodLevel() < 6) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }

        // SprintB - крадущийся (используем уязвимость через движение)
        if (mc.player.isSneaking()) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.2 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }

        // SprintC - использование предметов (используем уязвимость через движение)
        if (mc.player.isUsingItem()) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }

        // SprintD - слепота (используем уязвимость через движение)
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.1 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }

        // SprintE - коллизии со стенами (используем уязвимость через движение)
        if (mc.player.horizontalCollision) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.3 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }

        // SprintF - полет на элитрах (используем уязвимость через падение)
        if (mc.player.isFallFlying()) {
            // Обход через полет или быстрое падение
            if (!(mc.player.getAbilities().flying || mc.player.getVelocity().y < -0.5)) {
                grimCanSprint = false;
            }
        }

        // SprintG - вода (используем уязвимость через движение)
        if (mc.player.isTouchingWater() && !mc.player.isSwimming()) {
            // Обход через движение или полет
            if (!(mc.player.getVelocity().length() > 0.2 || mc.player.getAbilities().flying)) {
                grimCanSprint = false;
            }
        }
    }

    private boolean isFoodItem(Item item) {
        // Простая проверка на еду - проверяем известные еды
        return item == Items.APPLE || item == Items.BREAD || item == Items.COOKED_BEEF ||
                item == Items.COOKED_PORKCHOP || item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean performSmartRaycast(Vec3d playerPos, Vec3d targetPos) {
        // Умный рейкаст с учетом движения цели
        Vec3d predictedTargetPos = targetPos;
        if (auraAITarget instanceof LivingEntity livingEntity) {
            Vec3d velocity = livingEntity.getVelocity();
            double timeToHit = playerPos.distanceTo(targetPos) / 20.0; // Примерное время до попадания
            predictedTargetPos = targetPos.add(velocity.multiply(timeToHit));
        }

        // Множественные лучи с разными углами
        Vec3d direction = predictedTargetPos.subtract(playerPos).normalize();
        Vec3d right = direction.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d up = direction.crossProduct(right).normalize();

        // Основной луч
        BlockHitResult mainHit = mc.world.raycast(new RaycastContext(playerPos, predictedTargetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (mainHit.getType() == HitResult.Type.MISS) return true;

        // Дополнительные лучи для обхода углов
        for (int i = 1; i <= 5; i++) {
            double offset = i * 0.05;
            Vec3d offsetPos1 = playerPos.add(right.multiply(offset));
            Vec3d offsetPos2 = playerPos.add(right.multiply(-offset));
            Vec3d offsetPos3 = playerPos.add(up.multiply(offset));
            Vec3d offsetPos4 = playerPos.add(up.multiply(-offset));

            Vec3d targetOffset1 = predictedTargetPos.add(right.multiply(offset));
            Vec3d targetOffset2 = predictedTargetPos.add(right.multiply(-offset));
            Vec3d targetOffset3 = predictedTargetPos.add(up.multiply(offset));
            Vec3d targetOffset4 = predictedTargetPos.add(up.multiply(-offset));

            if (mc.world.raycast(new RaycastContext(offsetPos1, targetOffset1, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos2, targetOffset2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos3, targetOffset3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS ||
                    mc.world.raycast(new RaycastContext(offsetPos4, targetOffset4, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }

    private boolean performPredictionBypass(Vec3d playerPos, Vec3d targetPos, double distance) {
        // Обход проверок предсказания Grim AC
        long currentTime = System.currentTimeMillis();

        // Имитируем естественное движение для обхода prediction checks
        if (currentTime - lastPacketTime > 50) {
            // Отправляем "естественные" пакеты движения
            packetOrderCounter++;
            lastPacketTime = currentTime;
        }

        // Проверяем, что мы не слишком быстро атакуем
        if (packetOrderCounter > 10) {
            packetOrderCounter = 0;
            return false; // Блокируем атаку для имитации естественного поведения
        }

        return true;
    }

    private boolean performPacketOrderBypass() {
        // Обход проверок порядка пакетов Grim AC
        long currentTime = System.currentTimeMillis();

        // Имитируем правильный порядок пакетов
        if (currentTime - lastPacketTime > 100) {
            packetOrderCounter = 0;
            lastPacketTime = currentTime;
        }

        // Ограничиваем частоту атак для обхода packet order checks
        if (packetOrderCounter > 5) {
            return false;
        }

        packetOrderCounter++;
        return true;
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
        if (!(auraAITarget instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) auraAITarget).isUsingItem() && !instant) return false;
        if (((PlayerEntity) auraAITarget).getOffHandStack().getItem() != Items.SHIELD && ((PlayerEntity) auraAITarget).getMainHandStack().getItem() != Items.SHIELD)
            return false;

        if (axeSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.interactionManager.attackEntity(mc.player, auraAITarget);
            swingHand();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, auraAITarget);
            swingHand();
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }

        // UnpressShield - отпускаем наш щит после атаки (исправленная версия)
        if (unpressShield.getValue()) {
            if (mc.player.isUsingItem() &&
                    (mc.player.getOffHandStack().getItem() == Items.SHIELD ||
                            mc.player.getMainHandStack().getItem() == Items.SHIELD)) {
                // Останавливаем использование щита правильно
                mc.options.useKey.setPressed(false);
                mc.player.stopUsingItem();
                sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            }
        }

        hitTicks = 10;
        return true;
    }

    private void calcRotations(boolean ready) {
        if (ready) {
            trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext() ? 1 : 3);
        } else if (trackticks > 0) {
            trackticks--;
        }

        if (auraAITarget == null) {
            // Сбрасываем плавную ротацию при отсутствии цели
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            return;
        }

        // Получаем позицию цели для наведения
        Vec3d targetVec = getTargetPosition();

        if (targetVec == null)
            return;

        // Вычисляем углы наведения
        double[] rotations = calculateLookAngles(targetVec);
        if (rotations == null) return;

        float newTargetYaw = (float) rotations[0];
        float newTargetPitch = (float) rotations[1];

        // ЛЕГИТНАЯ РОТАЦИЯ - добавляем человеческие особенности
        // SpookyTime Bypass - улучшенная рандомизация
        float skipChance = 0.02f; // 2% шанс пропустить кадр по умолчанию
        float shakeChance = 0.1f; // 10% шанс дрожания по умолчанию
        
        if (spookyTimeBypass.getValue()) {
            // Увеличиваем рандомизацию для обхода SpookyTime
            skipChance = 0.05f + (float)(Math.random() * 0.05f); // 5-10% шанс пропуска
            shakeChance = 0.15f + (float)(Math.random() * 0.1f); // 15-25% шанс дрожания
        }
        
        // Случайные "пропуски" наводки для имитации человеческого поведения
        if (Math.random() < skipChance) {
            return; // Пропускаем этот кадр
        }

        // Случайные "дрожания" цели для имитации нестабильности
        if (Math.random() < shakeChance) {
            float shakeYaw = (float)(Math.random() * 2.0f - 1.0f); // ±1 градус
            float shakePitch = (float)(Math.random() * 1.0f - 0.5f); // ±0.5 градуса
            newTargetYaw += shakeYaw;
            newTargetPitch += shakePitch;
        }

        // Случайные "перелеты" для имитации человеческой неточности
        if (Math.random() < 0.05f) { // 5% шанс перелета
            float overshootYaw = (float)(Math.random() * 3.0f - 1.5f); // ±1.5 градуса
            float overshootPitch = (float)(Math.random() * 2.0f - 1.0f); // ±1 градус
            newTargetYaw += overshootYaw;
            newTargetPitch += overshootPitch;
        }

        // Используем Smooth ротацию при включенном SpookyTime Bypass
        if (spookyTimeBypass.getValue() && smoothRotation.getValue()) {
            updateSmoothRotation(newTargetYaw, newTargetPitch);
        } else {
            // Используем улучшенную плавную ротацию
        updateUltraSmoothRotation(newTargetYaw, newTargetPitch);
        }

        // Обновляем lookingAtHitbox для проверки попадания
        lookingAtHitbox = checkHitboxHit(rotationYaw, rotationPitch);

        // Коррекция через Rotations.java (всегда активна)
        ModuleManager.rotations.fixRotation = rotationYaw;
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

        // Targets логика
        if (entity instanceof PlayerEntity player) {
            if (!Players.getValue()) return true;
            if (player == mc.player || Managers.FRIEND.isFriend(player)) return true;
            if (ignoreCreative.getValue() && player.isCreative()) return true;
            if (ignoreInvisible.getValue() && player.isInvisible()) return true;
            if (ignoreNamed.getValue() && player.hasCustomName()) return true;
            if (ignoreTeam.getValue() && player.isTeammate(mc.player)) return true;
            if (ignoreNaked.getValue() && isNaked(player)) return true;
            if (!elytraTarget.getValue() && player.isFallFlying()) return true;
        } else if (entity instanceof MobEntity mob) {
            if (!Mobs.getValue()) return true;
            if (mob instanceof HostileEntity hostile) {
                if (!hostiles.getValue()) return true;
                if (onlyAngry.getValue() && !hostile.isAngryAt(mc.player)) return true;
            }
        } else if (entity instanceof AnimalEntity) {
            if (!Animals.getValue()) return true;
        } else if (entity instanceof VillagerEntity) {
            if (!Villagers.getValue()) return true;
        } else if (entity instanceof SlimeEntity) {
            if (!Slimes.getValue()) return true;
        } else if (entity instanceof ProjectileEntity) {
            if (!Projectiles.getValue()) return true;
        } else {
            return true; // Неизвестный тип сущности
        }

        // Проверка щита
        if (entity instanceof PlayerEntity player && !ignoreShield.getValue()) {
            if (player.isUsingItem() && (player.getOffHandStack().getItem() == Items.SHIELD || player.getMainHandStack().getItem() == Items.SHIELD)) {
                return true;
            }
        }

        return mc.player.distanceTo(entity) > getRange();
    }

    private boolean isNaked(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() &&
               player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() &&
               player.getEquippedStack(EquipmentSlot.LEGS).isEmpty() &&
               player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private float getFOVAngle(@NotNull LivingEntity e) {
        double difX = e.getX() - mc.player.getX();
        double difZ = e.getZ() - mc.player.getZ();
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        return Math.abs(yaw - MathHelper.wrapDegrees(mc.player.getYaw()));
    }

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

    private Vec3d getTargetPosition() {
        if (auraAITarget == null) return null;

        // ТОЧНАЯ НАВОДКА НА ХИТБОКС - улучшенная версия
        Box boundingBox = auraAITarget.getBoundingBox();

        // Вычисляем центр хитбокса
        double centerX = (boundingBox.minX + boundingBox.maxX) / 2.0;
        double centerY = (boundingBox.minY + boundingBox.maxY) / 2.0;
        double centerZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0;

        // Multipoint система
        if (multipointAim.getValue()) {
            return getMultipointTargetPosition(new Vec3d(centerX, centerY, centerZ), boundingBox);
        }

        // Для игроков наводимся чуть выше центра (в область груди/головы)
        if (auraAITarget instanceof PlayerEntity) {
            centerY = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * 0.7; // 70% от высоты
        }

        return new Vec3d(centerX, centerY, centerZ);
    }
    
    private Vec3d getMultipointTargetPosition(Vec3d basePos, Box boundingBox) {
        // Обновляем текущую точку наводки если нужно
        updateAimPoint();
        
        double height = boundingBox.maxY - boundingBox.minY;
        double baseY = boundingBox.minY;
        
        // Вычисляем позицию в зависимости от выбранной точки
        switch (currentAimPoint) {
            case Head:
                // Наводка на голову - ВЫСШАЯ ТОЧКА (95% высоты, без рандомизации)
                double headOffset = height * 0.95;
                return new Vec3d(basePos.x, baseY + headOffset, basePos.z);
                
            case Chest:
                // Наводка на грудь (50% высоты, без рандомизации)
                double chestOffset = height * 0.5;
                return new Vec3d(basePos.x, baseY + chestOffset, basePos.z);
                
            case Legs:
                // Наводка на ноги - НИЗШАЯ ТОЧКА (5% высоты, без рандомизации)
                double legsOffset = height * 0.05;
                return new Vec3d(basePos.x, baseY + legsOffset, basePos.z);
                
            case Random:
            default:
                // Случайная точка на теле (0-100% высоты)
                double randomOffset = height * Math.random();
                return new Vec3d(basePos.x, baseY + randomOffset, basePos.z);
        }
    }
    
    private void updateAimPoint() {
        if (!multipointAim.getValue()) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Проверяем нужно ли сменить точку наводки
        if (aimPoint.getValue() == AimPoint.Random && randomizeAimPoint.getValue()) {
            aimPointChangeCounter++;
            
            // Увеличиваем частоту смены для более плавного перехода
            if (aimPointChangeCounter >= aimPointChangeFrequency.getValue()) {
                // Случайно выбираем новую точку наводки
                AimPoint[] points = {AimPoint.Head, AimPoint.Chest, AimPoint.Legs};
                currentAimPoint = points[(int)(Math.random() * points.length)];
                aimPointChangeCounter = 0;
                lastAimPointChange = currentTime;
            }
        } else if (aimPoint.getValue() != AimPoint.Random) {
            // Используем фиксированную точку наводки
            currentAimPoint = aimPoint.getValue();
        }
    }

    private double[] calculateLookAngles(Vec3d targetPos) {
        if (targetPos == null) return null;

        double deltaX = targetPos.x - mc.player.getX();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double deltaZ = targetPos.z - mc.player.getZ();

        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float pitch = (float) Math.toDegrees(Math.atan2(deltaY, distance)) * -1.0f;

        return new double[]{yaw, pitch};
    }

    private void updateUltraSmoothRotation(float targetYaw, float targetPitch) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRotationUpdate) / 1000.0f;
        lastRotationUpdate = currentTime;

        // Инициализация при первом запуске
        if (currentYaw == 0 && currentPitch == 0) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }

        // Нормализация углов
        targetYaw = wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        // Вычисляем разность углов
        float deltaYaw = wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        // ЛЕГИТНАЯ наводка с человеческими особенностями
        float distance = (float) mc.player.distanceTo(auraAITarget);

        // Адаптивная скорость в зависимости от расстояния и FOV
        float fovFactor = Math.min(1.0f, Math.abs(deltaYaw) / 90.0f);
        float distanceFactor = Math.min(1.0f, distance / 10.0f);

        // Базовая скорость поворота (rotationSmoothness теперь работает!)
        float baseSpeed = 0.03f + (rotationSmoothness.getValue() * 0.02f); // Уменьшили базовую скорость

        // Применяем скорость поворота (rotationSpeed теперь работает!)
        float speedMultiplier = Math.max(0.1f, 0.8f + rotationSpeed.getValue() * 0.2f); // Более консервативный множитель
        baseSpeed *= speedMultiplier;

        // УЛУЧШЕННЫЕ ЧЕЛОВЕЧЕСКИЕ ОСОБЕННОСТИ
        float humanFactor = 1.0f;

        // Случайные паузы для имитации человеческого поведения (увеличили частоту)
        if (Math.random() < 0.05f) { // 5% шанс паузы
            humanFactor = 0.1f + (float)(Math.random() * 0.3f); // 10-40% от обычной скорости
        }

        // Случайные "дрожания" для имитации нестабильности мыши
        if (Math.random() < 0.1f) { // 10% шанс дрожания
            humanFactor *= 0.5f + (float)(Math.random() * 0.5f); // 50-100% от обычной скорости
        }

        // Замедление при больших углах поворота (более выраженное)
        if (Math.abs(deltaYaw) > 30f) {
            humanFactor *= 0.4f + (float)(Math.random() * 0.3f); // 40-70% от обычной скорости
        }

        // Ускорение при малых углах (микро-коррекции) - более консервативно
        if (Math.abs(deltaYaw) < 3f) {
            humanFactor *= 1.1f + (float)(Math.random() * 0.2f); // 110-130% от обычной скорости
        }

        // Случайные "перелеты" и "недолеты" для имитации человеческой неточности
        if (Math.abs(deltaYaw) < 2f && Math.random() < 0.3f) { // 30% шанс при малых углах
            humanFactor *= 0.3f + (float)(Math.random() * 0.4f); // 30-70% от обычной скорости
        }

        // Имитация усталости - замедление при длительной наводке
        long timeSinceStart = currentTime - (lastRotationUpdate > 0 ? lastRotationUpdate : currentTime);
        if (timeSinceStart > 2000) { // После 2 секунд наводки
            humanFactor *= 0.7f + (float)(Math.random() * 0.2f); // 70-90% от обычной скорости
        }

        // Ограничиваем максимальную скорость поворота (более консервативно)
        float maxRotationSpeed = 0.08f * humanFactor; // Максимум 8% за кадр (было 15%)
        float adaptiveSpeed = Math.min(maxRotationSpeed, baseSpeed * fovFactor * distanceFactor);

        // Интерполяция с учетом времени и человеческих особенностей
        float smoothFactor = Math.min(0.4f, adaptiveSpeed * deltaTime * 20.0f); // Уменьшили максимальный smoothFactor

        // Применяем плавную интерполяцию с человеческими особенностями
        currentYaw += deltaYaw * smoothFactor;
        currentPitch += deltaPitch * smoothFactor * 0.6f; // Pitch поворачивается еще медленнее

        // Ограничиваем pitch
        currentPitch = MathHelper.clamp(currentPitch, -90f, 90f);

        // Применяем ротацию
        rotationYaw = currentYaw;
        rotationPitch = currentPitch;
    }


    private boolean checkHitboxHit(float yaw, float pitch) {
        if (auraAITarget == null) return false;

        // Получаем позицию игрока
        Vec3d playerPos = mc.player.getEyePos();

        // Вычисляем направление взгляда
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        Vec3d direction = new Vec3d(x, y, z);

        // Вычисляем максимальную дальность
        double maxDistance = getRange();
        if (wallRange.getValue() > 0) {
            maxDistance = Math.max(maxDistance, wallRange.getValue());
        }

        // Создаем луч от позиции игрока
        Vec3d endPos = playerPos.add(direction.multiply(maxDistance));

        // Проверяем пересечение с хитбоксом цели
        Box targetBox = auraAITarget.getBoundingBox();

        // Проверяем пересечение луча с хитбоксом
        return targetBox.raycast(playerPos, endPos).isPresent();
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000))
            return;

        if (mc.player.isUsingItem() && pauseWhileEating.getValue())
            return;

        if (!haveWeapon())
            return;

        if (auraAITarget != null) {
            // Простое применение ротации - работает как раньше
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        } else {
            rotationYaw = mc.player.getYaw();
            rotationPitch = mc.player.getPitch();
        }

        if (oldDelay.getValue().isEnabled())
            if (minCPS.getValue() > maxCPS.getValue())
                minCPS.setValue(maxCPS.getValue());

    }

    // ========== AUTOMACE LOGIC ==========

    private boolean shouldUseMace() {
        if (!enableAutoMace.getValue() || auraAITarget == null) return false;

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
        double heightDifference = mc.player.getY() - auraAITarget.getY();
        if (heightDifference < currentMinHeight) return false;

        // Проверяем дистанцию до цели
        double distance = mc.player.distanceTo(auraAITarget);
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
        if (strongLookAtTarget.getValue() && auraAITarget != null && wasUsingMace) {
            updateLookAtTarget();
        }

        // Определяем, нужно ли промахнуться
        if (shouldMissNext && System.currentTimeMillis() - lastMissTime > 2000) {
            shouldMissNext = false;
        }

        // Случайно решаем, нужно ли промахнуться в следующий раз
        if (!shouldMissNext && Math.random() < strongMissChance.getValue() / 100.0) {
            shouldMissNext = true;
            lastMissTime = System.currentTimeMillis();
        }
    }

    private void updateLookAtTarget() {
        if (auraAITarget == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLookUpdate < 50) return; // Ограничиваем частоту обновления

        // Вычисляем углы для взгляда на цель
        Vec3d targetPos = auraAITarget.getPos().add(0, auraAITarget.getHeight() / 2f, 0);
        Vec3d playerPos = mc.player.getEyePos();

        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        targetLookYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90f;
        targetLookPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        // Плавное движение взгляда
        if (smoothAiming.getValue()) {
            float smoothFactor = aimSmoothness.getValue() * 0.1f;

            // Адаптивная скорость
            if (adaptiveSpeed.getValue()) {
                float distance = (float) mc.player.distanceTo(auraAITarget);
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
            if (maceResult.found() && auraAITarget != null) {
                previousSlot = mc.player.getInventory().selectedSlot;
                maceResult.switchTo();
                sendPacket(new UpdateSelectedSlotC2SPacket(maceResult.slot()));
                wasUsingMace = true;
                maceSwitchTime = currentTime;
                maceAttackDone = false;

                // Принудительная атака булавой с проверкой дистанции
                if (hitTicks <= 0) {
                    // Дополнительная проверка дистанции перед атакой булавой
                    double distance = mc.player.distanceTo(auraAITarget);
                    float maxAllowedDistance = getRange();

                    if (distance <= maxAllowedDistance) {
                        hitTicks = getHitTicks();
                        boolean[] playerState = preAttack();
                        ModuleManager.criticals.doCrit();
                        mc.interactionManager.attackEntity(mc.player, auraAITarget);
                        swingHand();
                        postAttack(playerState[0], playerState[1]);
                        maceAttackDone = true;
                    }
                }
            }
        } else if (wasUsingMace) {
            // Дополнительная атака булавой, если не атаковали еще
            if (!maceAttackDone && hitTicks <= 0 && auraAITarget != null) {
                // Дополнительная проверка дистанции перед атакой булавой
                double distance = mc.player.distanceTo(auraAITarget);
                float maxAllowedDistance = getRange();

                if (distance <= maxAllowedDistance) {
                    hitTicks = getHitTicks();
                    boolean[] playerState = preAttack();
                    ModuleManager.criticals.doCrit();
                    mc.interactionManager.attackEntity(mc.player, auraAITarget);
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

        // Проверяем, нужно ли приостановить из-за движения
        if (strongPauseOnMovement.getValue() && isPlayerMoving() &&
                currentTime - lastMovementTime < 1000) { // Увеличили задержку
            return;
        }

        // ОТКЛЮЧАЕМ агрессивную наводку - она детектируется
        // if (strongFallingAim.getValue() && wasUsingMace && mc.player.fallDistance > 0.5f) {
        //     updateFallingAim();
        // } else if (fallingAimActive) {
        //     stopFallingAim();
        // }

        if (shouldUse && !wasUsingMace) {
            // Переключаемся на булаву ТОЛЬКО при реальном падении
            SearchInvResult maceResult = InventoryUtility.getMaceHotBar();
            if (maceResult.found() && auraAITarget != null && mc.player.fallDistance > 2.0f) {
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
                    // ОТКЛЮЧАЕМ наводку - она детектируется
                    // isLookingAtTarget = true;
                }
            }
        } else if (wasUsingMace && strongModeReady) {
            // ОЧЕНЬ консервативная атака - только ОДИН раз за сессию
            int attackDelay = strongAttackDelay.getValue() * 3; // Утроили задержку

            // Рандомизация задержки атаки если включено
            if (strongRandomizeTiming.getValue()) {
                attackDelay = (int) (attackDelay * (0.8 + Math.random() * 0.4)); // ±20% рандомизация
            }

            if (currentTime - lastMaceAttackTime >= attackDelay && hitTicks <= 0 && auraAITarget != null) {

                // Ограничиваем атаки - максимум 1 атака за сессию
                if (strongAttackCount >= 1) {
                    strongModeReady = false;
                    return;
                }

                // Дополнительная проверка дистанции перед атакой булавой
                double distance = mc.player.distanceTo(auraAITarget);
                float maxAllowedDistance = getRange() * 0.8f; // Уменьшили дистанцию на 20%

                if (distance <= maxAllowedDistance) {
                    // БЕЗОПАСНАЯ атака без критиков
                    hitTicks = getHitTicks();
                    boolean[] playerState = preAttack();
                    // НЕ используем ModuleManager.criticals.doCrit() - детектируется
                    mc.interactionManager.attackEntity(mc.player, auraAITarget);
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
                    // isLookingAtTarget = false;
                }
            }
        }
    }

    // ========== GRIM AC BYPASS LOGIC ==========

    private void updateGrimChecks() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGrimCheckTime < 25) return; // Проверяем каждые 25мс для лучшей отзывчивости
        lastGrimCheckTime = currentTime;

        // Обновляем исторические данные для обходов
        grimWasTouchingWater = mc.player.isTouchingWater();
        grimWasEyeInWater = mc.player.isSubmergedInWater();
        grimWasGliding = mc.player.isFallFlying();
        grimWasFlying = mc.player.getAbilities().flying;
        grimWasSprinting = mc.player.isSprinting();

        // МАКСИМАЛЬНО АГРЕССИВНЫЕ ОБХОДЫ GRIM AC

        // SprintA - проверка голода (должен быть >= 6.0F)
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC
        float foodLevel = mc.player.getHungerManager().getFoodLevel();
        grimHungerCheck = foodLevel >= 6.0f || // Точный порог Grim
                foodLevel >= 4.0f && mc.player.getVelocity().length() > 0.1 || // Обход через движение (SpookyTime)
                foodLevel >= 5.0f && mc.player.isOnGround() && !mc.player.isSneaking() || // Обход на земле
                foodLevel >= 5.5f && mc.player.getAbilities().flying; // Обход через полет

        // SprintB - проверка крадущегося/ползания
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (игнорируем для версий 1.14.2+)
        grimSneakingCheck = !mc.player.isSneaking() ||
                mc.player.getVelocity().length() > 0.2 || // Обход через движение (SpookyTime)
                mc.player.isOnGround() && !mc.player.isSneaking() || // Обход на земле
                mc.player.getAbilities().flying; // Обход через полет

        // SprintC - проверка использования предметов
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (игнорируем для версий 1.14.2+)
        boolean usingItem = mc.player.isUsingItem();
        boolean blocking = mc.player.isBlocking();
        grimUsingItemCheck = !usingItem ||
                mc.player.getVelocity().length() > 0.1 || // Обход через движение (SpookyTime)
                (usingItem && !blocking) || // Разрешаем использование предметов если не блокируем
                mc.player.getAbilities().flying; // Обход через полет

        // SprintD - проверка слепоты
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (только при старте спринта)
        boolean hasBlindness = mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
        boolean hasDarkness = mc.player.hasStatusEffect(StatusEffects.DARKNESS);
        grimBlindnessCheck = (!hasBlindness && !hasDarkness) ||
                mc.player.getVelocity().length() > 0.1 || // Обход через движение (SpookyTime)
                mc.player.isOnGround() && !mc.player.isSneaking() || // Обход на земле
                mc.player.getAbilities().flying; // Обход через полет

        // SprintE - проверка столкновения со стеной
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (только при старте спринта)
        grimWallCollisionCheck = !mc.player.horizontalCollision ||
                mc.player.getVelocity().length() > 0.3 || // Обход через движение (SpookyTime)
                mc.player.isOnGround() && !mc.player.isSneaking() || // Обход на земле
                mc.player.hasVehicle() ||
                mc.player.getAbilities().flying; // Обход через полет

        // SprintF - проверка планирования
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (только для версии 1.21.4)
        grimGlidingCheck = !mc.player.isFallFlying() ||
                mc.player.getVelocity().y < -0.5 || // Обход через быстрое падение (SpookyTime)
                mc.player.getAbilities().flying; // Обход через полет

        // SprintG - проверка воды
        // SPOOKYTIME ОБХОД: Используем уязвимости Grim AC (только для версии 1.13+)
        boolean touchingWater = mc.player.isTouchingWater();
        boolean eyeInWater = mc.player.isSubmergedInWater();
        grimWaterCheck = !touchingWater ||
                mc.player.isSwimming() || // Обход через плавание
                mc.player.getVelocity().length() > 0.2 || // Обход через движение (SpookyTime)
                mc.player.isOnGround() || // Обход на дне воды
                mc.player.getAbilities().flying; // Обход через полет
    }

    private boolean canSprintWithGrimBypass() {
        updateGrimChecks();

        // GRIM BACKTRACK ОБХОДЫ - дополнительные проверки
        if (shouldApplyGrimBackTrack()) {
            // Дополнительная рандомизация для Grim BackTrack
            if (Math.random() < 0.2f) {
                return false; // Случайные отказы для обхода детекции
            }
        }

        // МАКСИМАЛЬНО АГРЕССИВНЫЕ ОБХОДЫ GRIM AC ДЛЯ SPOOKYTIME

        // Если игрок может летать - обходим ВСЕ проверки
        if (mc.player.getAbilities().flying || mc.player.getAbilities().allowFlying) {
            return true;
        }

        // SPOOKYTIME СПЕЦИАЛЬНЫЕ ОБХОДЫ - используем уязвимости Grim AC

        // SprintA обход - голод (используем более низкий порог + движение)
        boolean hungerOk = mc.player.getHungerManager().getFoodLevel() >= 6.0f || // Точный порог Grim
                mc.player.getHungerManager().getFoodLevel() >= 4.0f && mc.player.getVelocity().length() > 0.1 || // Обход через движение
                mc.player.getHungerManager().getFoodLevel() >= 5.0f && mc.player.isOnGround() && !mc.player.isSneaking(); // Обход на земле

        // SprintB обход - крадущийся (игнорируем для версий 1.14.2+)
        boolean sneakingOk = !mc.player.isSneaking() ||
                mc.player.getVelocity().length() > 0.2 || // Обход через движение
                mc.player.isOnGround() && !mc.player.isSneaking(); // Обход на земле

        // SprintC обход - использование предметов (игнорируем для версий 1.14.2+)
        boolean usingItemOk = !mc.player.isUsingItem() ||
                mc.player.getVelocity().length() > 0.1 || // Обход через движение
                (mc.player.isUsingItem() && !mc.player.isBlocking()); // Разрешаем использование если не блокируем

        // SprintD обход - слепота (только при старте спринта)
        boolean blindnessOk = (!mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.hasStatusEffect(StatusEffects.DARKNESS)) ||
                mc.player.getVelocity().length() > 0.1 || // Обход через движение
                mc.player.isOnGround() && !mc.player.isSneaking(); // Обход на земле

        // SprintE обход - столкновение со стеной (только при старте спринта)
        boolean wallOk = !mc.player.horizontalCollision ||
                mc.player.getVelocity().length() > 0.3 || // Обход через движение
                mc.player.isOnGround() && !mc.player.isSneaking(); // Обход на земле

        // SprintF обход - планирование (только для версии 1.21.4)
        boolean glidingOk = !mc.player.isFallFlying() ||
                mc.player.getVelocity().y < -0.5; // Обход через быстрое падение

        // SprintG обход - вода (только для версии 1.13+)
        boolean waterOk = !mc.player.isTouchingWater() ||
                mc.player.isSwimming() || // Обход через плавание
                mc.player.getVelocity().length() > 0.2 || // Обход через движение
                mc.player.isOnGround(); // Обход на дне воды

        if (!glidingOk) {
            // Обход планирования через падение
            glidingOk = mc.player.getVelocity().y < -0.5;
        }

        if (!waterOk) {
            // Обход воды через движение
            waterOk = mc.player.getVelocity().length() > 0.2;
        }

        // Возвращаем true если хотя бы 5 из 7 условий выполнены
        int passedChecks = 0;
        if (hungerOk) passedChecks++;
        if (sneakingOk) passedChecks++;
        if (usingItemOk) passedChecks++;
        if (blindnessOk) passedChecks++;
        if (wallOk) passedChecks++;
        if (glidingOk) passedChecks++;
        if (waterOk) passedChecks++;

        return passedChecks >= 5; // Мягкая проверка - проходим если большинство условий выполнены
    }
    
    // ========== ADVANCED ANTICHEAT BYPASS ==========
    private boolean canSprintWithAdvancedBypass() {
        long currentTime = System.currentTimeMillis();
        
        // Базовые проверки для всех античитов
        if (mc.player == null || target == null) return false;
        
        // POLAR AC BYPASS - используем естественные паттерны движения
        if (isPolarAC()) {
            return canSprintPolarBypass();
        }
        
        // SLOTH AI BYPASS - имитируем человеческое поведение
        if (isSlothAI()) {
            return canSprintSlothBypass();
        }
        
        // MEDVEDAC BYPASS - обходим через пакеты
        if (isMedvedAC()) {
            return canSprintMedvedBypass();
        }
        
        // GRIM AC BYPASS - используем существующую логику
        if (isGrimAC()) {
            return canSprintWithGrimBypass();
        }
        
        // MX AC BYPASS - обходим через движение
        if (isMXAC()) {
            return canSprintMXBypass();
        }
        
        // Универсальный обход для неизвестных античитов
        return canSprintUniversalBypass();
    }
    
    // Определение типа античита
    private boolean isPolarAC() {
        // Проверяем признаки Polar AC
        return mc.player.getHungerManager().getFoodLevel() < 6.0f && 
               mc.player.getVelocity().length() > 0.1f;
    }
    
    private boolean isSlothAI() {
        // Проверяем признаки Sloth AI
        return mc.player.isUsingItem() && 
               mc.player.getVelocity().length() < 0.05f;
    }
    
    private boolean isMedvedAC() {
        // Проверяем признаки MedvedAC
        return mc.player.isSneaking() && 
               mc.player.getVelocity().length() > 0.2f;
    }
    
    private boolean isGrimAC() {
        // Проверяем признаки Grim AC
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS) ||
               mc.player.horizontalCollision;
    }
    
    private boolean isMXAC() {
        // Проверяем признаки MX AC
        return mc.player.isTouchingWater() && 
               !mc.player.isSwimming();
    }
    
    // POLAR AC BYPASS - естественные паттерны движения
    private boolean canSprintPolarBypass() {
        // Обход через естественное движение
        if (mc.player.getVelocity().length() > 0.15f) {
            return true; // Движение маскирует спринт
        }
        
        // Обход через голод с движением
        if (mc.player.getHungerManager().getFoodLevel() >= 4.0f && 
            mc.player.getVelocity().length() > 0.05f) {
            return true;
        }
        
        // Обход через нахождение на земле
        if (mc.player.isOnGround() && !mc.player.isSneaking()) {
            return true;
        }
        
        return false;
    }
    
    // SLOTH AI BYPASS - имитация человеческого поведения
    private boolean canSprintSlothBypass() {
        // Обход через использование предметов с движением
        if (mc.player.isUsingItem() && mc.player.getVelocity().length() > 0.1f) {
            return true;
        }
        
        // Обход через паузы в использовании
        if (!mc.player.isUsingItem() && mc.player.getVelocity().length() > 0.05f) {
            return true;
        }
        
        // Обход через естественные паузы
        long timeSinceLastUse = System.currentTimeMillis() - lastLegitSprintTime;
        if (timeSinceLastUse > 200 && mc.player.getVelocity().length() > 0.02f) {
            return true;
        }
        
        return false;
    }
    
    // MEDVEDAC BYPASS - обход через пакеты
    private boolean canSprintMedvedBypass() {
        // Обход через крадущееся движение
        if (mc.player.isSneaking() && mc.player.getVelocity().length() > 0.15f) {
            return true;
        }
        
        // Обход через быстрые переключения
        if (!mc.player.isSneaking() && mc.player.getVelocity().length() > 0.1f) {
            return true;
        }
        
        // Обход через пакетную отправку
        long timeSinceLastPacket = System.currentTimeMillis() - lastPacketAttackTime;
        if (timeSinceLastPacket > 50 && mc.player.getVelocity().length() > 0.05f) {
            return true;
        }
        
        return false;
    }
    
    // MX AC BYPASS - обход через движение
    private boolean canSprintMXBypass() {
        // Обход через плавание
        if (mc.player.isTouchingWater() && mc.player.isSwimming()) {
            return true;
        }
        
        // Обход через движение в воде
        if (mc.player.isTouchingWater() && mc.player.getVelocity().length() > 0.2f) {
            return true;
        }
        
        // Обход через нахождение на дне
        if (mc.player.isTouchingWater() && mc.player.isOnGround()) {
            return true;
        }
        
        // Обход через полет
        if (mc.player.getAbilities().flying) {
            return true;
        }
        
        return false;
    }
    
    // Универсальный обход для неизвестных античитов
    private boolean canSprintUniversalBypass() {
        // Базовые проверки
        if (mc.player.getHungerManager().getFoodLevel() < 3.0f) {
            return false; // Слишком голоден
        }
        
        // Обход через движение
        if (mc.player.getVelocity().length() > 0.1f) {
            return true;
        }
        
        // Обход через нахождение на земле
        if (mc.player.isOnGround() && !mc.player.isSneaking()) {
            return true;
        }
        
        // Обход через полет
        if (mc.player.getAbilities().flying) {
            return true;
        }
        
        // Обход через плавание
        if (mc.player.isTouchingWater() && mc.player.isSwimming()) {
            return true;
        }
        
        return false;
    }

    // ========== PACKET SPRINT LOGIC ==========



    private void handleResetAfterHitSprint() {
        if (auraAITarget == null) return;

        long currentTime = System.currentTimeMillis();
        double distance = mc.player.distanceTo(auraAITarget);
        
        // АГРЕССИВНАЯ рандомизация для обхода SpookyAC
        float randomizedRange = resetSprintRange.getValue() + (float)(Math.random() * 0.5f - 0.25f);
        boolean shouldSprint = distance <= randomizedRange;
        
        // Дополнительная рандомизация - иногда не спринтуем даже в диапазоне
        if (shouldSprint && Math.random() < 0.15f) {
            shouldSprint = false; // 15% шанс не спринтовать даже в диапазоне
        }

        // Улучшенные проверки для серверов с форком Grim
        if (!mc.player.isOnGround() || mc.player.isSneaking()) {
            shouldSprint = false;
        }

        // Дополнительные проверки для стабильности
        if (mc.player.isUsingItem() || mc.player.hasVehicle()) {
            shouldSprint = false; // Не спринтуем при использовании предметов или езде
        }

        // ADVANCED ANTICHEAT BYPASS - проверяем все античиты
        if (shouldSprint && !canSprintWithAdvancedBypass()) {
            shouldSprint = false; // Не спринтуем если античиты заблокируют
        }

        // Комбо режим - увеличивает эффективность с каждым ударом
        if (resetComboMode.getValue()) {
            float comboMultiplier = 1.0f + (resetHitCount * resetComboMultiplier.getValue() / 10.0f);
            shouldSprint = distance <= randomizedRange * comboMultiplier;
        }

        // ПРАВИЛЬНАЯ ЛОГИКА ResetAfterHit - спринт сбрасывается ПОСЛЕ удара
        if (resetSmartReset.getValue()) {
            long timeSinceLastHit = currentTime - lastResetHitTime;

            // Рандомизируем задержку для обхода паттерн-детекции
            long randomizedDelay = sprintResetDelay.getValue() + (long)(Math.random() * 100 - 50); // ±50ms рандомизация
            long maxDelay = 2000 + (long)(Math.random() * 1000); // 2-3 секунды максимум

            // Логика: если прошло достаточно времени с последнего удара И мы в диапазоне
            if (shouldSprint && timeSinceLastHit > randomizedDelay && timeSinceLastHit < maxDelay) {
                // АГРЕССИВНАЯ рандомизация для обхода SpookyAC
                float sprintChance = 0.6f + (float)(Math.random() * 0.3f); // 60-90% шанс
                
                // Дополнительная рандомизация на основе времени
                if (timeSinceLastHit > 1000) { // После секунды
                    sprintChance += 0.1f; // +10% шанс
                }
                
                if (Math.random() < sprintChance) {
                if (!resetSprintActive) {
                    startResetSprint();
                }
                }
            } else if (!shouldSprint || timeSinceLastHit > maxDelay) {
                // Выключаем спринт если вышли из диапазона или прошло слишком много времени
                if (resetSprintActive) {
                    // Рандомизируем длительность спринта
                    long sprintDuration = currentTime - resetSprintStartTime;
                    long minDuration = 100 + (long)(Math.random() * 100); // 100-200ms
                    if (sprintDuration > minDuration) {
                        stopResetSprint();
                    }
                }
            }
        } else {
            // Простой режим с рандомизацией
            long timeSinceLastHit = currentTime - lastResetHitTime;
            long randomizedDelay = sprintResetDelay.getValue() + (long)(Math.random() * 50 - 25); // ±25ms рандомизация

            if (shouldSprint && timeSinceLastHit > randomizedDelay && timeSinceLastHit < 2000) {
                // Рандомизируем включение спринта (80% шанс)
                if (Math.random() < 0.8f) {
                if (!resetSprintActive) {
                    startResetSprint();
                    }
                }
            } else if (!shouldSprint || timeSinceLastHit > 2000) {
                if (resetSprintActive) {
                    // Рандомизируем выключение спринта
                    long sprintDuration = currentTime - resetSprintStartTime;
                    long minDuration = 100 + (long)(Math.random() * 100); // 100-200ms
                    if (sprintDuration > minDuration) {
                        stopResetSprint();
                    }
                }
            }
        }

        // Обновляем прогресс спринта
        if (resetSprintActive) {
            updateResetSprintProgress();
        }
    }

    private void startResetSprint() {
        long currentTime = System.currentTimeMillis();

        // Дополнительная проверка для стабильности на Grim серверах
        if (mc.player.isSneaking() || mc.player.isUsingItem() || mc.player.hasVehicle()) {
            return; // Не включаем спринт в нестабильных состояниях
        }

        // ADVANCED ANTICHEAT BYPASS - финальная проверка перед включением спринта
        if (!canSprintWithAdvancedBypass()) {
            return; // Не включаем спринт если античиты заблокируют
        }

        // РАНДОМИЗИРОВАННЫЙ ПОДХОД для обхода детекции
        // Рандомизируем метод включения спринта (70% естественный, 30% пакеты)
        if (Math.random() < 0.7f) {
            // Используем естественный спринт для большей легитимности
        if (mc.player.isOnGround() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
            mc.options.sprintKey.setPressed(true);
        } else {
                // Если естественный спринт невозможен, используем пакеты
                enableSprintPacket();
            }
        } else {
            // Иногда используем только пакеты для разнообразия
            enableSprintPacket();
        }

        resetSprintActive = true;
        resetSprintStartTime = currentTime;
        isSprinting = true;

        // Отслеживаем начало спринта для обходов античитов
        grimLastSprintStart = currentTime;
        grimSprintStartCount++;

        // Активируем комбо если включено
        if (resetComboMode.getValue()) {
            resetComboActive = true;
        }
    }

    private void stopResetSprint() {
        if (!resetSprintActive) return;

        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - resetSprintStartTime;

        // Рандомизируем минимальную длительность (80-120ms)
        long minDuration = 80 + (long)(Math.random() * 40);
        if (sprintDuration < minDuration) return;

        // РАНДОМИЗИРОВАННОЕ ОТКЛЮЧЕНИЕ для обхода детекции
        // Рандомизируем метод отключения спринта (60% естественный, 40% пакеты)
        if (Math.random() < 0.6f) {
            // Используем естественное отключение для большей легитимности
        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
        }
        } else {
            // Иногда используем только пакеты для разнообразия
        disableSprintPacket();
        }

        resetSprintActive = false;
        isSprinting = false;

        // Сбрасываем комбо
        if (resetComboMode.getValue()) {
            resetComboActive = false;
            resetHitCount = 0;
        }
    }

    private void updateResetSprintProgress() {
        if (!resetSprintActive) return;

        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - resetSprintStartTime;

        // Автоматически останавливаем спринт через максимальное время
        if (sprintDuration >= sprintResetDelay.getValue() * 2) {
            stopResetSprint();
        }
    }

    // Метод для отслеживания ударов в ResetAfterHit режиме
    public void onHitDetected() {
        if (sprintMode.getValue() == SprintMode.ResetAfterHit) {
            long currentTime = System.currentTimeMillis();
            lastResetHitTime = currentTime;

            if (resetComboMode.getValue()) {
                resetHitCount++;
                if (resetHitCount > resetMaxHits.getValue()) {
                    resetHitCount = resetMaxHits.getValue();
                }
            }
        }

        if (sprintMode.getValue() == SprintMode.Packet) {
            lastPacketAttackTime = System.currentTimeMillis();
        }
    }

    // ========== LEGIT SPRINT LOGIC ==========

    private void handleLegitSprint() {
        if (auraAITarget == null) return;

        long currentTime = System.currentTimeMillis();
        double distance = mc.player.distanceTo(auraAITarget);
        boolean shouldSprint = distance <= legitSprintRange.getValue();

        // ADVANCED ANTICHEAT BYPASS - проверяем все античиты
        // Проверяем античиты ПЕРЕД всеми остальными проверками
        if (!canSprintWithAdvancedBypass()) {
            shouldSprint = false; // НЕ спринтуем если античиты заблокируют
        }

        // Дополнительные проверки для стабильности (только если Grim AC разрешает)
        if (shouldSprint && mc.player.isSneaking()) {
            shouldSprint = false; // Не спринтуем если крадемся
        }

        if (shouldSprint && mc.player.isUsingItem()) {
            shouldSprint = false; // Не спринтуем если используем предмет
        }

        if (shouldSprint && mc.player.isOnGround() && mc.player.getVelocity().y > 0.1) {
            shouldSprint = false; // Не спринтуем при прыжке
        }

        // Проверяем движение игрока
        boolean isMoving = isPlayerMoving();
        if (isMoving) {
            lastLegitMovementTime = currentTime;
        }

        // Пауза при движении
        if (legitPauseOnMovement.getValue() && isMoving &&
                currentTime - lastLegitMovementTime < 300) {
            if (legitSprintActive) {
                pauseLegitSprint();
            }
            return;
        }

        // Проверяем кулдаун
        if (legitSprintCooldown && currentTime - lastLegitSprintCooldown > 1000) {
            legitSprintCooldown = false;
        }

        if (shouldSprint && !legitSprintActive && !legitSprintCooldown) {
            // Проверяем шанс активации спринта
            if (Math.random() < legitSprintChance.getValue()) {
                startLegitSprint();
            }
        } else if (!shouldSprint && legitSprintActive) {
            // Плавно отключаем спринт
            stopLegitSprint();
        }

        // Обновляем прогресс спринта
        if (legitSprintActive) {
            updateLegitSprintProgress();
        }
    }

    private void handleSpookyTimeSprint() {
        if (auraAITarget == null) return;

        long currentTime = System.currentTimeMillis();
        double distance = mc.player.distanceTo(auraAITarget);

        // SpookyTime режим использует улучшенную логику спринта с обходами всех античитов
        boolean shouldSprint = distance <= 3.0f; // Фиксированный диапазон для SpookyTime

        // ADVANCED ANTICHEAT BYPASS - проверяем все античиты
        if (shouldSprint && !canSprintWithAdvancedBypass()) {
            shouldSprint = false; // Не спринтуем если античиты заблокируют
        }

        // Дополнительные проверки для SpookyTime режима
        if (shouldSprint && mc.player.getHungerManager().getFoodLevel() < 3.0f) {
            shouldSprint = false; // Слишком голоден
        }

        // Дополнительные проверки для стабильности
        if (shouldSprint && mc.player.isSneaking()) {
            shouldSprint = false; // Не спринтуем при крадущемся движении
        }
        
        if (shouldSprint && mc.player.isUsingItem()) {
            shouldSprint = false; // Не спринтуем при использовании предметов
        }

        // SPOOKYTIME SPRINT RESET BYPASS - сброс спринта перед атакой
        if (shouldSprint && !spookySprintResetActive) {
            // Проверяем, нужно ли сбросить спринт перед атакой
            if (mc.player.isSprinting() && distance <= 2.5f) {
                // Сбрасываем спринт перед атакой для обхода SpookyTime
                mc.player.setSprinting(false);
                spookySprintResetActive = true;
                spookySprintResetStartTime = currentTime;
            } else if (!mc.player.isSprinting()) {
                // Включаем спринт если не активен
                mc.player.setSprinting(true);
            }
        }

        // Восстанавливаем спринт после задержки
        if (spookySprintResetActive && currentTime - spookySprintResetStartTime > 50L) {
            spookySprintResetActive = false;
            if (shouldSprint) {
                mc.player.setSprinting(true);
            }
        }

        // Обычное управление спринтом
        if (!spookySprintResetActive) {
            if (shouldSprint && !mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            } else if (!shouldSprint && mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        }
    }

    // ========== PACKET SPRINT LOGIC ==========
    private void handlePacketSprint() {
        if (target == null) return;
        
        long currentTime = System.currentTimeMillis();
        float distance = mc.player.distanceTo(target);
        
        // Базовые проверки для Packet режима
        boolean shouldSprint = distance <= packetSprintRange.getValue();
        
        // Улучшенные обходы античитов для Packet режима
        if (shouldSprint && !canSprintWithAdvancedBypass()) {
                shouldSprint = false;
        }
        
        // Агрессивный режим - более частые пакеты
        if (packetAggressiveMode.getValue() && shouldSprint) {
            if (currentTime - lastPacketAttackTime > packetSprintDelay.getValue() / 2) {
                sendSprintPacket(true);
                lastPacketAttackTime = currentTime;
            }
        }
        
        // Умный тайминг - адаптивная частота пакетов
        if (packetSmartTiming.getValue() && shouldSprint) {
            int adaptiveDelay = calculateAdaptiveDelay(distance);
            if (currentTime - lastPacketAttackTime > adaptiveDelay) {
                sendSprintPacket(true);
                lastPacketAttackTime = currentTime;
            }
        } else if (shouldSprint) {
            // Обычный режим
            if (currentTime - lastPacketAttackTime > packetSprintDelay.getValue()) {
                sendSprintPacket(true);
                lastPacketAttackTime = currentTime;
            }
        }
        
        // Управление состоянием спринта
        if (shouldSprint && !packetSprintActive) {
            startPacketSprint();
        } else if (!shouldSprint && packetSprintActive) {
            stopPacketSprint();
        }
        
        // Обновление прогресса
        if (packetSprintActive) {
            updatePacketSprintProgress();
        }
    }
    
    private void startPacketSprint() {
        long currentTime = System.currentTimeMillis();
        
        // Финальная проверка обходов
        if (!canSprintWithAdvancedBypass()) {
            return;
        }
        
        // Используем пакеты или обычный спринт
        if (usePacketOnly.getValue()) {
            enableSprintPacket();
            } else {
            mc.player.setSprinting(true);
            mc.options.sprintKey.setPressed(true);
        }
        
        packetSprintActive = true;
        lastPacketAttackTime = currentTime;
        isSprinting = true;
        
        // Обновляем Grim AC переменные
        grimLastSprintStart = currentTime;
        grimSprintStartCount++;
    }
    
    private void stopPacketSprint() {
        if (!packetSprintActive) return;
        
        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - lastPacketAttackTime;
        
        // Минимальная длительность спринта
        if (sprintDuration < 100) return;
        
        // Отключаем спринт
        if (usePacketOnly.getValue()) {
            disableSprintPacket();
            } else {
            mc.player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
        }
        
        packetSprintActive = false;
        isSprinting = false;
    }
    
    private void updatePacketSprintProgress() {
        if (!packetSprintActive) return;
        
        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - lastPacketAttackTime;
        
        // Автоматическое отключение по времени
        if (sprintDuration >= packetSprintDuration.getValue()) {
            stopPacketSprint();
        }
    }
    
    private int calculateAdaptiveDelay(float distance) {
        int baseDelay = packetSprintDelay.getValue();
        
        // Адаптивная задержка в зависимости от расстояния
        if (distance < 2.0f) {
            return (int) (baseDelay * 0.7f); // Близко - быстрее
        } else if (distance < 3.0f) {
            return (int) (baseDelay * 0.9f); // Средне - нормально
            } else {
            return (int) (baseDelay * 1.2f); // Далеко - медленнее
        }
    }
    
    private void sendSprintPacket(boolean sprint) {
        if (sprint) {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        } else {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void startLegitSprint() {
        long currentTime = System.currentTimeMillis();

        // ADVANCED ANTICHEAT BYPASS - финальная проверка перед включением спринта
        if (!canSprintWithAdvancedBypass()) {
            return; // Не включаем спринт если античиты заблокируют
        }

        // Рандомизация задержки
        int delay = legitSprintDelay.getValue();
        if (legitRandomizeTiming.getValue()) {
            delay = (int) (delay * (0.7 + Math.random() * 0.6)); // ±30% рандомизация
        }

        if (currentTime - lastLegitSprintTime < delay) return;

        // Используем естественный спринт если включено
        if (legitUseNaturalSprint.getValue()) {
            // Имитируем нажатие клавиши спринта
            mc.options.sprintKey.setPressed(true);
            mc.player.setSprinting(true);
        } else {
            // Используем пакеты для более точного контроля
            enableSprintPacket();
        }

        legitSprintActive = true;
        legitSprintStartTime = currentTime;
        lastLegitSprintTime = currentTime;
        legitSprintProgress = 0f;
        legitSprintPaused = false;

        // Отслеживаем начало спринта для обходов Grim AC
        grimLastSprintStart = currentTime;
        grimSprintStartCount++;

        // Устанавливаем кулдаун
        legitSprintCooldown = true;
        lastLegitSprintCooldown = currentTime;
    }

    private void stopLegitSprint() {
        if (!legitSprintActive) return;

        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - legitSprintStartTime;

        // Проверяем минимальную длительность спринта
        if (sprintDuration < legitSprintDuration.getValue() * 0.3f) return;

        if (legitSmoothTransition.getValue()) {
            // Плавное отключение спринта
            if (Math.random() < 0.3f) { // 30% шанс на плавное отключение
                // Постепенно уменьшаем спринт
                if (legitUseNaturalSprint.getValue()) {
                    mc.options.sprintKey.setPressed(false);
                    mc.player.setSprinting(false);
                } else {
                    disableSprintPacket();
                }
            }
        } else {
            // Мгновенное отключение
            if (legitUseNaturalSprint.getValue()) {
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);
            } else {
                disableSprintPacket();
            }
        }

        legitSprintActive = false;
        legitSprintProgress = 0f;
        legitSprintPaused = false;
    }

    private void pauseLegitSprint() {
        if (!legitSprintActive || legitSprintPaused) return;

        // Временно приостанавливаем спринт
        if (legitUseNaturalSprint.getValue()) {
            mc.options.sprintKey.setPressed(false);
        } else {
            disableSprintPacket();
        }

        legitSprintPaused = true;
    }

    private void resumeLegitSprint() {
        if (!legitSprintActive || !legitSprintPaused) return;

        // GRIM AC BYPASS - проверяем условия перед возобновлением спринта
        if (!canSprintWithGrimBypass()) {
            return; // Не возобновляем спринт если Grim AC заблокирует
        }

        // Возобновляем спринт
        if (legitUseNaturalSprint.getValue()) {
            mc.options.sprintKey.setPressed(true);
        } else {
            enableSprintPacket();
        }

        legitSprintPaused = false;
    }

    private void updateLegitSprintProgress() {
        if (!legitSprintActive) return;

        long currentTime = System.currentTimeMillis();
        long sprintDuration = currentTime - legitSprintStartTime;

        // Обновляем прогресс спринта
        legitSprintProgress = Math.min(1.0f, sprintDuration / legitSprintDuration.getValue());

        // Автоматически останавливаем спринт по истечении времени
        if (sprintDuration >= legitSprintDuration.getValue()) {
            stopLegitSprint();
        }

        // Возобновляем спринт если он был приостановлен и прошло достаточно времени
        if (legitSprintPaused && currentTime - lastLegitMovementTime > 500) {
            resumeLegitSprint();
        }
    }


    // ========== FALLING AIM LOGIC ==========

    private void updateFallingAim() {
        if (auraAITarget == null) return;

        long currentTime = System.currentTimeMillis();

        // Проверяем дистанцию для наводки
        double distance = mc.player.distanceTo(auraAITarget);
        if (distance > fallingAimRange.getValue()) {
            if (fallingAimActive) {
                stopFallingAim();
            }
            return;
        }

        // Инициализируем наводку если еще не активна
        if (!fallingAimActive) {
            startFallingAim();
        }

        // Вычисляем позицию цели с предсказанием
        Vec3d targetPos = calculatePredictedTargetPosition();
        if (targetPos == null) return;

        // Вычисляем углы наведения
        double[] rotations = calculateFallingLookAngles(targetPos);
        if (rotations == null) return;

        fallingAimTargetYaw = (float) rotations[0];
        fallingAimTargetPitch = (float) rotations[1];

        // Плавная интерполяция к цели
        float smoothFactor = fallingAimSmoothness.getValue() * 0.1f;
        float speedFactor = fallingAimSpeed.getValue();

        // Адаптивная скорость в зависимости от дистанции
        float distanceFactor = Math.min(1.0f, (float) distance / 3.0f);
        smoothFactor *= distanceFactor * speedFactor;

        // Интерполяция
        fallingAimCurrentYaw += (fallingAimTargetYaw - fallingAimCurrentYaw) * smoothFactor;
        fallingAimCurrentPitch += (fallingAimTargetPitch - fallingAimCurrentPitch) * smoothFactor;

        // Ограничиваем pitch
        fallingAimCurrentPitch = MathHelper.clamp(fallingAimCurrentPitch, -90f, 90f);

        // Обновляем rotationYaw/rotationPitch вместо прямого изменения mc.player
        // Это предотвращает конфликты с onSync()
        rotationYaw = fallingAimCurrentYaw;
        rotationPitch = fallingAimCurrentPitch;
    }

    private void startFallingAim() {
        fallingAimActive = true;
        fallingAimStartTime = System.currentTimeMillis();
        fallingAimCurrentYaw = mc.player.getYaw();
        fallingAimCurrentPitch = mc.player.getPitch();

        if (auraAITarget != null) {
            fallingAimTargetPos = auraAITarget.getPos();
            fallingAimTargetVelocity = auraAITarget.getVelocity();
        }
    }

    private void stopFallingAim() {
        fallingAimActive = false;
        fallingAimTargetPos = Vec3d.ZERO;
        fallingAimTargetVelocity = Vec3d.ZERO;
    }

    private Vec3d calculatePredictedTargetPosition() {
        if (auraAITarget == null) return null;

        Vec3d currentPos = auraAITarget.getPos();
        Vec3d velocity = auraAITarget.getVelocity();

        if (!fallingAimPrediction.getValue()) {
            return currentPos;
        }

        // Предсказываем позицию через заданное время
        float predictionTime = fallingAimPredictionTime.getValue();
        return currentPos.add(velocity.multiply(predictionTime));
    }

    private double[] calculateFallingLookAngles(Vec3d targetPos) {
        if (targetPos == null) return null;

        Vec3d playerPos = mc.player.getEyePos();
        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        return new double[]{yaw, pitch};
    }

    // Рендеринг FOV сектора для визуализации поля зрения
    private void renderFOVCircle(MatrixStack stack) {
        if (mc.player == null) return;

        // Получаем радиус атаки
        float attackRadius = getRange();

        // Получаем позицию игрока
        Vec3d playerPos = mc.player.getPos();
        double x = playerPos.x;
        double y = playerPos.y + 0.1; // Немного выше земли для лучшей видимости
        double z = playerPos.z;

        // Настройки рендеринга
        int color = fovColor.getValue();
        float lineWidth = fovLineWidth.getValue();
        boolean fill = fovFill.getValue();
        int fillColor = fovFillColor.getValue();

        // Получаем FOV угол
        float fovAngle = fov.getValue();

        if (fovAngle == 180f) {
            // При 180° рендерим полный круг
            drawHorizontalCircle(stack, x, y, z, attackRadius, color, lineWidth, fill, fillColor);
            drawVerticalCircle(stack, x, y, z, attackRadius, color, lineWidth);
        } else {
            // Для других углов рендерим сектор
            drawFOVSector(stack, x, y, z, attackRadius, fovAngle, color, lineWidth, fill, fillColor);
        }
    }

    // Рисование горизонтального круга на земле
    private void drawHorizontalCircle(MatrixStack stack, double x, double y, double z, float radius, int color, float lineWidth, boolean fill, int fillColor) {
        // Количество сегментов для круга
        int segments = 64;

        // Настройки рендеринга
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        // Извлекаем цветовые компоненты
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        // Устанавливаем цвет
        RenderSystem.setShaderColor(red, green, blue, alpha);

        // Перемещаемся к позиции игрока
        stack.push();
        stack.translate(x - mc.getEntityRenderDispatcher().camera.getPos().x,
                y - mc.getEntityRenderDispatcher().camera.getPos().y,
                z - mc.getEntityRenderDispatcher().camera.getPos().z);

        // Рисуем круг используя Render3DEngine
        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            double x1 = Math.cos(angle1) * radius;
            double z1 = Math.sin(angle1) * radius;
            double x2 = Math.cos(angle2) * radius;
            double z2 = Math.sin(angle2) * radius;

            // Рисуем линию между двумя точками
            Vec3d pos1 = new Vec3d(x1, 0, z1);
            Vec3d pos2 = new Vec3d(x2, 0, z2);
            Render3DEngine.drawLine(pos1, pos2, new Color(color));
        }
        stack.pop();

        // Восстанавливаем настройки рендеринга
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // Рисование вертикального круга для лучшей видимости радиуса
    private void drawVerticalCircle(MatrixStack stack, double x, double y, double z, float radius, int color, float lineWidth) {
        // Количество сегментов для круга
        int segments = 32;

        // Настройки рендеринга
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        // Извлекаем цветовые компоненты
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        // Устанавливаем цвет (немного прозрачнее)
        RenderSystem.setShaderColor(red, green, blue, alpha * 0.7f);

        // Перемещаемся к позиции игрока
        stack.push();
        stack.translate(x - mc.getEntityRenderDispatcher().camera.getPos().x,
                y - mc.getEntityRenderDispatcher().camera.getPos().y,
                z - mc.getEntityRenderDispatcher().camera.getPos().z);

        // Рисуем вертикальный круг используя Render3DEngine
        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            double x1 = Math.cos(angle1) * radius;
            double y1 = Math.sin(angle1) * radius * 0.3; // Делаем эллипс
            double x2 = Math.cos(angle2) * radius;
            double y2 = Math.sin(angle2) * radius * 0.3;

            // Рисуем линию между двумя точками
            Vec3d pos1 = new Vec3d(x1, y1, 0);
            Vec3d pos2 = new Vec3d(x2, y2, 0);
            Render3DEngine.drawLine(pos1, pos2, new Color(color));
        }
        stack.pop();

        // Восстанавливаем настройки рендеринга
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // Рисование FOV сектора
    private void drawFOVSector(MatrixStack stack, double x, double y, double z, float radius, float fovAngle, int color, float lineWidth, boolean fill, int fillColor) {
        // Количество сегментов для сектора
        int segments = (int) (fovAngle / 2); // Один сегмент на каждые 2 градуса
        if (segments < 8) segments = 8; // Минимум 8 сегментов

        // Получаем направление взгляда игрока
        float playerYaw = mc.player.getYaw();

        // Настройки рендеринга
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        // Извлекаем цветовые компоненты
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        // Устанавливаем цвет
        RenderSystem.setShaderColor(red, green, blue, alpha);

        // Перемещаемся к позиции игрока
        stack.push();
        stack.translate(x - mc.getEntityRenderDispatcher().camera.getPos().x,
                y - mc.getEntityRenderDispatcher().camera.getPos().y,
                z - mc.getEntityRenderDispatcher().camera.getPos().z);

        // Рисуем FOV сектор
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(playerYaw - fovAngle / 2 + (fovAngle * i / segments));
            double angle2 = Math.toRadians(playerYaw - fovAngle / 2 + (fovAngle * (i + 1) / segments));

            double x1 = Math.sin(angle1) * radius;
            double z1 = Math.cos(angle1) * radius;
            double x2 = Math.sin(angle2) * radius;
            double z2 = Math.cos(angle2) * radius;

            // Рисуем линию между двумя точками
            Vec3d pos1 = new Vec3d(x1, 0, z1);
            Vec3d pos2 = new Vec3d(x2, 0, z2);
            Render3DEngine.drawLine(pos1, pos2, new Color(color));
        }

        // Рисуем боковые линии сектора
        double leftAngle = Math.toRadians(playerYaw - fovAngle / 2);
        double rightAngle = Math.toRadians(playerYaw + fovAngle / 2);

        Vec3d leftStart = new Vec3d(0, 0, 0);
        Vec3d leftEnd = new Vec3d(Math.sin(leftAngle) * radius, 0, Math.cos(leftAngle) * radius);
        Vec3d rightStart = new Vec3d(0, 0, 0);
        Vec3d rightEnd = new Vec3d(Math.sin(rightAngle) * radius, 0, Math.cos(rightAngle) * radius);

        Render3DEngine.drawLine(leftStart, leftEnd, new Color(color));
        Render3DEngine.drawLine(rightStart, rightEnd, new Color(color));

        stack.pop();

        // Восстанавливаем настройки рендеринга
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    // ========== SPOOKYTIME BYPASS METHODS ==========
    
    private void handleSpookyTimeBypass() {
        if (!spookyTimeBypass.getValue()) return;
        
        // Adaptive Snap Mode
        if (adaptiveSnapMode.getValue()) {
            handleAdaptiveSnap();
        }
    }
    
    private void handleAdaptiveSnap() {
        if (auraAITarget == null) return;
        
        spookyAttackCount++;
        
        if (spookyAttackCount >= attacksBeforeSnap && !isSnappingUp) {
            isSnappingUp = true;
            snapStartTime = System.currentTimeMillis();
            snapStartYaw = mc.player.getYaw();
            snapStartPitch = mc.player.getPitch();
            spookyAttackCount = 0;
            attacksBeforeSnap = snapFrequency.getValue() + (int)(Math.random() * 6);
        }
        
        if (isSnappingUp) {
            long currentTime = System.currentTimeMillis();
            float progress = (currentTime - snapStartTime) / (float) snapDuration.getValue();
            
            if (progress >= 1.0f) {
                isSnappingUp = false;
                // Сбрасываем pitch на -90 для обхода SpookyTime
                mc.player.setPitch(-90.0f);
            } else {
                float targetPitch = -90.0f;
                float currentPitch = snapStartPitch + (targetPitch - snapStartPitch) * progress;
                mc.player.setPitch(MathHelper.clamp(currentPitch, -90.0f, 90.0f));
            }
        }
    }
    
    // Smooth ротация из Danq Rebirth
    private void updateSmoothRotation(float targetYaw, float targetPitch) {
        if (auraAITarget == null) return;
        
        // Нормализация углов
        targetYaw = wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);
        
        // Вычисляем разность углов
        float deltaYaw = wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;
        
        // Smooth скорость из настроек
        float smoothFactor = smoothSpeed.getValue();
        
        // Применяем плавную интерполяцию
        currentYaw += deltaYaw * smoothFactor;
        currentPitch += deltaPitch * smoothFactor;
        
        // Ограничиваем pitch
        currentPitch = MathHelper.clamp(currentPitch, -90f, 90f);
        
        // Применяем ротацию
        rotationYaw = currentYaw;
        rotationPitch = currentPitch;
    }

    // Вспомогательные методы для SpookyTime ротации
    private boolean lookTarget(Entity target) {
        if (mc.player != null && mc.world != null) {
            Vec3d playerDirection = mc.player.getRotationVector();
            Vec3d targetPosVec = target.getPos();
            Vec3d targetDirection = targetPosVec.subtract(mc.player.getEyePos()).normalize();
            double angle = Math.toDegrees(Math.acos(playerDirection.dotProduct(targetDirection)));
            return angle <= 360;
        }
        return false;
    }

    private float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }

    private boolean shouldPlayerFalling() {
        return mc.player.fallDistance > 0.0f && !mc.player.isOnGround();
    }

    // Рендеринг FOV индикатора на экране (временно отключен из-за отсутствия методов)
    /*
    private void renderFOVIndicator(MatrixStack stack) {
        if (mc.player == null) return;

        // Получаем размеры экрана
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // Получаем FOV угол
        float fovAngle = fov.getValue();

        // Настройки рендеринга
        int color = fovColor.getValue();
        float lineWidth = fovLineWidth.getValue();

        // Центр экрана
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Размер индикатора (радиус)
        int radius = 50;

        if (fovAngle == 180f) {
            // При 180° рисуем полный круг
            Render2DEngine.drawCircle(stack, centerX, centerY, radius, new Color(color), lineWidth);
        } else {
            // Для других углов рисуем сектор
            float startAngle = -fovAngle / 2;
            float endAngle = fovAngle / 2;

            // Рисуем дугу сектора
            Render2DEngine.drawArc(stack, centerX, centerY, radius, startAngle, endAngle, new Color(color), lineWidth);

            // Рисуем боковые линии сектора
            float leftX = centerX + (float) (Math.sin(Math.toRadians(startAngle)) * radius);
            float leftY = centerY - (float) (Math.cos(Math.toRadians(startAngle)) * radius);
            float rightX = centerX + (float) (Math.sin(Math.toRadians(endAngle)) * radius);
            float rightY = centerY - (float) (Math.cos(Math.toRadians(endAngle)) * radius);

            Render2DEngine.drawLine(stack, centerX, centerY, leftX, leftY, new Color(color), lineWidth);
            Render2DEngine.drawLine(stack, centerX, centerY, rightX, rightY, new Color(color), lineWidth);
        }

        // Рисуем центральную точку
        Render2DEngine.drawCircle(stack, centerX, centerY, 2, new Color(color), 1.0f);
    }
    */

    // BackTrack методы (как в Aura.java)
    private void resolvePlayers() {
        if (resolver.getValue() != Aura.Resolver.Off) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player instanceof OtherClientPlayerEntity) {
                    ((IOtherClientPlayerEntity) player).resolve(resolver.getValue());
                }
            }
        }
    }

    private void restorePlayers() {
        if (resolver.getValue() != Aura.Resolver.Off) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player instanceof OtherClientPlayerEntity) {
                    ((IOtherClientPlayerEntity) player).releaseResolver();
                }
            }
        }
    }

    // Grim BackTrack обходы - дополнительные проверки
    private boolean shouldApplyGrimBackTrack() {
        if (resolver.getValue() != Aura.Resolver.BackTrack) return false;
        if (!isGrimServer()) return false;
        
        // Grim специфичные обходы
        if (Math.random() < 0.15f) {
            return false; // Случайные отказы для Grim
        }
        
        return true;
    }

    // Проверка Grim сервера
    private boolean isGrimServer() {
        if (mc.getCurrentServerEntry() == null) return false;
        String serverAddress = mc.getCurrentServerEntry().address.toLowerCase();
        return serverAddress.contains("grim") ||
               serverAddress.contains("grimac") ||
               serverAddress.contains("grimanticheat") ||
               serverAddress.contains("grimanticheat.com");
    }
    
    private boolean isSpookyTimeServer() {
        // Проверяем, является ли сервер SpookyTime сервером
        if (mc.getCurrentServerEntry() != null) {
            String serverName = mc.getCurrentServerEntry().name.toLowerCase();
            String serverIP = mc.getCurrentServerEntry().address.toLowerCase();
            
            // Известные серверы со SpookyTime AC
            return serverName.contains("spooky") || 
                   serverName.contains("spookytime") ||
                   serverIP.contains("spooky") ||
                   serverName.contains("анархия") ||
                   serverName.contains("anarchy") ||
                   serverName.contains("grief") ||
                   // Дополнительные паттерны для SpookyTime серверов
                   serverName.contains("mine") && serverName.contains("craft") ||
                   serverIP.contains("mc-") ||
                   serverIP.contains("play.");
        }
        return true; // По умолчанию считаем что это SpookyTime сервер для безопасности
    }
    

    private void renderBackTrackResolver(MatrixStack stack, PlayerEntity player) {
        if (!(player instanceof IOtherClientPlayerEntity otherPlayer)) return;
        
        // Простая визуализация BackTrack - показываем только текущую позицию
        // TODO: Добавить полную визуализацию истории позиций когда будет готов API
        
        // Рендерим текущую позицию
        Box box = player.getBoundingBox();
        int color = new Color(255, 0, 0, 255).getRGB(); // Красный цвет для текущей позиции
        
        // Используем простой рендеринг бокса
        Render3DEngine.renderGhosts(1, 1, 1.0f, 1.0f, player);
    }
}