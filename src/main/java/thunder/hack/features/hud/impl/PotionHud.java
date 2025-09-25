package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Formatting;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;

public class PotionHud extends HudElement {
    
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
    
    public PotionHud() {
        super("Potions", 100, 100);
    }

    private float vAnimation, hAnimation;
    
    // Вспомогательные методы для работы с шрифтами
    private float getStringWidth(String text, FontStyle style) {
        float baseWidth = switch (style) {
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
        return baseWidth * textScale.getValue();
    }
    
    private void drawString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        context.getMatrices().push();
        context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
        float scaledX = x / textScale.getValue();
        float scaledY = y / textScale.getValue();
        
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, scaledX, scaledY, color);
        };
        context.getMatrices().pop();
    }
    
    private void drawTitleCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        context.getMatrices().push();
        context.getMatrices().scale(titleScale.getValue(), titleScale.getValue(), 1.0f);
        float scaledX = x / titleScale.getValue();
        float scaledY = y / titleScale.getValue();
        
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case PROFONT -> FontRenderers.profont.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case COMFORTAA -> FontRenderers.settings.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ICONS -> FontRenderers.icons.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
        };
        context.getMatrices().pop();
    }
    
    private void drawTitleGradientCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        context.getMatrices().push();
        context.getMatrices().scale(titleScale.getValue(), titleScale.getValue(), 1.0f);
        float scaledX = x / titleScale.getValue();
        float scaledY = y / titleScale.getValue();
        
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case PROFONT -> FontRenderers.profont.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ICONS -> FontRenderers.icons.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
        };
        context.getMatrices().pop();
    }
    
    private void drawCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        context.getMatrices().push();
        context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
        float scaledX = x / textScale.getValue();
        float scaledY = y / textScale.getValue();
        
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case PROFONT -> FontRenderers.profont.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case COMFORTAA -> FontRenderers.settings.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ICONS -> FontRenderers.icons.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
        };
        context.getMatrices().pop();
    }
    
    private void drawGradientCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        context.getMatrices().push();
        context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
        float scaledX = x / textScale.getValue();
        float scaledY = y / textScale.getValue();
        
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case PROFONT -> FontRenderers.profont.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ICONS -> FontRenderers.icons.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientCenteredString(context.getMatrices(), text, scaledX, scaledY, color);
        };
        context.getMatrices().pop();
    }

    private final Setting<Boolean> colored = new Setting<>("Colored", false);
    
    // Настройки фона
    public final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    public final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100, v -> showBackground.getValue());
    public final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true, v -> showBackground.getValue());
    public final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> showBackground.getValue() && enableBlur.getValue());
    public final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> showBackground.getValue() && enableBlur.getValue());
    public final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> showBackground.getValue());
    
    // Настройки шрифтов
    public final Setting<FontStyle> titleFontStyle = new Setting<>("TitleFontStyle", FontStyle.SF_BOLD);
    public final Setting<FontStyle> textFontStyle = new Setting<>("TextFontStyle", FontStyle.SF_BOLD_MINI);
    public final Setting<Float> textSpacing = new Setting<>("TextSpacing", 9f, 6f, 15f);
    public final Setting<Float> textPadding = new Setting<>("TextPadding", 5f, 2f, 10f);
    public final Setting<Float> titleScale = new Setting<>("TitleScale", 1.0f, 0.5f, 2.0f);
    public final Setting<Float> textScale = new Setting<>("TextScale", 1.0f, 0.5f, 2.0f);

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "*:*";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            String sec = String.format("%02d", (var1 % 1200) / 20);
            return mins + ":" + sec;
        }
    }

        /*
        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        for (StatusEffectInstance potionEffect : effects) {
            StatusEffect potion = potionEffect.getEffectType().value();
            String power = "";
            switch (potionEffect.getAmplifier()) {
                case 0 -> power = "I";
                case 1 -> power = "II";
                case 2 -> power = "III";
                case 3 -> power = "IV";
                case 4 -> power = "V";
            }

            String s = potion.getName().getString() + " " + power;
            String s2 = getDuration(potionEffect) + "";

            Color c = new Color(potionEffect.getEffectType().value().getColor());
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), s + "  " + s2, getPosX() + 5, getPosY() + 20 + y_offset, colored.getValue() ? c.getRGB() : HudEditor.textColor.getValue().getColor());
            y_offset += 10;
        }*/

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        int y_offset1 = 0;
        float max_width = 50;

        float pointerX = 0;
        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            StatusEffect potion = potionEffect.getEffectType().value();

            if (y_offset1 == 0)
                y_offset1 += 4;

            y_offset1 += textSpacing.getValue();

            float nameWidth = getStringWidth(potion.getName().getString() + " " + (potionEffect.getAmplifier() + 1), textFontStyle.getValue());
            float timeWidth = getStringWidth(getDuration(potionEffect), textFontStyle.getValue());
            float width = (nameWidth + timeWidth) * 1.4f;

            if (width > max_width)
                max_width = width;

            if (timeWidth > pointerX)
                pointerX = timeWidth;
        }

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, max_width, 15);

        // Рендеринг фона с новыми настройками
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            float cornerRadiusValue = this.cornerRadius.getValue();
            
            if (enableBlur.getValue()) {
                // Используем красивое размытие с учетом прозрачности
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                // Применяем прозрачность к blurOpacity
                float finalBlurOpacity = blurOpacity.getValue() * alpha;
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }
        // Если ShowBackground = false, фон НЕ рисуется вообще

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            drawTitleCenteredString(context, "Potions", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject().getRGB(), titleFontStyle.getValue());
        } else {
            drawTitleGradientCenteredString(context, "Potions", getPosX() + hAnimation / 2, getPosY() + 4, 10, titleFontStyle.getValue());
        }

        // Рендерим разделители только если включен фон
        if (y_offset1 > 0 && showBackground.getValue()) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 4, getPosY() + 13.5f, new Color(0x54FFFFFF, true));
            } else {
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 14, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            }
        }

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 0;
        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            StatusEffect potion = potionEffect.getEffectType().value();

            float px = getPosX() + (max_width - pointerX - 10);

            // Рендерим иконку только если включен фон
            if (showBackground.getValue()) {
                context.getMatrices().push();
                context.getMatrices().translate(getPosX() + 2, getPosY() + 16 + y_offset, 0);
                context.drawSprite(0, 0, 0, 8, 8, mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType()));
                context.getMatrices().pop();
            }

            // Используем настраиваемые шрифты и отступы
            float textY = getPosY() + 19 + y_offset;
            int textColor = HudEditor.textColor.getValue().getColor();
            
            // Рендерим название эффекта с правильным отступом
            String effectName = potion.getName().getString() + " " + Formatting.RED + (potionEffect.getAmplifier() + 1);
            float textX = showBackground.getValue() ? getPosX() + 12 : getPosX() + textPadding.getValue();
            drawString(context, effectName, textX, textY, textColor, textFontStyle.getValue());
            
            // Рендерим время с правильным позиционированием
            float timeX = showBackground.getValue() ? 
                px + (getPosX() + max_width - px) / 2f : 
                getPosX() + max_width - textPadding.getValue();
            drawCenteredString(context, getDuration(potionEffect), timeX, textY, textColor, textFontStyle.getValue());
            
            // Разделитель только если включен фон
            if (showBackground.getValue()) {
                Render2DEngine.drawRect(context.getMatrices(), px, getPosY() + 17 + y_offset, 0.5f, 8, new Color(0x44FFFFFF, true));
            }
            y_offset += textSpacing.getValue();
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }
}