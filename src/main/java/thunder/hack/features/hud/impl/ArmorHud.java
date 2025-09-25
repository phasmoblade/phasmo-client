package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.setting.impl.BooleanSettingGroup;

import java.awt.Color;

public class ArmorHud extends HudElement {
    public ArmorHud() {
        super("ArmorHud", 60, 25);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.V2);
    
    // Настройки фона
    private final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 0f, 0f, 8f).addToGroup(backgroundSettings);
    
    // Настройки размеров
    private final Setting<Float> itemSpacing = new Setting<>("ItemSpacing", 20f, 15f, 30f);
    private final Setting<Float> backgroundPadding = new Setting<>("BackgroundPadding", 4f, 0f, 10f);
    private final Setting<Float> itemSize = new Setting<>("ItemSize", 16f, 12f, 24f);
    
    // Настройки шрифта
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.SF_BOLD_MINI);
    private final Setting<Float> textScale = new Setting<>("TextScale", 1.0f, 0.5f, 2.0f);
    private final Setting<Float> textOffset = new Setting<>("TextOffset", 0f, -5f, 5f);

    private enum Mode {
        V1, V2
    }
    
    private Color getDurabilityColor(int percentage) {
        if (percentage >= 50) {
            // Зеленый цвет для 50-100%
            return new Color(0, 255, 0, 255);
        } else if (percentage >= 20) {
            // Желтый цвет для 20-49%
            return new Color(255, 255, 0, 255);
        } else {
            // Красный цвет для 1-19%
            return new Color(255, 0, 0, 255);
        }
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

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        
        // Подсчитываем количество предметов брони для расчета ширины фона
        int armorCount = 0;
        for (ItemStack itemStack : mc.player.getInventory().armor.reversed()) {
            if (!itemStack.isEmpty()) armorCount++;
        }
        
        if (armorCount == 0) return;
        
        // Размеры с учетом настроек
        float itemSpacing = this.itemSpacing.getValue();
        float itemSize = this.itemSize.getValue();
        float padding = backgroundPadding.getValue();
        float backgroundWidth = armorCount * itemSpacing - (itemSpacing - itemSize) + 2f; // +2 пикселя шире вправо
        float backgroundHeight = itemSize + 8f; // itemSize + место для текста (оптимизировано)
        
        // Рисуем фон с системой закругления как в SessionInfo
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
                Render2DEngine.drawRoundedBlur(context.getMatrices(), 
                    getPosX() - padding + 1, getPosY() - padding + 1, 
                    backgroundWidth + padding * 2, backgroundHeight + padding * 2, 
                    cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), 
                    getPosX() - padding + 1, getPosY() - padding + 1, 
                    backgroundWidth + padding * 2, backgroundHeight + padding * 2, 
                    cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }
        
        // Рисуем предметы брони
        float xItemOffset = getPosX();
        for (ItemStack itemStack : mc.player.getInventory().armor.reversed()) {
            if (itemStack.isEmpty()) {
                xItemOffset += itemSpacing;
                continue;
            }

            // Центрируем предмет в его слоте
            float itemX = xItemOffset + (itemSpacing - itemSize) / 2f;
            float itemY = getPosY();

            if (mode.is(Mode.V1)) {
                context.drawItem(itemStack, (int) itemX, (int) itemY);
                context.drawItemInSlot(mc.textRenderer, itemStack, (int) itemX, (int) itemY);
            } else {
                RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 0.35f);
                context.drawItem(itemStack, (int) itemX, (int) itemY);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                float offset = ((itemStack.getItem() instanceof ArmorItem ai) && ai.getSlotType() == EquipmentSlot.HEAD) ? -4 : 0;
                Render2DEngine.addWindow(context.getMatrices(), (int) itemX, itemY + offset + (itemSize - offset) * ((float) itemStack.getDamage() / (float) itemStack.getMaxDamage()), itemX + itemSize, itemY + itemSize, 1f);
                context.drawItem(itemStack, (int) itemX, (int) itemY);
                Render2DEngine.popWindow();
            }
            
            // Отображаем процент прочности под предметом
            if (itemStack.isDamageable()) {
                int maxDamage = itemStack.getMaxDamage();
                int currentDamage = itemStack.getDamage();
                int durability = maxDamage - currentDamage;
                int percentage = (int) ((durability * 100.0f) / maxDamage);
                
                String percentageText = String.valueOf(percentage);
                Color durabilityColor = getDurabilityColor(percentage);
                
                // Точное центрирование текста под предметом
                float textWidth = getStringWidth(percentageText);
                float itemCenterX = xItemOffset + itemSpacing / 2f; // Центр слота предмета
                float textX = itemCenterX - (textWidth / 2f);
                float textY = itemY + itemSize + 1f + textOffset.getValue(); // Ровно под предметом (оптимизировано)
                
                drawString(context, percentageText, textX, textY, durabilityColor.getRGB());
            }
            
            xItemOffset += itemSpacing;
        }

        // Обновляем границы (учитываем уменьшенные левый и нижний отступы)
        setBounds(getPosX() - padding + 1, getPosY() - padding + 1, backgroundWidth + padding * 2, backgroundHeight + padding * 2);
    }
}