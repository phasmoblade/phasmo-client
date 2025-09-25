package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.Setting;

public class PVPResources extends HudElement {
    public PVPResources() {
        super("PVPResources", 50, 50);
    }
    
    // Настройки фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 0f, 0f, 8f);
public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        
        // Рисуем фон с учетом настроек
        if (showBackground.getValue()) {
            float alpha = backgroundTransparency.getValue() / 100f;
            Color bgColor = HudEditor.blurColor.getValue().getColorObject();
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int) (alpha * 255));
            
            if (enableBlur.getValue()) {
                float finalBlurOpacity = blurOpacity.getValue() * alpha;
                Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), 50, 50, 
                    cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                Render2DEngine.drawRect(context.getMatrices(), getPosX(), getPosY(), 50, 50, 
                    cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }

        setBounds(getPosX(), getPosY(), 50, 50);

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX(), getPosY() + 24.5f, getPosX() + 50, getPosY() + 25, new Color(0x54FFFFFF, true));
            Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 24.5f, getPosY() - 1, getPosX() + 25, getPosY() + 49, new Color(0x54FFFFFF, true));
        } else {
            Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 24.5f, getPosX() + 26, getPosY() + 25, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
            Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 26, getPosY() + 24.5f, getPosX() + 48, getPosY() + 25, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            Render2DEngine.verticalGradient(context.getMatrices(), getPosX() + 25.5f, getPosY() + 2, getPosX() + 26, getPosY() + 23, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
            Render2DEngine.verticalGradient(context.getMatrices(), getPosX() + 25.5f, getPosY() + 23, getPosX() + 26, getPosY() + 48, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
        }

        int totemCount = getItemCount(Items.TOTEM_OF_UNDYING);
        int xpCount = getItemCount(Items.EXPERIENCE_BOTTLE);
        int crystalCount = getItemCount(Items.END_CRYSTAL);
        int gappleCount = getItemCount(Items.ENCHANTED_GOLDEN_APPLE);

        List<ItemStack> list = new ArrayList<>();

        if (totemCount > 0) list.add(new ItemStack(Items.TOTEM_OF_UNDYING, totemCount));
        if (xpCount > 0) list.add(new ItemStack(Items.EXPERIENCE_BOTTLE, xpCount));
        if (crystalCount > 0) list.add(new ItemStack(Items.END_CRYSTAL, crystalCount));
        if (gappleCount > 0) list.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, gappleCount));

        for (int i = 0; i < list.size(); ++i) {
            int offsetX = i % 2 * 25;
            int offsetY = i / 2 * 25;
            context.drawItem(list.get(i), (int) (getPosX() + offsetX + 4), (int) (getPosY() + offsetY + 4));
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 151);
            Render2DEngine.drawBlurredShadow(context.getMatrices(), getPosX() + offsetX + 8, getPosY() + offsetY + 8, 9, 9, 12, Color.BLACK);
            FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), String.valueOf(list.get(i).getCount()), (int) (getPosX() + offsetX + 12), (int) (getPosY() + offsetY + 11f), HudEditor.textColor.getValue().getColor());
            context.getMatrices().pop();
        }
    }

    public int getItemCount(Item item) {
        if (mc.player == null) return 0;
        int n = 0;
        int n2 = 44;
        for (int i = 0; i <= n2; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() != item) continue;
            n += itemStack.getCount();
        }
        return n;
    }
}