package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.FrameRateCounter;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WaterMarkNew extends HudElement {
    
    public enum FontStyle {
        SF_BOLD,
        SF_MEDIUM,
        MONSTERRAT,
        PROFONT,
        COMFORTAA,
        ICONS,
        ZONA_ULTRA,
        SF_MEDIUM_MINI,
        SF_BOLD_MINI,
        SF_BOLD_MICRO
    }
    
    public WaterMarkNew() {
        super("WaterMarkNew", 100, 35);
    }

    private final Setting<Boolean> showPlayer = new Setting<>("ShowPlayer", true);
    private final Setting<Boolean> showServer = new Setting<>("ShowServer", true);
    private final Setting<Boolean> showPing = new Setting<>("ShowPing", true);
    private final Setting<Boolean> showFPS = new Setting<>("ShowFPS", true);
    private final Setting<Boolean> showTime = new Setting<>("ShowTime", true);
    private final Setting<Boolean> showDate = new Setting<>("ShowDate", false);
    private final Setting<Boolean> showVersion = new Setting<>("ShowVersion", true);
    private final Setting<Boolean> showProxy = new Setting<>("ShowProxy", true);
    
    private final Setting<Boolean> showIcons = new Setting<>("ShowIcons", true);
    private final Setting<Boolean> showSeparators = new Setting<>("ShowSeparators", true);
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100);
    private final Setting<Float> spacing = new Setting<>("Spacing", 8f, 0f, 20f);
    private final Setting<Float> padding = new Setting<>("Padding", 5f, 0f, 15f);
    
    private final Setting<Boolean> useGradient = new Setting<>("UseGradient", true);
    private final Setting<Boolean> useClientColors = new Setting<>("UseClientColors", true);
    private final Setting<FontStyle> fontStyle = new Setting<>("FontStyle", FontStyle.SF_BOLD);
    
    // Настройки размытия
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> showBackground.getValue());

    // Вспомогательные методы для работы с шрифтами
    private float getStringWidth(String text) {
        return switch (fontStyle.getValue()) {
            case SF_BOLD -> FontRenderers.sf_bold.getStringWidth(text);
            case SF_MEDIUM -> FontRenderers.sf_medium.getStringWidth(text);
            case MONSTERRAT -> FontRenderers.monsterrat.getStringWidth(text);
            case PROFONT -> FontRenderers.profont.getStringWidth(text);
            case COMFORTAA -> FontRenderers.settings.getStringWidth(text);
            case ICONS -> FontRenderers.icons.getStringWidth(text);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.getStringWidth(text);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.getStringWidth(text);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.getStringWidth(text);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.getStringWidth(text);
        };
    }
    
    private float getCenteredY(float baseY) {
        // Высота фона ватермарки
        float backgroundHeight = 15f;
        
        // Получаем высоту шрифта для правильного центрирования
        float fontHeight = switch (fontStyle.getValue()) {
            case SF_BOLD -> 9f; // Примерная высота SF_BOLD
            case SF_MEDIUM -> 9f;
            case MONSTERRAT -> 9f;
            case PROFONT -> 9f;
            case COMFORTAA -> 9f;
            case ICONS -> 9f;
            case ZONA_ULTRA -> 8f; // ZONA_ULTRA имеет другую высоту
            case SF_MEDIUM_MINI -> 7f;
            case SF_BOLD_MINI -> 7f;
            case SF_BOLD_MICRO -> 6f;
        };
        
        // Центрируем текст по вертикали и перемещаем на 3 пикселя выше
        return baseY + (backgroundHeight - fontHeight) / 2f - 3f;
    }
    
    private void drawString(DrawContext context, String text, float x, float y, int color) {
        float centeredY = getCenteredY(y);
        switch (fontStyle.getValue()) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, x, centeredY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, x, centeredY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, x, centeredY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, x, centeredY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, x, centeredY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, x, centeredY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, x, centeredY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, x, centeredY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, x, centeredY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, x, centeredY, color);
        }
    }
    
    private void drawGradientString(DrawContext context, String text, float x, float y, int color) {
        float centeredY = getCenteredY(y);
        switch (fontStyle.getValue()) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case PROFONT -> FontRenderers.profont.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case ICONS -> FontRenderers.icons.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawGradientString(context.getMatrices(), text, x, centeredY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientString(context.getMatrices(), text, x, centeredY, color);
        }
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            renderBlurryStyle(context);
        } else {
            renderNormalStyle(context);
        }
    }

    private void renderBlurryStyle(DrawContext context) {
        StringBuilder info = new StringBuilder();
        float totalWidth = 0f;
        float spacing = this.spacing.getValue();
        
        // Добавляем название клиента
        info.append("PHASMOCLIENT");
        totalWidth += getStringWidth("PHASMOCLIENT");
        
        // Добавляем имя игрока
        if (showPlayer.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String username = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? 
                (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : 
                mc.getSession().getUsername();
            info.append(username);
            totalWidth += getStringWidth(username);
        }
        
        // Добавляем пинг
        if (showPing.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String ping = Managers.SERVER.getPing() + "ms";
            info.append(ping);
            totalWidth += getStringWidth(ping);
        }
        
        // Добавляем FPS
        if (showFPS.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String fps = FrameRateCounter.INSTANCE.getFps() + "fps";
            info.append(fps);
            totalWidth += getStringWidth(fps);
        }
        
        // Добавляем время
        if (showTime.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            info.append(time);
            totalWidth += getStringWidth(time);
        }
        
        // Добавляем дату
        if (showDate.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String date = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
            info.append(date);
            totalWidth += getStringWidth(date);
        }
        
        // Добавляем версию
        if (showVersion.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String version = "v1.3.0";
            info.append(version);
            totalWidth += getStringWidth(version);
        }
        
        // Добавляем сервер
        if (showServer.getValue()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String server = mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address;
            info.append(server);
            totalWidth += getStringWidth(server);
        }
        
        // Добавляем прокси
        if (showProxy.getValue() && Managers.PROXY.isActive()) {
            if (showSeparators.getValue()) {
                info.append(" | ");
                totalWidth += getStringWidth(" | ");
            }
            String proxy = Managers.PROXY.getActiveProxy().getName();
            info.append(proxy);
            totalWidth += getStringWidth(proxy);
        }
        
        // Добавляем отступы
        totalWidth += padding.getValue() * 2;
        
        // Рисуем фон
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            if (enableBlur.getValue()) {
                // Используем красивое размытие с учетом прозрачности
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                // Применяем прозрачность к blurOpacity
                float finalBlurOpacity = blurOpacity.getValue() * alpha;
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), totalWidth, 15f, cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                if (alpha >= 1.0f) {
                    Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), totalWidth, 15f, cornerRadius.getValue(), HudEditor.blurColor.getValue().getColorObject());
                } else {
                    Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                            HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                            HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                            (int)(alpha * 255));
                    Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), totalWidth, 15f, cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
                }
            }
        }
        
        // Рисуем текст
        if (useGradient.getValue()) {
            drawGradientString(context, info.toString(), 
                             getPosX() + padding.getValue(), 
                             getPosY() + 4.5f, 
                             20);
        } else if (useClientColors.getValue()) {
            drawString(context, info.toString(), 
                      getPosX() + padding.getValue(), 
                      getPosY() + 4.5f, 
                      HudEditor.getColor(1).getRGB());
        } else {
            drawString(context, info.toString(), 
                      getPosX() + padding.getValue(), 
                      getPosY() + 4.5f, 
                      HudEditor.textColor.getValue().getColor());
        }
        
        setBounds(getPosX(), getPosY(), totalWidth, 15f);
    }

    private void renderNormalStyle(DrawContext context) {
        StringBuilder info = new StringBuilder();

        info.append("PHASMOCLIENT");
        if (showSeparators.getValue()) info.append(" | ");

        if (showPlayer.getValue()) {
            String username = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? 
                (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : 
                mc.getSession().getUsername();
            info.append(username);
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showPing.getValue()) {
            info.append(Managers.SERVER.getPing()).append("ms");
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showFPS.getValue()) {
            info.append(FrameRateCounter.INSTANCE.getFps()).append("fps");
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showTime.getValue()) {
            info.append(new SimpleDateFormat("HH:mm:ss").format(new Date()));
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showDate.getValue()) {
            info.append(new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showVersion.getValue()) {
            info.append("v1.3.0");
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showServer.getValue()) {
            info.append(mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address);
            if (showSeparators.getValue()) info.append(" | ");
        }

        if (showProxy.getValue() && Managers.PROXY.isActive()) {
            info.append(Managers.PROXY.getActiveProxy().getName());
        }

        // Убираем последний разделитель если есть
        if (showSeparators.getValue() && info.length() > 0) {
            String infoStr = info.toString();
            if (infoStr.endsWith(" | ")) {
                infoStr = infoStr.substring(0, infoStr.length() - 3);
            }
            info = new StringBuilder(infoStr);
        }
        
        float width = getStringWidth(info.toString()) + 5;
        
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            if (enableBlur.getValue()) {
                // Используем красивое размытие с учетом прозрачности
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                // Применяем прозрачность к blurOpacity
                float finalBlurOpacity = blurOpacity.getValue() * alpha;
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), width, 10, cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), width, 10, cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }
        
        // Рисуем текст
        if (useGradient.getValue()) {
            drawGradientString(context, info.toString(), 
                             getPosX() + 2, 
                             getPosY() + 2.5f, 
                             20);
        } else if (useClientColors.getValue()) {
            drawString(context, info.toString(), 
                      getPosX() + 2, 
                      getPosY() + 2.5f, 
                      HudEditor.getColor(1).getRGB());
        } else {
            drawString(context, info.toString(), 
                      getPosX() + 2, 
                      getPosY() + 2.5f, 
                      HudEditor.textColor.getValue().getColor());
        }
        
        setBounds(getPosX(), getPosY(), width, 10);
    }
}