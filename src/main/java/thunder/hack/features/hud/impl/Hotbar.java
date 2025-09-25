package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.gui.windows.WindowsScreen;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import thunder.hack.utility.hud.HudFontHelper;

public class Hotbar extends HudElement {
    public Hotbar() {
        super("Hotbar", 0, 0);
    }
    // Настройки фона
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 0f, 0f, 8f);

    public static final Setting<Mode> lmode = new Setting<>("LeftHandMode", Mode.Merged);

    public enum Mode {
        Merged, Separately
    }

    public void onRender2D(DrawContext context) {
        if (mc.currentScreen instanceof WindowsScreen)
            return;

        PlayerEntity playerEntity = mc.player;
        if (playerEntity != null) {
            MatrixStack matrices = context.getMatrices();
            int i = mc.getWindow().getScaledWidth() / 2;

            if (mc.player.getOffHandStack().isEmpty()) {
                // Рисуем фон для основного хотбара
                if (showBackground.getValue()) {
                    float alpha = backgroundTransparency.getValue() / 100f;
                    Color bgColor = HudEditor.blurColor.getValue().getColorObject();
                    bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int) (alpha * 255));
                    
                    if (enableBlur.getValue()) {
                        float finalBlurOpacity = blurOpacity.getValue() * alpha;
                        Render2DEngine.drawRoundedBlur(matrices, i - 90, mc.getWindow().getScaledHeight() - 25, 180, 20, 
                            cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
                    } else {
                        Render2DEngine.drawRect(matrices, i - 90, mc.getWindow().getScaledHeight() - 25, 180, 20, 
                            cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
                    }
                }
            } else if (lmode.getValue() == Mode.Merged) {
                // Рисуем фон для объединенного хотбара
                if (showBackground.getValue()) {
                    float alpha = backgroundTransparency.getValue() / 100f;
                    Color bgColor = HudEditor.blurColor.getValue().getColorObject();
                    bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int) (alpha * 255));
                    
                    if (enableBlur.getValue()) {
                        float finalBlurOpacity = blurOpacity.getValue() * alpha;
                        Render2DEngine.drawRoundedBlur(matrices, i - 111, mc.getWindow().getScaledHeight() - 25, 201, 20, 
                            cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
                    } else {
                        Render2DEngine.drawRect(matrices, i - 111, mc.getWindow().getScaledHeight() - 25, 201, 20, 
                            cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
                    }
                }

                if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                    Render2DEngine.drawRect(context.getMatrices(), i - 109 + 18, mc.getWindow().getScaledHeight() - 23, 0.5f, 15, new Color(0x44FFFFFF, true));
                } else {
                    Render2DEngine.verticalGradient(matrices, i - 109 + 18, mc.getWindow().getScaledHeight() - 22 + 1 - 4, i - 108 + 18 - 0.5f, mc.getWindow().getScaledHeight() - 11 + 1 - 4, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                    Render2DEngine.verticalGradient(matrices, i - 109 + 18, mc.getWindow().getScaledHeight() - 11 - 4, i - 108 + 18 - 0.5f, mc.getWindow().getScaledHeight() - 5, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
                }
            } else {
                // Рисуем фон для раздельного хотбара
                if (showBackground.getValue()) {
                    float alpha = backgroundTransparency.getValue() / 100f;
                    Color bgColor = HudEditor.blurColor.getValue().getColorObject();
                    bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int) (alpha * 255));
                    
                    if (enableBlur.getValue()) {
                        float finalBlurOpacity = blurOpacity.getValue() * alpha;
                        Render2DEngine.drawRoundedBlur(matrices, i - 90, mc.getWindow().getScaledHeight() - 25, 180, 20, 
                            cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
                        Render2DEngine.drawRoundedBlur(matrices, i - 112.5f, mc.getWindow().getScaledHeight() - 25, 20, 20, 
                            cornerRadius.getValue(), bgColor, blurStrength.getValue(), finalBlurOpacity);
                    } else {
                        Render2DEngine.drawRect(matrices, i - 90, mc.getWindow().getScaledHeight() - 25, 180, 20, 
                            cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
                        Render2DEngine.drawRect(matrices, i - 112.5f, mc.getWindow().getScaledHeight() - 25, 20, 20, 
                            cornerRadius.getValue(), alpha, bgColor, bgColor, bgColor, bgColor);
                    }
                }
            }

            Color c = HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry) ? new Color(0x7C151515, true) : new Color(0x7C2F2F2F, true);

            Render2DEngine.drawRect(matrices, i - 88 + playerEntity.getInventory().selectedSlot * 19.8f, mc.getWindow().getScaledHeight() - 24, 17, 17, HudEditor.hudRound.getValue(), 0.7f, c, c, c, c);
        }
    }

    // Bake only items
    public static void renderHotBarItems(float tickDelta, DrawContext context) {
        if (mc.currentScreen instanceof WindowsScreen)
            return;

        PlayerEntity playerEntity = mc.player;
        if (playerEntity != null) {

            MatrixStack matrices = context.getMatrices();
            int i = mc.getWindow().getScaledWidth() / 2;
            int o = mc.getWindow().getScaledHeight() - 16 - 3;

            if (mc.player.getOffHandStack().isEmpty()) {
            } else if (lmode.getValue() == Mode.Merged) {
                renderHotbarItem(context, i - 109, o - 5, playerEntity.getOffHandStack());
            } else {
                renderHotbarItem(context, i - 111, o - 5, playerEntity.getOffHandStack());
            }

            for (int m = 0; m < 9; ++m) {
                int n = i - 90 + m * 20 + 2;
                if (m == mc.player.getInventory().selectedSlot)
                    renderHotbarItem(context, n, o - 7, playerEntity.getInventory().main.get(m));
                else renderHotbarItem(context, n, o - 5, playerEntity.getInventory().main.get(m));
            }
        }
    }

    private static void renderHotbarItem(DrawContext context, int i, int j, ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate((float) (i + 8), (float) (j + 12), 0.0F);
            context.getMatrices().scale(0.9f, 0.9f, 1.0F);
            context.getMatrices().translate((float) (-(i + 8)), (float) (-(j + 12)), 0.0F);
            context.drawItem(itemStack, i, j);
            context.drawItemInSlot(mc.textRenderer, itemStack, i, j);
            context.getMatrices().pop();
        }
    }

    public static void renderXpBar(int x, MatrixStack matrices) {
        mc.getProfiler().push("expBar");
        int k;
        int l;
        mc.getProfiler().pop();

        if (mc.player.experienceLevel > 0) {
            mc.getProfiler().push("expLevel");
            String string = "" + mc.player.experienceLevel;
            k = (int) ((mc.getWindow().getScaledWidth() - FontRenderers.sf_bold_mini.getStringWidth(string)) / 2);
            l = mc.getWindow().getScaledHeight() - 31 - 4;
            FontRenderers.sf_bold_mini.drawString(matrices, string, k, l, 8453920);
            mc.getProfiler().pop();
        }
    }
}
