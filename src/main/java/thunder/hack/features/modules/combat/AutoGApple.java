package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PostPlayerUpdateEvent;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;

public final class AutoGApple extends Module {
    public final Setting<Integer> Delay = new Setting<>("UseDelay", 0, 0, 2000);
    private final Setting<Float> health = new Setting<>("health", 15f, 1f, 36f);
    public Setting<Boolean> absorption = new Setting<>("Absorption", false);
    public Setting<Boolean> autoTotemIntegration = new Setting<>("AutoTotemIntegration", true);

    private boolean isActive;
    private final Timer useDelay = new Timer();

    public AutoGApple() {
        super("AutoGApple", Category.COMBAT);
    }

    @EventHandler
    public void onUpdate(PostPlayerUpdateEvent e) {
        if (!isEnabled()) return; // ВАЖНО: Проверяем включен ли модуль!
        if (fullNullCheck()) return;
        if (GapInOffHand()) {
            if (mc.player.getHealth() + (absorption.getValue() ? mc.player.getAbsorptionAmount() : 0) <= health.getValue() && useDelay.passedMs(Delay.getValue())) {
                isActive = true;
                if (mc.currentScreen != null && !mc.player.isUsingItem())
                    ((IMinecraftClient) mc).idoItemUse();
                else
                    mc.options.useKey.setPressed(true);
            } else if (isActive) {
                isActive = false;
                mc.options.useKey.setPressed(false);
            }
        } else if (isActive) {
            isActive = false;
            mc.options.useKey.setPressed(false);
        }
    }

    private boolean GapInOffHand() {
        if (autoTotemIntegration.getValue() && ModuleManager.autoTotem.isEnabled() && InventoryUtility.findItemInHotBar(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE).found()) {
            // Если AutoTotem включен и есть яблоки в hotbar, разрешаем использование
            return true;
        }

        return !mc.player.getOffHandStack().isEmpty() && (mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE || mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE);
    }
}