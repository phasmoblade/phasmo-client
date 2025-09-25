package thunder.hack.features.hud.impl;

import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.hud.HudFontHelper;

public final class Notifications extends HudElement {
    public Notifications() {
        super("Notifications", 200, 50);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.CrossHair);
    
    // Настройки фона и шрифтов для уведомлений
    public final Setting<BooleanSettingGroup> backgroundSettings = new Setting<>("Background", new BooleanSettingGroup(true));
    public final Setting<Integer> backgroundTransparency = new Setting<>("BackgroundTransparency", 100, 0, 100).addToGroup(backgroundSettings);
    public final Setting<Boolean> enableBlur = new Setting<>("EnableBlur", true).addToGroup(backgroundSettings);
    public final Setting<Float> blurStrength = new Setting<>("BlurStrength", 5f, 1f, 20f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    public final Setting<Float> blurOpacity = new Setting<>("BlurOpacity", 0.8f, 0.1f, 1f, v -> enableBlur.getValue()).addToGroup(backgroundSettings);
    public final Setting<Float> cornerRadius = new Setting<>("CornerRadius", 3f, 0f, 10f, v -> backgroundSettings.getValue().isEnabled()).addToGroup(backgroundSettings);
    public final Setting<HudFontHelper.FontStyle> fontStyle = new Setting<>("FontStyle", HudFontHelper.FontStyle.SF_BOLD_MINI);

    public enum Mode {
        Default, CrossHair, Text
    }
}



