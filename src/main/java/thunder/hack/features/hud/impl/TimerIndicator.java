package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.movement.Timer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.EaseOutCirc;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.Setting;

import java.awt.*;

public class TimerIndicator extends HudElement {
    private final EaseOutCirc timerAnimation = new EaseOutCirc();
    
    // Настройки шрифта и фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100, v -> showBackground.getValue());
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true, v -> showBackground.getValue());
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> showBackground.getValue());
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);

    public TimerIndicator() {
        super("TimerIndicator", 60, 10);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        float width = 65;
        float height = 15f;
        String text = Timer.energy >= 0.99f ? "100%" : (int) Math.ceil(Timer.energy * 100) + "%";

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
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), width, height, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), width, height, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }

        // Прогресс бар
        Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), width * Timer.energy, height, 3f, 0.4f);

        // Рендеринг текста с выбранным шрифтом (центрированный)
        float textY = HudFontHelper.getHudTextY(getPosY(), height, fontStyle.getValue());
        float textX = getPosX() + width / 2 - HudFontHelper.getStringWidth(text, fontStyle.getValue()) / 2;
        HudFontHelper.drawString(context, text, textX, textY, new Color(200, 200, 200, 255).getRGB(), fontStyle.getValue());

        setBounds(getPosX(), getPosY(), width, height);
    }

    @Override
    public void onUpdate() {
        timerAnimation.update();
    }
}
