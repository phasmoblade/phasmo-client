package thunder.hack.features.hud.impl;

import com.google.common.collect.Lists;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.impl.BooleanSettingGroup;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KillFeed extends HudElement {
    public KillFeed() {
        super("KillFeed", 50, 50);
    }

    private Setting<Boolean> resetOnDeath = new Setting<>("ResetOnDeath", true);
    
    // Настройки шрифта и фона
    private final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> backgroundSettings.getValue().isEnabled()).addToGroup(backgroundSettings);
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.MODULES_RENDERER);

    private final List<KillComponent> players = new ArrayList<>();

    private float vAnimation, hAnimation;

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        int y_offset1 = 0;
        float scale_x = 30;

        for (KillComponent kc : Lists.newArrayList(players)) {
            if (FontRenderers.modules.getStringWidth(kc.getString()) > scale_x)
                scale_x = FontRenderers.modules.getStringWidth(kc.getString());
            y_offset1 += 15;
        }

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, scale_x + 10, 15);

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
        } else {
            // Используем старую систему HudBase
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, HudEditor.hudRound.getValue());
        }

        // Заголовок
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), "KillFeed", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject());
        } else {
            FontRenderers.sf_bold.drawGradientCenteredString(context.getMatrices(), "KillFeed", getPosX() + hAnimation / 2, getPosY() + 4, 10);
        }

        // Разделитель
        if (y_offset1 > 0) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 14, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            } else {
                Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 4, getPosY() + 13.5f, new Color(0x54FFFFFF, true));
            }
        }

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 3;
        for (KillComponent kc : Lists.newArrayList(players)) {
            // Рендеринг текста с выбранным шрифтом
            HudFontHelper.drawString(context, kc.getString(), getPosX() + 5, getPosY() + 18 + y_offset, -1, fontStyle.getValue());
            y_offset += 10;
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @EventHandler
    public void onPacket(PacketEvent.@NotNull Receive e) {
        if (!(e.getPacket() instanceof EntityStatusS2CPacket pac)) return;
        if (pac.getStatus() == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES && pac.getEntity(mc.world) instanceof PlayerEntity pl) {

            if(pl == mc.player && resetOnDeath.getValue()) {
                players.clear();
                return;
            }

            if ((Aura.target != null && Aura.target == pac.getEntity(mc.world)) || (AutoCrystal.target != null && AutoCrystal.target == pac.getEntity(mc.world))) {
                for (KillComponent kc : Lists.newArrayList(players))
                    if (Objects.equals(kc.getName(), pl.getName().getString())) {
                        kc.increase();
                        return;
                    }
                players.add(new KillComponent(pl.getName().getString()));
            }
        }
    }

    @Override
    public void onDisable() {
        players.clear();
    }

    private class KillComponent {
        private String name;
        private int count;

        public KillComponent(String name) {
            this.name = name;
            this.count = 1;
        }

        public void increase() {
            count++;
        }

        public String getName() {
            return name;
        }

        public String getString() {
            return Formatting.RED + "EZ - " + Formatting.RESET + name + (count > 1 ?  (" [" + Formatting.GRAY + "x" + count + Formatting.RESET + "]") : "");
        }
    }
}
