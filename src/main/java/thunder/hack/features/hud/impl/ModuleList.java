package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import thunder.hack.core.Managers;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ModuleList extends HudElement {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.ColorText);
    private final Setting<Integer> gste = new Setting<>("GS", 30, 1, 50);
    private final Setting<Boolean> glow = new Setting<>("glow", false);
    private final Setting<Boolean> hrender = new Setting<>("HideHud", true);
    private final Setting<Boolean> hhud = new Setting<>("HideRender", true);
    private final Setting<ColorSetting> color3 = new Setting<>("RectColor", new ColorSetting(-16777216));
    private final Setting<ColorSetting> color4 = new Setting<>("SideRectColor", new ColorSetting(-16777216));
    
    // Настройки фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f);
    
    // Настройки шрифта
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);
    private final Setting<Float> textScale = new Setting<>("TextScale", 1.0f, 0.5f, 2.0f);
    private final Setting<Float> textSpacing = new Setting<>("TextSpacing", 9f, 6f, 15f);
    private final Setting<Float> textPadding = new Setting<>("TextPadding", 3f, 1f, 8f);

    public ModuleList() {
        super("ArrayList", 50, 30);
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        boolean reverse = getPosX() > (mc.getWindow().getScaledWidth() / 2f);
        float currentY = getPosY();
        float maxWidth = 0;
        float reversedX = getPosX();

        List<Module> list;

        try {
            list = Managers.MODULE.getEnabledModules().stream().sorted(Comparator.comparing(module -> getStringWidth(module.getFullArrayString()) * -1)).toList();
        } catch (IllegalArgumentException ex) {
            return;
        }

        // Первый проход - вычисляем размеры
        for (Module module : list) {
            if (!shouldRender(module))
                continue;

            String moduleText = module.getDisplayName() + Formatting.GRAY + (module.getDisplayInfo() != null ? " [" + Formatting.WHITE + module.getDisplayInfo() + Formatting.GRAY + "]" : "");
            float textWidth = getStringWidth(moduleText);
            float moduleWidth = textWidth + textPadding.getValue() * 2;
            
            if (moduleWidth > maxWidth)
                maxWidth = moduleWidth;
        }

        // Второй проход - рендерим модули
        for (Module module : list) {
            if (!shouldRender(module))
                continue;

            String moduleText = module.getDisplayName() + Formatting.GRAY + (module.getDisplayInfo() != null ? " [" + Formatting.WHITE + module.getDisplayInfo() + Formatting.GRAY + "]" : "");
            float textWidth = getStringWidth(moduleText);
            float moduleWidth = textWidth + textPadding.getValue() * 2;
            float moduleHeight = textSpacing.getValue();

            Color color1 = HudEditor.getColor((int) (currentY - getPosY()));

            // Рисуем фон с учетом настроек
            if (showBackground.getValue()) {
                float moduleX = reverse ? reversedX - moduleWidth : getPosX();
                float moduleY = currentY;
                
                if (enableBlur.getValue()) {
                    // Размытый фон
                    Render2DEngine.drawRoundedBlur(context.getMatrices(), moduleX, moduleY, moduleWidth, moduleHeight, 
                                                 0f, // Убрали настройку CornerRadius
                                                 mode.getValue() == Mode.ColorRect ? color1 : color3.getValue().getColorObject(), 
                                                 blurStrength.getValue(), blurOpacity.getValue());
                } else {
                    // Обычный фон с прозрачностью
                    Color bgColor = mode.getValue() == Mode.ColorRect ? color1 : color3.getValue().getColorObject();
                    int alpha = (int) (backgroundTransparency.getValue() * 2.55f); // Конвертируем 0-100 в 0-255
                    Color transparentBgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), alpha);
                    Render2DEngine.drawRect(context.getMatrices(), moduleX, moduleY, moduleWidth, moduleHeight, 0f, alpha, transparentBgColor, transparentBgColor, transparentBgColor, transparentBgColor);
                }
                
                // Боковая полоска с прозрачностью
                Color sideColor = mode.getValue() == Mode.ColorRect ? color4.getValue().getColorObject() : color1;
                int sideAlpha = (int) (backgroundTransparency.getValue() * 2.55f); // Конвертируем 0-100 в 0-255
                Color transparentSideColor = new Color(sideColor.getRed(), sideColor.getGreen(), sideColor.getBlue(), sideAlpha);
                
                if (reverse) {
                    Render2DEngine.drawRect(context.getMatrices(), reversedX + 1f, currentY, 2.0f, moduleHeight, transparentSideColor);
                } else {
                    Render2DEngine.drawRect(context.getMatrices(), getPosX() - 2.0f, currentY, 2.0f, moduleHeight, transparentSideColor);
                }
            }

            // Рисуем тень/свечение
            if (glow.getValue()) {
                Render2DEngine.drawBlurredShadow(context.getMatrices(), 
                    reverse ? reversedX - moduleWidth - 3 : getPosX(), 
                    currentY - 1, 
                    moduleWidth + 4, 
                    moduleHeight + 2, 
                    gste.getValue(), 
                    color1);
            }

            // Рисуем текст
            float textX = reverse ? reversedX - textWidth - textPadding.getValue() : getPosX() + textPadding.getValue();
            float textY = currentY + textPadding.getValue() + 1f;
            drawString(context, moduleText, textX, textY, mode.getValue() == Mode.ColorRect ? -1 : color1.getRGB());

            // Обновляем позицию для следующего модуля
            currentY += moduleHeight;
        }

        // Устанавливаем границы
        float totalHeight = currentY - getPosY();
        setBounds(getPosX(), getPosY(), (int) maxWidth * (reverse ? -1 : 1), (int) totalHeight);
    }

    private boolean shouldRender(Module m) {
        return m.isDrawn() && (!hrender.getValue() || m.getCategory() != Category.RENDER) && (!hhud.getValue() || m.getCategory() != Category.HUD);
    }
    
    // Вспомогательные методы для работы с шрифтами
    private float getStringWidth(String text) {
        return HudFontHelper.getStringWidth(text, fontStyle.getValue()) * textScale.getValue();
    }
    
    private void drawString(DrawContext context, String text, float x, float y, int color) {
        context.getMatrices().push();
        context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1f);
        HudFontHelper.drawString(context, text, x / textScale.getValue(), y / textScale.getValue(), color, fontStyle.getValue());
        context.getMatrices().pop();
    }
    
    private void drawCenteredString(DrawContext context, String text, float x, float y, int color) {
        context.getMatrices().push();
        context.getMatrices().scale(textScale.getValue(), textScale.getValue(), 1f);
        float textWidth = getStringWidth(text) / textScale.getValue();
        HudFontHelper.drawString(context, text, (x - textWidth / 2) / textScale.getValue(), y / textScale.getValue(), color, fontStyle.getValue());
        context.getMatrices().pop();
    }

    private enum Mode {
        ColorText, ColorRect
    }
}

