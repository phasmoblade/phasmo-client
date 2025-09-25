package thunder.hack.utility.hud;

import net.minecraft.client.gui.DrawContext;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;

public class HudFontHelper {
    
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
        SF_BOLD_MICRO,
        MODULES_RENDERER
    }
    
    // Получить ширину строки в зависимости от выбранного шрифта
    public static float getStringWidth(String text, FontStyle fontStyle) {
        return switch (fontStyle) {
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
            case MODULES_RENDERER -> FontRenderers.getModulesRenderer().getStringWidth(text);
        };
    }
    
    // Получить высоту шрифта для правильного центрирования
    public static float getFontHeight(FontStyle fontStyle) {
        return switch (fontStyle) {
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
            case MODULES_RENDERER -> 8f;
        };
    }
    
    // Нарисовать строку с выбранным шрифтом
    public static void drawString(DrawContext context, String text, float x, float y, int color, FontStyle fontStyle) {
        switch (fontStyle) {
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
                case MODULES_RENDERER -> FontRenderers.getModulesRenderer().drawString(context.getMatrices(), text, x, y, color);
        }
    }
    
    // Нарисовать центрированную строку с выбранным шрифтом
    public static void drawCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle fontStyle) {
        float textWidth = getStringWidth(text, fontStyle);
        drawString(context, text, x - textWidth / 2f, y, color, fontStyle);
    }
    
    // Нарисовать градиентную строку с выбранным шрифтом
    public static void drawGradientString(DrawContext context, String text, float x, float y, int color, FontStyle fontStyle) {
        switch (fontStyle) {
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
                case MODULES_RENDERER -> FontRenderers.getModulesRenderer().drawGradientString(context.getMatrices(), text, x, y, color);
        }
    }
    
    // Нарисовать фон с учетом настроек (как в KeyBinds)
    public static void drawBackground(DrawContext context, float x, float y, float width, float height, 
                                    boolean showBackground, int backgroundTransparency, boolean enableBlur, 
                                    float blurStrength, float blurOpacity) {
        if (!showBackground) return;
        
        float alpha = backgroundTransparency / 100f;
        float cornerRadius = HudEditor.hudRound.getValue();
        
        if (enableBlur) {
            // Используем красивое размытие с учетом прозрачности (как в KeyBinds)
            Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                    HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                    HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                    (int)(alpha * 255));
            // Применяем прозрачность к blurOpacity
            float finalBlurOpacity = blurOpacity * alpha;
            Render2DEngine.drawRoundedBlur(context.getMatrices(), x, y, width, height, cornerRadius, bgColor, blurStrength, finalBlurOpacity);
        } else {
            // Обычный фон без размытия (как в KeyBinds)
            Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                    HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                    HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                    (int)(alpha * 255));
            Render2DEngine.drawRound(context.getMatrices(), x, y, width, height, cornerRadius, bgColor);
        }
    }
    
    // Получить правильную Y координату для центрирования текста
    public static float getCenteredY(float baseY, float backgroundHeight, FontStyle fontStyle) {
        // Используем простое центрирование с небольшим смещением вниз для лучшего визуального выравнивания
        return baseY + backgroundHeight / 2f - 1.0f;
    }
    
    // Получить Y координату для текста в HUD элементах (как в оригинальных модулях)
    public static float getHudTextY(float baseY, FontStyle fontStyle) {
        // Используем стандартное смещение как в оригинальных модулях
        // В StaffBoard используется getPosY() + 19 для текста, но у нас высота 13, поэтому используем 5
        return baseY + 5.0f;
    }
    
    // Получить Y координату для текста в HUD элементах с учетом высоты фона
    public static float getHudTextY(float baseY, float backgroundHeight, FontStyle fontStyle) {
        // Для элементов с высотой 13 используем смещение 5, для больших - центрируем
        if (backgroundHeight <= 15) {
            return baseY + 5.0f;
        } else {
            return baseY + backgroundHeight / 2f - 1.0f;
        }
    }
}