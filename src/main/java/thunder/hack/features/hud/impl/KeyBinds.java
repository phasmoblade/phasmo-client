package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.hud.HudFontHelper;

import java.awt.*;
import java.util.Objects;

public class KeyBinds extends HudElement {
    
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
    
    public enum DisplayMode {
        Default,
        Brackets
    }
    
    public final Setting<DisplayMode> displayMode = new Setting<>("Mode", DisplayMode.Default);
    public final Setting<ColorSetting> oncolor = new Setting<>("OnColor", new ColorSetting(-1));
    public final Setting<ColorSetting> offcolor = new Setting<>("OffColor", new ColorSetting(1));
    public final Setting<Boolean> onlyEnabled = new Setting<>("OnlyEnabled", false);
    
    // Настройки фона
    public final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    public final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    public final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    public final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    public final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    public final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> backgroundSettings.getValue().isEnabled()).addToGroup(backgroundSettings);
    
    // Настройки шрифтов
    public final Setting<FontStyle> titleFontStyle = new Setting<>("TitleFontStyle", FontStyle.SF_BOLD);
    public final Setting<FontStyle> textFontStyle = new Setting<>("TextFontStyle", FontStyle.SF_BOLD_MINI);
    public final Setting<Float> textSpacing = new Setting<>("TextSpacing", 9f, 6f, 15f);
    public final Setting<Float> textPadding = new Setting<>("TextPadding", 5f, 2f, 10f);
    public final Setting<Float> titleScale = new Setting<>("TitleScale", 1.0f, 0.5f, 2.0f);
    public final Setting<Float> textScale = new Setting<>("TextScale", 1.0f, 0.5f, 2.0f);
    
    // Настройки позиционирования заголовка
    public final Setting<Boolean> customTitlePosition = new Setting<>("CustomTitlePosition", false);
    public final Setting<Float> titleOffsetX = new Setting<>("TitleOffsetX", 0f, -50f, 50f, v -> customTitlePosition.getValue());
    public final Setting<Float> titleOffsetY = new Setting<>("TitleOffsetY", 0f, -50f, 50f, v -> customTitlePosition.getValue());

    public KeyBinds() {
        super("KeyBinds", 100, 100);
    }

    private float vAnimation, hAnimation;
    
    // Вспомогательные методы для работы с шрифтами
    private float getStringWidth(String text, FontStyle style) {
        // Конвертируем FontStyle в HudFontHelper.FontStyle
        HudFontHelper.FontStyle hudStyle = convertFontStyle(style);
        return HudFontHelper.getStringWidth(text, hudStyle) * textScale.getValue();
    }
    
    // Конвертер FontStyle в HudFontHelper.FontStyle
    private HudFontHelper.FontStyle convertFontStyle(FontStyle style) {
        return switch (style) {
            case SF_BOLD -> HudFontHelper.FontStyle.SF_BOLD;
            case SF_MEDIUM -> HudFontHelper.FontStyle.SF_MEDIUM;
            case MONSTERRAT -> HudFontHelper.FontStyle.MONSTERRAT;
            case PROFONT -> HudFontHelper.FontStyle.PROFONT;
            case COMFORTAA -> HudFontHelper.FontStyle.COMFORTAA;
            case ICONS -> HudFontHelper.FontStyle.ICONS;
            case ZONA_ULTRA -> HudFontHelper.FontStyle.ZONA_ULTRA;
            case SF_MEDIUM_MINI -> HudFontHelper.FontStyle.SF_MEDIUM_MINI;
            case SF_BOLD_MINI -> HudFontHelper.FontStyle.SF_BOLD_MINI;
            case SF_BOLD_MICRO -> HudFontHelper.FontStyle.SF_BOLD_MICRO;
        };
    }
    
    private void drawString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для всех шрифтов
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        if (textScale.getValue() != 1.0f) {
            context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
            roundedX = roundedX / textScale.getValue();
            roundedY = roundedY / textScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, roundedX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    private void drawTitleString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для заголовков
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        if (titleScale.getValue() != 1.0f) {
            context.getMatrices().scale(titleScale.getValue(), titleScale.getValue(), 1.0f);
            roundedX = roundedX / titleScale.getValue();
            roundedY = roundedY / titleScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, roundedX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, roundedX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    private void drawCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для центрированного текста
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        // Вычисляем ширину текста для центрирования
        float textWidth = getStringWidth(text, style);
        float centeredX = roundedX - textWidth / 2f;
        
        if (textScale.getValue() != 1.0f) {
            context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
            centeredX = centeredX / textScale.getValue();
            roundedY = roundedY / textScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, centeredX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    private void drawTitleCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для центрированного заголовка
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        // Вычисляем ширину текста для центрирования
        float textWidth = getStringWidth(text, style);
        float centeredX = roundedX - textWidth / 2f;
        
        if (titleScale.getValue() != 1.0f) {
            context.getMatrices().scale(titleScale.getValue(), titleScale.getValue(), 1.0f);
            centeredX = centeredX / titleScale.getValue();
            roundedY = roundedY / titleScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawString(context.getMatrices(), text, centeredX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    private void drawGradientCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для градиентного центрированного текста
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        // Вычисляем ширину текста для центрирования
        float textWidth = getStringWidth(text, style);
        float centeredX = roundedX - textWidth / 2f;
        
        if (textScale.getValue() != 1.0f) {
            context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1.0f);
            centeredX = centeredX / textScale.getValue();
            roundedY = roundedY / textScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_mini.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    private void drawTitleGradientCenteredString(DrawContext context, String text, float x, float y, int color, FontStyle style) {
        // Применяем правильное сглаживание для градиентного центрированного заголовка
        context.getMatrices().push();
        
        // Округляем координаты для четкого рендеринга
        float roundedX = Math.round(x);
        float roundedY = Math.round(y);
        
        // Вычисляем ширину текста для центрирования
        float textWidth = getStringWidth(text, style);
        float centeredX = roundedX - textWidth / 2f;
        
        if (titleScale.getValue() != 1.0f) {
            context.getMatrices().scale(titleScale.getValue(), titleScale.getValue(), 1.0f);
            centeredX = centeredX / titleScale.getValue();
            roundedY = roundedY / titleScale.getValue();
        }
        
        // Используем прямое обращение к FontRenderers для лучшего качества
        switch (style) {
            case SF_BOLD -> FontRenderers.sf_bold.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM -> FontRenderers.sf_medium.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case MONSTERRAT -> FontRenderers.monsterrat.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case PROFONT -> FontRenderers.profont.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case COMFORTAA -> FontRenderers.settings.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case ICONS -> FontRenderers.icons.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case ZONA_ULTRA -> FontRenderers.zona_ultra.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_MEDIUM_MINI -> FontRenderers.sf_medium_mini.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MINI -> FontRenderers.sf_bold_micro.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
            case SF_BOLD_MICRO -> FontRenderers.sf_bold_micro.drawGradientString(context.getMatrices(), text, centeredX, roundedY, color);
        }
        
        context.getMatrices().pop();
    }
    
    // Получить высоту шрифта для правильного выравнивания
    private float getFontHeight(FontStyle fontStyle) {
        return HudFontHelper.getFontHeight(convertFontStyle(fontStyle));
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        int y_offset1 = 0;
        float max_width = 50;
        float maxBindWidth = 0;

        float pointerX = 0;
        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue()) continue;
            if (!Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui && feature != ModuleManager.thunderHackGui) {
                if (y_offset1 == 0)
                    y_offset1 += 4;

                y_offset1 += textSpacing.getValue();

                float nameWidth = getStringWidth(feature.getName(), textFontStyle.getValue());
                String keyText = displayMode.getValue() == DisplayMode.Brackets ? 
                    "[" + getShortKeyName(feature) + "]" : getShortKeyName(feature);
                float bindWidth = getStringWidth(keyText, textFontStyle.getValue());

                if (bindWidth > maxBindWidth)
                    maxBindWidth = bindWidth;

                if(nameWidth > pointerX)
                    pointerX = nameWidth;
            }
        }

        float px = getPosX() + 10 + pointerX;
        max_width = Math.max(20 + pointerX + maxBindWidth, 50);

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, max_width, 15);

        // Рендеринг фона с новыми настройками
        if (backgroundSettings.getValue().isEnabled()) {
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

        // Позиционирование заголовка с возможностью кастомной настройки
        float titleHeight = getFontHeight(titleFontStyle.getValue()) * titleScale.getValue();
        float titleY = getPosY() + 1.5f + titleHeight / 2f; // Подняли еще на 1px выше разделителя
        float titleCenterX = getPosX() + hAnimation / 2f; // Точный центр по X
        
        // Применяем кастомные смещения если включено
        if (customTitlePosition.getValue()) {
            titleCenterX += titleOffsetX.getValue();
            titleY += titleOffsetY.getValue();
        }
        
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            drawTitleCenteredString(context, "KeyBinds", titleCenterX, titleY, HudEditor.textColor.getValue().getColorObject().getRGB(), titleFontStyle.getValue());
        } else {
            drawTitleGradientCenteredString(context, "KeyBinds", titleCenterX, titleY, 10, titleFontStyle.getValue());
        }

        // Рендерим разделители только если включен фон
        if (y_offset1 > 0 && backgroundSettings.getValue().isEnabled()) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 4, getPosY() + 13.5f, new Color(0x54FFFFFF, true));
            } else {
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 14, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            }
        }


        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 0;
        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue())
                continue;
            if (!Objects.equals(feature.getBind().getBind(), "None") && feature != ModuleManager.clickGui && feature != ModuleManager.thunderHackGui) {
                // Идеальное выравнивание текста с отступом от разделителя
                float baseY = getPosY() + 21.5f + y_offset; // Опустили еще на 1px ниже разделителя
                float fontHeight = getFontHeight(textFontStyle.getValue()) * textScale.getValue();
                float textY = baseY - fontHeight / 2f + 1f; // Точное центрирование по вертикали
                int textColor = feature.isOn() ? oncolor.getValue().getColor() : offcolor.getValue().getColor();
                
                // Подготавливаем текст клавиши
                String keyText = displayMode.getValue() == DisplayMode.Brackets ? 
                    "[" + getShortKeyName(feature) + "]" : getShortKeyName(feature);
                
                // Вычисляем ширину текста для идеального центрирования
                float nameWidth = getStringWidth(feature.getName(), textFontStyle.getValue());
                float keyWidth = getStringWidth(keyText, textFontStyle.getValue());
                
                // Центрируем весь блок "название | клавиша" относительно заголовка
                float totalWidth = nameWidth + keyWidth + 10; // 10px для разделителя и отступов
                float blockStartX = getPosX() + (hAnimation - totalWidth) / 2f;
                
                // Рендерим название модуля
                drawString(context, feature.getName(), blockStartX, textY, textColor, textFontStyle.getValue());
                
                // Рендерим клавишу
                float keyX = blockStartX + nameWidth + 10; // 10px отступ от названия
                drawString(context, keyText, keyX, textY, textColor, textFontStyle.getValue());
                
                // Разделитель только если включен фон (идеальное центрирование)
                if (backgroundSettings.getValue().isEnabled()) {
                    float separatorY = baseY - fontHeight / 2f;
                    float separatorX = blockStartX + nameWidth + 5; // Точно между названием и клавишей
                    Render2DEngine.drawRect(context.getMatrices(), separatorX, separatorY, 0.5f, fontHeight, new Color(0x44FFFFFF, true));
                }

                y_offset += textSpacing.getValue();
            }
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @NotNull
    public static String getShortKeyName(Module feature) {
        String sbind = feature.getBind().getBind();
        return switch (feature.getBind().getBind()) {
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            default -> sbind.toUpperCase();
        };
    }
}
