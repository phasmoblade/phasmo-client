package thunder.hack.features.hud.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import thunder.hack.features.cmd.impl.StaffCommand;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import thunder.hack.utility.hud.HudFontHelper;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;

public class StaffBoard extends HudElement {
    private static final Pattern validUserPattern = Pattern.compile("^\\w{3,16}$");
    private List<String> players = new ArrayList<>();
    private List<String> notSpec = new ArrayList<>();
    private Map<String, Identifier> skinMap = new HashMap<>();

    private float vAnimation, hAnimation;
    
    // Настройки фона и шрифтов
    private final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    private final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    private final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    private final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    private final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> backgroundSettings.getValue().isEnabled()).addToGroup(backgroundSettings);
    private final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.SF_BOLD_MINI);

    public StaffBoard() {
        super("StaffBoard", 50, 50);
    }
public static List<String> getOnlinePlayer() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .filter(profileName -> validUserPattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }

    public static List<String> getOnlinePlayerD() {
        List<String> S = new ArrayList<>();
        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            if (mc.isInSingleplayer() || player.getScoreboardTeam() == null) break;
            String prefix = player.getScoreboardTeam().getPrefix().getString();
            if (check(Formatting.strip(prefix).toLowerCase())
                    || StaffCommand.staffNames.toString().toLowerCase().contains(player.getProfile().getName().toLowerCase())
                    || player.getProfile().getName().toLowerCase().contains("1danil_mansoru1")
                    || player.getProfile().getName().toLowerCase().contains("barslan_")
                    || player.getProfile().getName().toLowerCase().contains("timmings")
                    || player.getProfile().getName().toLowerCase().contains("timings")
                    || player.getProfile().getName().toLowerCase().contains("ruthless")
                    || player.getScoreboardTeam().getPrefix().getString().contains("YT")
                    || (player.getScoreboardTeam().getPrefix().getString().contains("Y") && player.getScoreboardTeam().getPrefix().getString().contains("T"))) {
                String name = Arrays.asList(player.getScoreboardTeam().getPlayerList().toArray()).toString().replace("[", "").replace("]", "");

                if (player.getGameMode() == GameMode.SPECTATOR) {
                    S.add(player.getScoreboardTeam().getPrefix().getString() + name + ":gm3");
                    continue;
                }
                S.add(player.getScoreboardTeam().getPrefix().getString() + name + ":active");
            }
        }
        return S;
    }

    public List<String> getVanish() {
        List<String> list = new ArrayList<>();
        for (Team s : mc.world.getScoreboard().getTeams()) {
            if (s.getPrefix().getString().isEmpty() || mc.isInSingleplayer()) continue;
            String name = Arrays.asList(s.getPlayerList().toArray()).toString().replace("[", "").replace("]", "");

            if (getOnlinePlayer().contains(name) || name.isEmpty())
                continue;
            if (StaffCommand.staffNames.toString().toLowerCase().contains(name.toLowerCase())
                    && check(s.getPrefix().getString().toLowerCase())
                    || check(s.getPrefix().getString().toLowerCase())
                    || name.toLowerCase().contains("1danil_mansoru1")
                    || name.toLowerCase().contains("barslan_")
                    || name.toLowerCase().contains("timmings")
                    || name.toLowerCase().contains("timings")
                    || name.toLowerCase().contains("ruthless")
                    || s.getPrefix().getString().contains("YT")
                    || (s.getPrefix().getString().contains("Y") && s.getPrefix().getString().contains("T"))
            )
                list.add(s.getPrefix().getString() + name + ":vanish");
        }
        return list;
    }

    public static boolean check(String name) {
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.contains("mcfunny")) {
            return name.contains("helper") || name.contains("moder") || name.contains("модер") || name.contains("хелпер");
        }
        return name.contains("helper") || name.contains("moder") || name.contains("admin") || name.contains("owner") || name.contains("curator") || name.contains("куратор") || name.contains("модер") || name.contains("админ") || name.contains("хелпер") || name.contains("поддержка") || name.contains("сотрудник") || name.contains("зам") || name.contains("стажёр");
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        List<String> all = new java.util.ArrayList<>();
        all.addAll(players);
        all.addAll(notSpec);

        int y_offset1 = 0;
        float max_width = 50;

        float pointerX = 0;
        for (String player : all) {
            if (y_offset1 == 0)
                y_offset1 += 4;

            y_offset1 += 9;

            // Используем выбранный шрифт для расчета ширины
            float nameWidth = HudFontHelper.getStringWidth(player.split(":")[0], fontStyle.getValue());
            float timeWidth = HudFontHelper.getStringWidth((player.split(":")[1].equalsIgnoreCase("vanish") ? Formatting.RED + "V" : player.split(":")[1].equalsIgnoreCase("gm3") ? Formatting.RED + "V " + Formatting.YELLOW + "(GM3)" : Formatting.GREEN + "Z"), fontStyle.getValue());

            float width = (nameWidth + timeWidth) * 1.4f;

            if (width > max_width)
                max_width = width;

            if (timeWidth > pointerX)
                pointerX = timeWidth;
        }

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, max_width, 15);

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
            Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, HudEditor.hudRound.getValue());
        }

        // Рендеринг заголовка с выбранным шрифтом
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            HudFontHelper.drawCenteredString(context, "Staff", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject().getRGB(), fontStyle.getValue());
        } else {
            HudFontHelper.drawCenteredString(context, "Staff", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.getColor(10).getRGB(), fontStyle.getValue());
        }

        if (y_offset1 > 0) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 8, getPosY() + 14, new Color(0x54FFFFFF, true));
            } else {
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.5f, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            }
        }


        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 0;

        for (String player : all) {
            float px = getPosX() + (max_width - pointerX - 10);

            Identifier tex = getTexture(player);
            if (tex != null) {
                context.drawTexture(tex, (int) (getPosX() + 3), (int) (getPosY() + 16 + y_offset), 8, 8, 8, 8, 8, 8, 64, 64);
                context.drawTexture(tex, (int) (getPosX() + 3), (int) (getPosY() + 16 + y_offset), 8, 8, 40, 8, 8, 8, 64, 64);
            }

            // Рендеринг текста с выбранным шрифтом
            HudFontHelper.drawString(context, player.split(":")[0], getPosX() + 13, getPosY() + 19 + y_offset, HudEditor.textColor.getValue().getColor(), fontStyle.getValue());
            HudFontHelper.drawCenteredString(context, (player.split(":")[1].equalsIgnoreCase("vanish") ? Formatting.RED + "O" : player.split(":")[1].equalsIgnoreCase("gm3") ? Formatting.YELLOW + "O" : Formatting.GREEN + "O"),
                    px + (getPosX() + max_width - px) / 2f, getPosY() + 19 + y_offset, HudEditor.textColor.getValue().getColor(), fontStyle.getValue());
            Render2DEngine.drawRect(context.getMatrices(), px, getPosY() + 17 + y_offset, 0.5f, 8, new Color(0x44FFFFFF, true));
            y_offset += 9;
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @Override
    public void onUpdate() {
        if (mc.player != null && mc.player.age % 10 == 0) {
            players = getVanish();
            notSpec = getOnlinePlayerD();
            players.sort(String::compareTo);
            notSpec.sort(String::compareTo);
        }
    }

    private Identifier getTexture(String n) {
        Identifier id = null;
        if (skinMap.containsKey(n))
            id = skinMap.get(n);

        for (PlayerListEntry ple : mc.getNetworkHandler().getPlayerList())
            if (n.contains(ple.getProfile().getName())) {
                id = ple.getSkinTextures().texture();
                if (!skinMap.containsKey(n))
                    skinMap.put(n, id);
                break;
            }

        return id;
    }
}
