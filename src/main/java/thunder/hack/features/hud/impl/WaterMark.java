package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextUtil;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.impl.BooleanSettingGroup;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WaterMark extends HudElement {
    public WaterMark() {
        super("WaterMark", 100, 35);
    }
    
    // Настройки только для Small режима
    private final Setting<Boolean> showBackground = new Setting<>("ShowBackground", true, v -> mode.getValue() == Mode.Small);
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100, v -> mode.getValue() == Mode.Small && showBackground.getValue());
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true, v -> mode.getValue() == Mode.Small && showBackground.getValue());
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> mode.getValue() == Mode.Small && showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> mode.getValue() == Mode.Small && showBackground.getValue() && enableBlur.getValue());
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> mode.getValue() == Mode.Small);
    private final Setting<HudFontHelper.FontStyle> playerNameFont = new Setting<>("PlayerNameFont", HudFontHelper.FontStyle.MODULES_RENDERER, v -> mode.getValue() == Mode.Small);
    private final Setting<HudFontHelper.FontStyle> serverFont = new Setting<>("ServerFont", HudFontHelper.FontStyle.MODULES_RENDERER, v -> mode.getValue() == Mode.Small);
    private final Setting<HudFontHelper.FontStyle> clientNameFont = new Setting<>("ClientNameFont", HudFontHelper.FontStyle.MODULES_RENDERER, v -> mode.getValue() == Mode.Small);

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Big);
    private final Setting<Boolean> ru = new Setting<>("RU", false);

    private final TextUtil textUtil = new TextUtil(
            "phasmo client",
            "фабос пидор",
            "фазмо клиент",
            "фазмоблейд сосал",
            "зига пидар",
            "алексей навальный",
            "ньюлосити буст",
            "паста клиент",
            "бля иди нахуй"
    );

    private enum Mode {
        Big, Small, Classic, BaltikaClient, Rifk
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        String username = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : mc.getSession().getUsername();

        if (mode.getValue() == Mode.Big) {
            float width = 106, height = 30;
            
            // Обычный рендеринг без дополнительных настроек
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), width, height, HudEditor.hudRound.getValue());
            
            // Обычный рендеринг текста
            FontRenderers.sf_bold.drawString(context.getMatrices(), "  PHASMO", getPosX() + 5.5f, getPosY() + 5, -1);
            FontRenderers.sf_bold.drawString(context.getMatrices(), "client", getPosX() + 35.5f, getPosY() + 21f, HudEditor.getColor(1).getRGB());
            setBounds(getPosX(), getPosY(), width, height);
        } else if (mode.getValue() == Mode.Small) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                // Рассчитываем ширину с учетом выбранного шрифта
                float offset1 = HudFontHelper.getStringWidth(username, playerNameFont.getValue()) + 66;
                float offset2 = HudFontHelper.getStringWidth((mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address), serverFont.getValue());
                float offset3 = (Managers.PROXY.isActive() ? HudFontHelper.getStringWidth(Managers.PROXY.getActiveProxy().getName(), playerNameFont.getValue()) + 11 : 0);
                float width = 50f + offset1 + offset2 - 34 + offset3;
                float height = 15f;

                // Рендеринг фона с системой закругления как в SessionInfo
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
                } else {
                    Render2DEngine.drawRoundedBlur(context.getMatrices(), getPosX(), getPosY(), width, height, cornerRadius.getValue(), HudEditor.blurColor.getValue().getColorObject());
                }

                Render2DEngine.setupRender();

                Render2DEngine.drawRect(context.getMatrices(), getPosX() + 13, getPosY() + 1.5f, 0.5f, 11, new Color(0x44FFFFFF, true));

                HudFontHelper.drawString(context, "Phasmo ", getPosX() + 18, getPosY() + 5, HudEditor.getColor(10).getRGB(), clientNameFont.getValue());

                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShaderTexture(0, TextureStorage.altLogo);
                Render2DEngine.renderGradientTexture(context.getMatrices(), getPosX() + 1, getPosY() + 2, 11, 11, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                RenderSystem.setShaderTexture(0, TextureStorage.playerIcon);
                Render2DEngine.renderGradientTexture(context.getMatrices(), getPosX() + 52, getPosY() + 3, 8, 8, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                RenderSystem.setShaderTexture(0, TextureStorage.serverIcon);
                Render2DEngine.renderGradientTexture(context.getMatrices(), getPosX() + offset1 - 1, getPosY() + 2, 10, 10, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                if (Managers.PROXY.isActive()) {
                    RenderSystem.setShaderTexture(0, TextureStorage.proxyIcon);
                    Render2DEngine.renderGradientTexture(context.getMatrices(), getPosX() + offset1 + offset2 + 15, getPosY() + 2, 10, 10, 0, 0, 128, 128, 128, 128,
                            HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                    FontRenderers.sf_bold.drawString(context.getMatrices(), Managers.PROXY.getActiveProxy().getName(), getPosX() + offset1 + offset2 + 27, getPosY() + 5, -1);
                }

                Render2DEngine.endRender();

                Render2DEngine.setupRender();
                RenderSystem.defaultBlendFunc();
                // Рендеринг текста с выбранными шрифтами только для имени игрока и адреса сервера
                HudFontHelper.drawString(context, username, getPosX() + 62, getPosY() + 4.5f, HudEditor.textColor.getValue().getColor(), playerNameFont.getValue());
                HudFontHelper.drawString(context, (mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address), getPosX() + offset1 + 12, getPosY() + 4.5f, HudEditor.textColor.getValue().getColor(), serverFont.getValue());
                Render2DEngine.endRender();
                setBounds(getPosX(), getPosY(), width, height);
            } else {
                String info = Formatting.DARK_GRAY + "| " + Formatting.RESET + username + Formatting.DARK_GRAY + " | " + Formatting.RESET + Managers.SERVER.getPing() + " ms" + Formatting.DARK_GRAY + " | " + Formatting.RESET + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address);
                String clientText = ru.getValue() ? textUtil + " " : "phasmoclient ";
                float width = HudFontHelper.getStringWidth(clientText, clientNameFont.getValue()) + HudFontHelper.getStringWidth(info, playerNameFont.getValue()) + 5;
                float height = 10;
                
                // Рендеринг фона с системой закругления
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
                } else {
                    Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), width, height, cornerRadius.getValue());
                }
                
                // Рендеринг текста с выбранным шрифтом
                HudFontHelper.drawString(context, clientText, getPosX() + 2, getPosY() + 2.5f, HudEditor.getColor(10).getRGB(), clientNameFont.getValue());
                HudFontHelper.drawString(context, info, getPosX() + 2 + HudFontHelper.getStringWidth(clientText, clientNameFont.getValue()), getPosY() + 2.5f, HudEditor.textColor.getValue().getColor(), playerNameFont.getValue());
                setBounds(getPosX(), getPosY(), width, height);
            }

        } else if (mode.getValue() == Mode.BaltikaClient) {
            float width = 100, height = 64;
            
            // Обычный рендеринг без дополнительных настроек
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), width, height, HudEditor.hudRound.getValue());

            Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + width, getPosY() + height, 1f);
            context.getMatrices().push();
            context.getMatrices().translate(getPosX() + 10, getPosY() + 32, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(mc.player.age * 3 + Render3DEngine.getTickDelta())));
            context.getMatrices().translate(-(getPosX() + 10), -(getPosY() + 32), 0);
            context.drawTexture(TextureStorage.baltika, (int) getPosX() - 10, (int) getPosY() + 2, 0, 0, 40, 64, 40, 64);
            context.getMatrices().pop();
            Render2DEngine.popWindow();

            // Обычный рендеринг текста
            FontRenderers.sf_bold.drawString(context.getMatrices(), "BALTIKA", getPosX() + 43, getPosY() + 41.5f, -1);
            setBounds(getPosX(), getPosY(), width, height);
        } else if (mode.is(Mode.Rifk)) {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

            // лень вставлять реал билд дату
            // too lazy to insert the real build date

            String info = Formatting.GREEN + String.format("ph4 | build: 16/06/2024 | rate: %d | %s", Math.round(Managers.SERVER.getTPS()), format.format(date));
            float width = FontRenderers.sf_bold.getStringWidth(info) + 5;
            float height = 8;
            
            // Обычный рендеринг без дополнительных настроек
            Render2DEngine.drawRectWithOutline(context.getMatrices(), getPosX(), getPosY(), width, height, Color.decode("#192A1A"), Color.decode("#833B7B"));
            Render2DEngine.drawGradientBlurredShadow1(context.getMatrices(), getPosX(), getPosY(), width, height, 10, Color.decode("#161A1E"), Color.decode("#161A1E"), Color.decode("#382E37"), Color.decode("#382E37"));
            
            // Обычный рендеринг текста
            FontRenderers.sf_bold.drawString(context.getMatrices(), info, getPosX() + 2.7f, getPosY() + 2.953f, HudEditor.textColor.getValue().getColor());
            setBounds(getPosX(), getPosY(), width, height);
        } else {
            String text = "phasmoclient v" + ThunderHack.VERSION;
            float width = FontRenderers.sf_bold.getStringWidth(text) + 11;
            float height = 13;
            
            // Обычный рендеринг без дополнительных настроек
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), width, height, HudEditor.hudRound.getValue());
            
            // Обычный рендеринг текста
            FontRenderers.sf_bold.drawString(context.getMatrices(), text, getPosX() + 5.5f, getPosY() + 5, HudEditor.getColor(10).getRGB());
            setBounds(getPosX(), getPosY(), width, height);
        }
    }

    @Override
    public void onUpdate() {
        textUtil.tick();
    }
}