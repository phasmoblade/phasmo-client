package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Colors;
import org.lwjgl.opengl.GL40C;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.hud.HudEditorGui;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientOverlayOld extends HudElement {
    
    public ClientOverlayOld() {
        super("ClientOverlayOld", 200, 100);
    }

    private final Setting<Boolean> showLogo = new Setting<>("ShowLogo", true);
    private final Setting<Boolean> showInfo = new Setting<>("ShowInfo", true);
    private final Setting<Boolean> showArraylist = new Setting<>("ShowArraylist", true);
    private final Setting<Boolean> showKeyBinds = new Setting<>("ShowKeyBinds", true);
    private final Setting<Boolean> showPotions = new Setting<>("ShowPotions", true);
    private final Setting<Boolean> showTargetHud = new Setting<>("ShowTargetHud", true);
    
    private final Setting<Boolean> showUser = new Setting<>("ShowUser", true);
    private final Setting<Boolean> showSession = new Setting<>("ShowSession", true);
    private final Setting<Boolean> showWorldInfo = new Setting<>("ShowWorldInfo", true);
    
    // TargetHud настройки
    private final Setting<Boolean> showAbsorption = new Setting<>("ShowAbsorption", true);
    private final Setting<Boolean> showHealthPercent = new Setting<>("ShowHealthPercent", true);
    private final Setting<Float> scale = new Setting<>("Scale", 1.0f, 0.5f, 2.0f);
    
    // Эффекты
    private final Setting<Boolean> rainbowEffect = new Setting<>("RainbowEffect", true);
    
    private final Setting<Boolean> showFPS = new Setting<>("ShowFPS", true);
    private final Setting<Boolean> showPing = new Setting<>("ShowPing", true);
    private final Setting<Boolean> showTime = new Setting<>("ShowTime", true);
    private final Setting<Boolean> showTPS = new Setting<>("ShowTPS", true);
    
    private final Setting<Boolean> roundBPS = new Setting<>("RoundBPS", false);
    private final Setting<Boolean> shadow = new Setting<>("Shadow", true);
    
    private final Setting<Float> spacing = new Setting<>("Spacing", 8f, 0f, 20f);
    private final Setting<Float> padding = new Setting<>("Padding", 5f, 0f, 15f);
    private final Setting<ColorSetting> color1 = new Setting<>("Color", new ColorSetting(-16492289));
    private final Setting<ColorSetting> color2 = new Setting<>("Color2", new ColorSetting(-8365735));
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f);
    
    // TargetHud переменные
    private LivingEntity targetEntity = null;
    private float health, absorption;
    private float animation = 0f;

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        
        float x = getPosX();
        float y = getPosY();
        
        // Рисуем логотип
        if (showLogo.getValue()) {
            renderLogo(context, x, y);
        }
        
        // Рисуем информацию
        if (showInfo.getValue()) {
            renderInfo(context, x, y + 30);
        }
        
        // Рисуем сессию
        if (showSession.getValue()) {
            renderSessionInfo(context, x, y + 80);
        }
        
        // Рисуем Arraylist (упрощенная версия)
        if (showArraylist.getValue()) {
            renderArraylist(context, x + 200, y);
        }
        
        // Рисуем KeyBinds
        if (showKeyBinds.getValue()) {
            renderKeyBinds(context, x, y + 150);
        }
        
        // Рисуем Potions
        if (showPotions.getValue()) {
            renderPotions(context, x + 200, y + 150);
        }
        
        // Рисуем TargetHud
        if (showTargetHud.getValue()) {
            renderTargetHud(context, x + 400, y);
        }
    }
    
    private void renderLogo(DrawContext context, float x, float y) {
        String logo = "shitpaste oblivion | local | 1.2";
        float width = FontRenderers.sf_bold.getStringWidth(logo) + padding.getValue() * 2;
        float height = 20f;
        
        // Рисуем градиентный фон (горизонтальный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color2.getValue().getColorObject(),
                color2.getValue().getColorObject(), color1.getValue().getColorObject(),
                x, y, width, height, cornerRadius.getValue());
        
        // Рисуем текст с радужным эффектом
        FontRenderers.sf_bold.drawString(context.getMatrices(), logo, 
                x + padding.getValue(), y + 6, getRainbowColor(0));
    }
    
    private void renderInfo(DrawContext context, float x, float y) {
        StringBuilder info = new StringBuilder();
        float totalWidth = 0f;
        
        // Добавляем FPS
        if (showFPS.getValue()) {
            String fps = "FPS: " + FrameRateCounter.INSTANCE.getFps();
            info.append(fps);
            totalWidth += FontRenderers.sf_bold_mini.getStringWidth(fps);
        }
        
        // Добавляем пинг
        if (showPing.getValue()) {
            if (info.length() > 0) {
                info.append(" | ");
                totalWidth += FontRenderers.sf_bold_mini.getStringWidth(" | ");
            }
            String ping = "Ping: " + Managers.SERVER.getPing() + "ms";
            info.append(ping);
            totalWidth += FontRenderers.sf_bold_mini.getStringWidth(ping);
        }
        
        // Добавляем время
        if (showTime.getValue()) {
            if (info.length() > 0) {
                info.append(" | ");
                totalWidth += FontRenderers.sf_bold_mini.getStringWidth(" | ");
            }
            String time = "Time: " + new SimpleDateFormat("HH:mm:ss").format(new Date());
            info.append(time);
            totalWidth += FontRenderers.sf_bold_mini.getStringWidth(time);
        }
        
        // Добавляем TPS
        if (showTPS.getValue()) {
            if (info.length() > 0) {
                info.append(" | ");
                totalWidth += FontRenderers.sf_bold_mini.getStringWidth(" | ");
            }
            String tps = "TPS: " + (roundBPS.getValue() ? 
                    String.format("%.1f", 20.0) : 
                    String.format("%.2f", 20.0));
            info.append(tps);
            totalWidth += FontRenderers.sf_bold_mini.getStringWidth(tps);
        }
        
        totalWidth += padding.getValue() * 2;
        float height = 15f;
        
        // Рисуем градиентный фон (вертикальный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color1.getValue().getColorObject(),
                color2.getValue().getColorObject(), color2.getValue().getColorObject(),
                x, y, totalWidth, height, cornerRadius.getValue());
        
        // Рисуем текст белым цветом
        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), info.toString(), 
                x + padding.getValue(), y + 4, Colors.WHITE);
    }
    
    private void renderSessionInfo(DrawContext context, float x, float y) {
        String playerName = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? 
                (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : 
                mc.getSession().getUsername();
        
        String serverInfo = mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address;
        String playTime = getPlayTime();
        
        float width = 180f;
        float height = 60f;
        
        // Рисуем градиентный фон (диагональный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color2.getValue().getColorObject(),
                color2.getValue().getColorObject(), color1.getValue().getColorObject(),
                x, y, width, height, cornerRadius.getValue());
        
        // Заголовок
        FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), 
                "Session Information", x + width / 2f, y + 8, Colors.WHITE);
        
        // Информация
        float yOffset = 20f;
        
        if (showUser.getValue()) {
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                    "Name: " + playerName, x + 8, y + yOffset, Colors.WHITE);
            yOffset += 12f;
        }
        
        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                "Server: " + serverInfo, x + 8, y + yOffset, Colors.WHITE);
        yOffset += 12f;
        
        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                "Play Time: " + playTime, x + 8, y + yOffset, Colors.WHITE);
    }
    
    private void renderArraylist(DrawContext context, float x, float y) {
        // Упрощенная версия arraylist - показываем только активные модули
        float maxWidth = 0f;
        int moduleCount = 0;
        
        for (thunder.hack.features.modules.Module module : Managers.MODULE.modules) {
            if (module.isOn() && module != ModuleManager.clickGui && module != ModuleManager.thunderHackGui) {
                float width = FontRenderers.sf_bold_mini.getStringWidth(module.getName());
                if (width > maxWidth) maxWidth = width;
                moduleCount++;
            }
        }
        
        if (moduleCount == 0) return;
        
        float height = 14 + moduleCount * 9;
        maxWidth += padding.getValue() * 2;
        
        // Рисуем градиентный фон (вертикальный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color1.getValue().getColorObject(),
                color2.getValue().getColorObject(), color2.getValue().getColorObject(),
                x, y, maxWidth, height, cornerRadius.getValue());
        
        // Заголовок
        FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), 
                "Modules", x + maxWidth / 2f, y + 4, Colors.WHITE);
        
        // Модули
        float yOffset = 14f;
        for (thunder.hack.features.modules.Module module : Managers.MODULE.modules) {
            if (module.isOn() && module != ModuleManager.clickGui && module != ModuleManager.thunderHackGui) {
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                        module.getName(), x + padding.getValue(), y + yOffset, 
                        Colors.WHITE);
                yOffset += 9;
            }
        }
    }
    
    private void renderKeyBinds(DrawContext context, float x, float y) {
        // Упрощенная версия keybinds
        float maxWidth = 0f;
        int bindCount = 0;
        
        for (thunder.hack.features.modules.Module module : Managers.MODULE.modules) {
            if (module.isOn() && !module.getBind().getBind().equals("None") && 
                module != ModuleManager.clickGui && module != ModuleManager.thunderHackGui) {
                float width = FontRenderers.sf_bold_mini.getStringWidth(module.getName() + " " + module.getBind().getBind());
                if (width > maxWidth) maxWidth = width;
                bindCount++;
            }
        }
        
        if (bindCount == 0) return;
        
        float height = 14 + bindCount * 9;
        maxWidth += padding.getValue() * 2;
        
        // Рисуем градиентный фон (диагональный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color2.getValue().getColorObject(),
                color2.getValue().getColorObject(), color1.getValue().getColorObject(),
                x, y, maxWidth, height, cornerRadius.getValue());
        
        // Заголовок
        FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), 
                "KeyBinds", x + maxWidth / 2f, y + 4, Colors.WHITE);
        
        // Клавиши
        float yOffset = 14f;
        for (thunder.hack.features.modules.Module module : Managers.MODULE.modules) {
            if (module.isOn() && !module.getBind().getBind().equals("None") && 
                module != ModuleManager.clickGui && module != ModuleManager.thunderHackGui) {
                String text = module.getName() + " " + module.getBind().getBind();
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                        text, x + padding.getValue(), y + yOffset, Colors.WHITE);
                yOffset += 9;
            }
        }
    }
    
    private void renderPotions(DrawContext context, float x, float y) {
        // Упрощенная версия potions
        int potionCount = mc.player.getStatusEffects().size();
        if (potionCount == 0) return;
        
        float maxWidth = 0f;
        for (var effect : mc.player.getStatusEffects()) {
            String text = effect.getEffectType().value().getName().getString() + " " + (effect.getAmplifier() + 1);
            float width = FontRenderers.sf_bold_mini.getStringWidth(text);
            if (width > maxWidth) maxWidth = width;
        }
        
        float height = 14 + potionCount * 9;
        maxWidth += padding.getValue() * 2;
        
        // Рисуем градиентный фон (вертикальный градиент)
        Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                color1.getValue().getColorObject(), color1.getValue().getColorObject(),
                color2.getValue().getColorObject(), color2.getValue().getColorObject(),
                x, y, maxWidth, height, cornerRadius.getValue());
        
        // Заголовок
        FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), 
                "Potions", x + maxWidth / 2f, y + 4, Colors.WHITE);
        
        // Эффекты
        float yOffset = 14f;
        for (var effect : mc.player.getStatusEffects()) {
            String text = effect.getEffectType().value().getName().getString() + " " + (effect.getAmplifier() + 1);
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), 
                    text, x + padding.getValue(), y + yOffset, Colors.WHITE);
            yOffset += 9;
        }
    }
    
    private String getPlayTime() {
        // Упрощенная версия - можно улучшить
        return "01:21"; // Заглушка
    }
    
    private int getRainbowColor(float offset) {
        if (!rainbowEffect.getValue()) return Colors.WHITE;
        
        float hue = (System.currentTimeMillis() / 20f + offset) % 360f;
        return Color.HSBtoRGB(hue / 360f, 0.8f, 1f);
    }
    
    private void renderTargetHud(DrawContext context, float x, float y) {
        getTarget();
        if (targetEntity == null) {
            animation = AnimationUtility.fast(animation, 0f, 9);
            if (animation < 0.01f) return;
        } else {
            animation = AnimationUtility.fast(animation, 1f, 9);
        }

        float width = 90f * scale.getValue();
        float height = 28f * scale.getValue();

        // Анимация появления/исчезновения
        context.getMatrices().push();
        context.getMatrices().translate(x + width / 2, y + height / 2, 0);
        context.getMatrices().scale(animation, animation, 1);
        context.getMatrices().translate(-(x + width / 2), -(y + height / 2), 0);

        // Обновляем здоровье и абсорбцию
        float targetHealth = MathUtility.clamp(targetEntity.getHealth() / targetEntity.getMaxHealth(), 0, 1);
        float targetAbsorption = MathUtility.clamp(targetEntity.getAbsorptionAmount() / targetEntity.getMaxHealth(), 0, 1);

        if (targetEntity.getAbsorptionAmount() <= 0) {
            targetAbsorption = 0;
        }

        health = AnimationUtility.fast(health, targetHealth, 9);
        absorption = AnimationUtility.fast(absorption, targetAbsorption, 9);

        // Рисуем градиентный фон
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawHudBase2(context.getMatrices(), x, y, width, height, cornerRadius.getValue(),
                    HudEditor.blurStrength.getValue(), HudEditor.blurOpacity.getValue(), animation);
        } else {
            // Диагональный градиент для TargetHud
            Render2DEngine.renderRoundedGradientRect(context.getMatrices(),
                    color1.getValue().getColorObject(), color2.getValue().getColorObject(),
                    color2.getValue().getColorObject(), color1.getValue().getColorObject(),
                    x, y, width, height, cornerRadius.getValue());
        }

        // Рисуем голову игрока
        if (targetEntity instanceof PlayerEntity) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayerEntity) targetEntity).getSkinTextures().texture());
            
            context.getMatrices().push();
            context.getMatrices().translate(x + 3.5f + (height - 6) / 2, y + 3.5f + (height - 6) / 2, 0);
            context.getMatrices().scale(1f, 1f, 1f);
            context.getMatrices().translate(-(x + 3.5f + (height - 6) / 2), -(y + 3.5f + (height - 6) / 2), 0);
            
            RenderSystem.enableBlend();
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(GL40C.GL_COLOR_BUFFER_BIT, false);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Render2DEngine.renderRoundedQuadInternal(context.getMatrices().peek().getPositionMatrix(), 
                    animation, animation, animation, animation, 
                    x + 3.5f, y + 3.5f, x + 3.5f + (height - 6), y + 3.5f + (height - 6), 7, 10);
            RenderSystem.blendFunc(GL40C.GL_DST_ALPHA, GL40C.GL_ONE_MINUS_DST_ALPHA);
            RenderSystem.setShaderColor(animation, animation, animation, animation);
            Render2DEngine.renderTexture(context.getMatrices(), x + 3.5f, y + 3.5f, height - 6, height - 6, 8, 8, 8, 8, 64, 64);
            Render2DEngine.renderTexture(context.getMatrices(), x + 3.5f, y + 3.5f, height - 6, height - 6, 40, 8, 8, 8, 64, 64);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            context.getMatrices().pop();
        } else {
            Render2DEngine.drawRound(context.getMatrices(), x + 3, y + 3, height - 6, height - 6, cornerRadius.getValue(),
                    new Color(22, 22, 22, 120));
            FontRenderers.sf_bold.drawString(context.getMatrices(), "?", x + 10.5F, y + 10.5F, Colors.WHITE);
        }

        // Получаем имя игрока
        String name = targetEntity instanceof PlayerEntity ? 
                ((PlayerEntity) targetEntity).getGameProfile().getName() : 
                targetEntity.getName().getString();

        // Обрезаем имя если слишком длинное
        float maxNameWidth = 60 * scale.getValue();
        float nameWidth = FontRenderers.sf_bold.getStringWidth(name);
        if (nameWidth > maxNameWidth) {
            StringBuilder shortened = new StringBuilder();
            for (char c : name.toCharArray()) {
                String temp = shortened.toString() + c + "...";
                if (FontRenderers.sf_bold.getStringWidth(temp) > maxNameWidth) {
                    break;
                }
                shortened.append(c);
            }
            name = shortened.toString() + "...";
        }

        // Рисуем имя игрока
        String displayName = ModuleManager.media.isEnabled() ? "Protected" : 
                (ModuleManager.nameProtect.isEnabled() && targetEntity == mc.player ? 
                        NameProtect.getCustomName() : name);
        
        FontRenderers.sf_bold.drawString(context.getMatrices(), displayName, 
                x + height, y + 7, Colors.WHITE);

        // Рисуем полоску здоровья
        float min = 40 * scale.getValue();
        Render2DEngine.drawRound(context.getMatrices(), x + height - 1, y + height - 9 - 1, min + 2, 6, 1.2f,
                new Color(22, 22, 22, 120));

        // Рисуем здоровье
        Render2DEngine.drawRound(context.getMatrices(), x + height, y + height - 9, 
                Math.min(min, min * health), 4, 1.2f,
                HudEditor.getColor(1));

        // Рисуем абсорбцию
        if (showAbsorption.getValue() && absorption > 0.01) {
            Render2DEngine.drawRound(context.getMatrices(), x + height, y + height - 9, 
                    Math.min(min, min * absorption), 4, 1.2f,
                    new Color(255, 215, 0));
        }

        // Рисуем процент здоровья
        if (showHealthPercent.getValue()) {
            String healthText = (int) Math.round(health * 100) + "%";
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), healthText,
                    x + height + Math.min(min, min * health) + 2, y + height - 9, -1);
        }

        context.getMatrices().pop();
    }
    
    private void getTarget() {
        if (ModuleManager.autoCrystal.target != null) {
            targetEntity = ModuleManager.autoCrystal.target;
            if (ModuleManager.autoCrystal.target.isDead()) {
                ModuleManager.autoCrystal.target = null;
                targetEntity = null;
            }
        } else if (ModuleManager.aura.target != null) {
            if (ModuleManager.aura.target instanceof LivingEntity) {
                targetEntity = (LivingEntity) ModuleManager.aura.target;
            } else {
                targetEntity = null;
            }
        } else if (ModuleManager.auraAI.auraAITarget != null) {
            if (ModuleManager.auraAI.auraAITarget instanceof LivingEntity) {
                targetEntity = (LivingEntity) ModuleManager.auraAI.auraAITarget;
            } else {
                targetEntity = null;
            }
        } else if (ModuleManager.autoAnchor.target != null) {
            targetEntity = ModuleManager.autoAnchor.target;
            if (ModuleManager.autoAnchor.target.isDead()) {
                ModuleManager.autoAnchor.target = null;
                targetEntity = null;
            }
        } else if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof HudEditorGui) {
            targetEntity = mc.player;
        } else {
            if (animation < 0.02f)
                targetEntity = null;
        }
    }
}
