package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import thunder.hack.utility.hud.HudFontHelper;

public class MemoryHud extends HudElement {
    public MemoryHud() {
        super("MemoryHud", 100, 10);
    }
    // Настройки шрифта и фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> showBackground.getValue());
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        long m = Runtime.getRuntime().maxMemory();
        long t = Runtime.getRuntime().totalMemory();
        long f = Runtime.getRuntime().freeMemory();
        long o = t - f;
        String str = "Mem: " + Formatting.WHITE + toMiB(o) + "/" + toMiB(m) + "MB [" + (o * 100L / m) + "%]";

        // Используем выбранный шрифт для расчета ширины
        float textWidth = HudFontHelper.getStringWidth(str, fontStyle.getValue());
        float pX = getPosX() > mc.getWindow().getScaledWidth() / 2f ? getPosX() - textWidth : getPosX();
        float totalWidth = textWidth + 21;
        float totalHeight = 13f;

        // Рисуем фон с учетом настроек
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            float cornerRadiusValue = this.cornerRadius.getValue();
            
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                if (enableBlur.getValue()) {
                    // Используем красивое размытие с учетом прозрачности
                    Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                            HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                            HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                            (int)(alpha * 255));
                    // Применяем прозрачность к blurOpacity
                    float finalBlurOpacity = blurOpacity.getValue() * alpha;
                    Render2DEngine.drawRoundedBlur(context.getMatrices(), pX, getPosY(), totalWidth, totalHeight, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
                } else {
                    // Обычный фон без размытия
                    Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                            HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                            HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                            (int)(alpha * 255));
                    Render2DEngine.drawRect(context.getMatrices(), pX, getPosY(), totalWidth, totalHeight, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
                }
                
                Render2DEngine.drawRect(context.getMatrices(), pX + 14, getPosY() + 2, 0.5f, 8, new Color(0x44FFFFFF, true));

                Render2DEngine.setupRender();
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShaderTexture(0, TextureStorage.memoryIcon);
                Render2DEngine.renderGradientTexture(context.getMatrices(), pX + 2, getPosY() + 1, 10, 10, 0, 0, 512, 512, 512, 512,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
                Render2DEngine.endRender();
            } else {
                // Для обычного стиля тоже рисуем фон с закруглением
                if (enableBlur.getValue()) {
                    Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                            HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                            HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                            (int)(alpha * 255));
                    float finalBlurOpacity = blurOpacity.getValue() * alpha;
                    Render2DEngine.drawRoundedBlur(context.getMatrices(), pX, getPosY(), totalWidth, totalHeight, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
                } else {
                    Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                            HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                            HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                            (int)(alpha * 255));
                    Render2DEngine.drawRect(context.getMatrices(), pX, getPosY(), totalWidth, totalHeight, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
                }
            }
        }

        // Рисуем текст с выбранным шрифтом
        HudFontHelper.drawString(context, str, pX + 18, getPosY() + 5, HudEditor.getColor(1).getRGB(), fontStyle.getValue());
        setBounds(pX, getPosY(), totalWidth, totalHeight);
    }

    private long toMiB(long bytes) {
        return bytes / 1024L / 1024L;
    }
}
