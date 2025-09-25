package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;

import java.awt.*;

public class KillStats extends HudElement {
    int death = 0, killstreak = 0, kills = 0;
    
    // Настройки шрифта и фона
    private final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> backgroundSettings.getValue().isEnabled()).addToGroup(backgroundSettings);
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);
    
    public KillStats() {
        super("KillStats",100,35);
    }

    @Override
    public void onDisable() {
        death = 0;
        kills = 0;
        killstreak = 0;
    }

    @EventHandler
    private void death(PacketEvent.Receive event) {
        if(event.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3){
            if(!(pac.getEntity(mc.world) instanceof PlayerEntity)) return;
            if(pac.getEntity(mc.world) == mc.player){
                death++;
                killstreak = 0;
            }
            else if(Aura.target == pac.getEntity(mc.world) || AutoCrystal.target == pac.getEntity(mc.world)){
                killstreak++;
                kills++;
            }
        }
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        String streak = "KillStreak: " + Formatting.WHITE + killstreak;
        String kd = " KD: " + Formatting.WHITE + MathUtility.round((float) kills / (death > 0 ? death : 1));
        
        // Рассчитываем ширину с учетом выбранного шрифта
        float streakWidth = HudFontHelper.getStringWidth(streak, fontStyle.getValue());
        float kdWidth = HudFontHelper.getStringWidth(kd, fontStyle.getValue());
        float width = streakWidth + kdWidth + 21;
        float height = 13f;
        
        float pX = getPosX() > mc.getWindow().getScaledWidth() / 2f ? getPosX() - width : getPosX();

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
                Render2DEngine.drawRoundedBlur(context.getMatrices(), pX, getPosY(), width, height, cornerRadiusValue, bgColor, blurStrength.getValue(), finalBlurOpacity);
            } else {
                // Обычный фон без размытия
                Color bgColor = new Color(HudEditor.blurColor.getValue().getColorObject().getRed(), 
                                        HudEditor.blurColor.getValue().getColorObject().getGreen(), 
                                        HudEditor.blurColor.getValue().getColorObject().getBlue(), 
                                        (int)(alpha * 255));
                Render2DEngine.drawRect(context.getMatrices(), pX, getPosY(), width, height, cornerRadiusValue, alpha, bgColor, bgColor, bgColor, bgColor);
            }
        }
        
        // Разделитель
        Render2DEngine.drawRect(context.getMatrices(), pX + 14, getPosY() + 2, 0.5f, 8, new Color(0x44FFFFFF, true));
        
        // Иконка меча
        Render2DEngine.setupRender();
        RenderSystem.setShaderTexture(0, TextureStorage.swordIcon);
        Render2DEngine.renderGradientTexture(context.getMatrices(), pX + 2, getPosY() + 1, 10, 10, 0, 0, 16, 16, 16, 16,
                HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
        Render2DEngine.endRender();

        // Рендеринг текста с выбранным шрифтом
        float textY = HudFontHelper.getHudTextY(getPosY(), height, fontStyle.getValue());
        HudFontHelper.drawString(context, streak, pX + 18, textY, HudEditor.getColor(1).getRGB(), fontStyle.getValue());
        HudFontHelper.drawString(context, kd, pX + 18 + streakWidth, textY, HudEditor.getColor(1).getRGB(), fontStyle.getValue());
        
        setBounds(pX, getPosY(), width, height);
    }
}
