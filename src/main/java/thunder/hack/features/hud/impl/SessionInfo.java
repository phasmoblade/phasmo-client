package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import thunder.hack.utility.hud.HudFontHelper;

public class SessionInfo extends HudElement {
    
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
    
    public SessionInfo() {
        super("SessionInfo", 200, 100);
    }

    // Основные настройки отображения
    private final Setting<Boolean> showServer = new Setting<>("ShowServer", true);
    private final Setting<Boolean> showPlayerName = new Setting<>("ShowPlayerName", true);
    private final Setting<Boolean> showPlayTime = new Setting<>("ShowPlayTime", true);
    
    // Настройки фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100, v -> showBackground.getValue());
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true, v -> showBackground.getValue());
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> showBackground.getValue() && enableBlur.getValue());
    
    // Настройки текста
    private final Setting<Boolean> useGradient = new Setting<>("UseGradient", true);
    private final Setting<Boolean> useClientColors = new Setting<>("UseClientColors", true, v -> !useGradient.getValue());
    private final Setting<FontStyle> fontStyle = new Setting<>("FontStyle", FontStyle.SF_BOLD);
    private final Setting<FontStyle> titleFontStyle = new Setting<>("TitleFontStyle", FontStyle.SF_BOLD);
    private final Setting<FontStyle> infoFontStyle = new Setting<>("InfoFontStyle", FontStyle.SF_BOLD_MINI);
    
    // Настройки размеров и отступов
    private final Setting<Float> padding = new Setting<>("Padding", 8f, 0f, 20f);
    private final Setting<Float> spacing = new Setting<>("Spacing", 12f, 8f, 20f);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f);

    private long sessionStartTime = System.currentTimeMillis();
    
    // Вспомогательные методы для работы с шрифтами
    private float getStringWidth(String text, FontStyle style) {
        return switch (style) {
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
    
    private float getFontHeight(FontStyle style) {
        return switch (style) {
            case SF_BOLD -> 9f;
            case SF_MEDIUM -> 9f;
            case MONSTERRAT -> 9f;
            case PROFONT -> 9f;
            case COMFORTAA -> 9f;
            case ICONS -> 9f;
            case ZONA_ULTRA -> 8f;
            case SF_MEDIUM_MINI -> 7f;
            case SF_BOLD_MINI -> 7f;
            case SF_BOLD_MICRO -> 6f;
        };
    }
    
    private void drawString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, x, y, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, x, y, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, x, y, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, x, y, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, x, y, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, x, y, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, x, y, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, x, y, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, x, y, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, x, y, color);
        }
    }
    
    private void drawGradientString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientString(context.getMatrices(), text, x, y, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientString(context.getMatrices(), text, x, y, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientString(context.getMatrices(), text, x, y, color);
            case PROFONT -> FontRenderers.profont.drawGradientString(context.getMatrices(), text, x, y, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientString(context.getMatrices(), text, x, y, color);
            case ICONS -> FontRenderers.icons.drawGradientString(context.getMatrices(), text, x, y, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientString(context.getMatrices(), text, x, y, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientString(context.getMatrices(), text, x, y, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawGradientString(context.getMatrices(), text, x, y, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientString(context.getMatrices(), text, x, y, color);
        }
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        
        String playerName = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? 
            (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : 
            mc.getSession().getUsername();
        
        String serverInfo = mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address;
        String playTime = getPlayTime();
        
        // Вычисляем размеры контента
        float padding = this.padding.getValue();
        float spacing = this.spacing.getValue();
        
        // Заголовок
        String title = "Session Information";
        float titleWidth = getStringWidth(title, titleFontStyle.getValue());
        float titleHeight = getFontHeight(titleFontStyle.getValue());
        
        // Информация
        float maxInfoWidth = 0f;
        float infoHeight = 0f;
        
        if (showServer.getValue()) {
            String serverText = "Server: " + serverInfo;
            maxInfoWidth = Math.max(maxInfoWidth, getStringWidth(serverText, infoFontStyle.getValue()));
            infoHeight += getFontHeight(infoFontStyle.getValue());
        }
        
        if (showPlayerName.getValue()) {
            String nameText = "Name: " + playerName;
            maxInfoWidth = Math.max(maxInfoWidth, getStringWidth(nameText, infoFontStyle.getValue()));
            infoHeight += getFontHeight(infoFontStyle.getValue());
        }
        
        if (showPlayTime.getValue()) {
            String timeText = "Play time: " + playTime;
            maxInfoWidth = Math.max(maxInfoWidth, getStringWidth(timeText, infoFontStyle.getValue()));
            infoHeight += getFontHeight(infoFontStyle.getValue());
        }
        
        // Добавляем отступы между строками
        if (showServer.getValue() && showPlayerName.getValue()) infoHeight += 2f;
        if ((showServer.getValue() || showPlayerName.getValue()) && showPlayTime.getValue()) infoHeight += 2f;
        
        // Общие размеры
        float totalWidth = Math.max(titleWidth, maxInfoWidth) + padding * 2;
        float totalHeight = titleHeight + infoHeight + padding * 3;
        
        // Рисуем фон
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            float cornerRadius = this.cornerRadius.getValue();
            
            if (enableBlur.getValue()) {
                // Используем красивое размытие с учетом прозрачности
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                // Применяем прозрачность к blurOpacity
                float finalBlurOpacity = blurOpacity.getValue() * alpha;
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), totalWidth, totalHeight, cornerRadius, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), totalWidth, totalHeight, cornerRadius, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }
        
        // Рисуем заголовок
        float titleX = getPosX() + (totalWidth - titleWidth) / 2f;
        float titleY = getPosY() + padding;
        
        if (useGradient.getValue()) {
            drawGradientString(context, title, titleX, titleY, 20, titleFontStyle.getValue());
        } else if (useClientColors.getValue()) {
            drawString(context, title, titleX, titleY, HudEditor.getColor(1).getRGB(), titleFontStyle.getValue());
        } else {
            drawString(context, title, titleX, titleY, HudEditor.textColor.getValue().getColor(), titleFontStyle.getValue());
        }
        
        // Рисуем информацию
        float infoY = getPosY() + padding + titleHeight + 8f;
        
        if (showServer.getValue()) {
            String serverText = "Server: " + serverInfo;
            float serverX = getPosX() + padding;
            
            if (useGradient.getValue()) {
                drawGradientString(context, serverText, serverX, infoY, 20, infoFontStyle.getValue());
            } else if (useClientColors.getValue()) {
                drawString(context, serverText, serverX, infoY, HudEditor.getColor(1).getRGB(), infoFontStyle.getValue());
            } else {
                drawString(context, serverText, serverX, infoY, HudEditor.textColor.getValue().getColor(), infoFontStyle.getValue());
            }
            infoY += getFontHeight(infoFontStyle.getValue()) + 2f;
        }
        
        if (showPlayerName.getValue()) {
            String nameText = "Name: " + playerName;
            float nameX = getPosX() + padding;
            
            if (useGradient.getValue()) {
                drawGradientString(context, nameText, nameX, infoY, 20, infoFontStyle.getValue());
            } else if (useClientColors.getValue()) {
                drawString(context, nameText, nameX, infoY, HudEditor.getColor(1).getRGB(), infoFontStyle.getValue());
            } else {
                drawString(context, nameText, nameX, infoY, HudEditor.textColor.getValue().getColor(), infoFontStyle.getValue());
            }
            infoY += getFontHeight(infoFontStyle.getValue()) + 2f;
        }
        
        if (showPlayTime.getValue()) {
            String timeText = "Play time: " + playTime;
            float timeX = getPosX() + padding;
            
            if (useGradient.getValue()) {
                drawGradientString(context, timeText, timeX, infoY, 20, infoFontStyle.getValue());
            } else if (useClientColors.getValue()) {
                drawString(context, timeText, timeX, infoY, HudEditor.getColor(1).getRGB(), infoFontStyle.getValue());
            } else {
                drawString(context, timeText, timeX, infoY, HudEditor.textColor.getValue().getColor(), infoFontStyle.getValue());
            }
        }
        
        setBounds(getPosX(), getPosY(), totalWidth, totalHeight);
    }
    
    private String getPlayTime() {
        long currentTime = System.currentTimeMillis();
        long playTime = currentTime - sessionStartTime;
        
        long hours = playTime / (1000 * 60 * 60);
        long minutes = (playTime % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (playTime % (1000 * 60)) / 1000;
        
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        sessionStartTime = System.currentTimeMillis();
    }
}