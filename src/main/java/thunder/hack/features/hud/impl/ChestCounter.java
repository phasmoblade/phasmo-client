package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;

import static thunder.hack.features.modules.render.StorageEsp.getBlockEntities;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.Setting;

public class ChestCounter extends HudElement {
    public ChestCounter() {
        super("ChestCounter", 50, 10);
    }
    
    // Настройки шрифта и фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100, v -> showBackground.getValue());
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true, v -> showBackground.getValue());
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> showBackground.getValue());
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);
public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        Pair<Integer, Integer> chests = getChestCount();
        String str = "Chests: " + Formatting.WHITE + "S:" + chests.getLeft() + " D:" + chests.getRight();
        
        // Рассчитываем ширину с учетом выбранного шрифта
        float textWidth = HudFontHelper.getStringWidth(str, fontStyle.getValue());
        float height = 13f;
        float pX = getPosX() > mc.getWindow().getScaledWidth() / 2f ? getPosX() - textWidth : getPosX();

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
                Render2DEngine.drawRoundedBlur(context.getMatrices(), pX, getPosY(), textWidth, height, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), pX, getPosY(), textWidth, height, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }

        // Рендеринг текста с выбранным шрифтом
        float textY = HudFontHelper.getHudTextY(getPosY(), height, fontStyle.getValue());
        HudFontHelper.drawString(context, str, pX, textY, HudEditor.getColor(1).getRGB(), fontStyle.getValue());
        
        setBounds(pX, getPosY(), textWidth, height);
    }

    public Pair<Integer, Integer> getChestCount() {
        int singleCount = 0;
        int doubleCount = 0;

        for (BlockEntity be : getBlockEntities()) {
            if (be instanceof ChestBlockEntity chest) {
                ChestType chestType = mc.world.getBlockState(chest.getPos()).get(ChestBlock.CHEST_TYPE);
                if (chestType == ChestType.SINGLE) {
                    singleCount++;
                } else doubleCount++;
            }
        }
        return new Pair<>(singleCount, doubleCount / 2);
    }
}
