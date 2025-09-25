package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Formatting;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;

import java.util.Objects;

public class AutoJoiner extends Module {
    private final Setting<ServerMode> serverMode = new Setting<>("Server", ServerMode.SpookyTimeDuels);
    private final Setting<Float> delay = new Setting<>("Delay", 1.0f, 0.1f, 5.0f);
    
    private final Timer timer = new Timer();
    private int grief;

    public AutoJoiner() {
        super("AutoJoiner", Module.Category.MISC);
    }

    @Override
    public void onEnable() {
        if (serverMode.getValue() == ServerMode.SpookyTimeDuels) {
            selectCompass();
        }
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent event) {
        if (serverMode.getValue() == ServerMode.SpookyTimeDuels) {
            handleSpookyTimeUpdate();
        }
        checkForDuels();
    }

    private void checkForDuels() {
        if (mc.world != null && mc.player != null) {
            String scoreboardTitle = getScoreboardTitle();
            if (scoreboardTitle != null && scoreboardTitle.contains("SpookyTime.net")) {
                sendMessage("Вы успешно зашли на SpookyTime Duels!");
                this.disable();
            }
        }
    }

    private String getScoreboardTitle() {
        try {
            if (mc.world != null && mc.world.getScoreboard() != null && mc.world.getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR) != null) {
                return Objects.requireNonNull(mc.world.getScoreboard().getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR)).getDisplayName().getString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (serverMode.getValue() == ServerMode.SpookyTimeDuels) {
            handleSpookyTimePacket(event);
        }

        if (event.getPacket() instanceof ScoreboardDisplayS2CPacket) {
            checkForDuels();
        }
    }

    private void selectCompass() {
        if (mc.player == null) return;
        
        // Ищем компас в хотбаре
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COMPASS) {
                slot = i;
                break;
            }
        }
        
        if (slot == -1) return;
        
        mc.player.getInventory().selectedSlot = slot;
        sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0.0f, 0.0f));
    }

    private void handleSpookyTimeUpdate() {
        if (mc.currentScreen instanceof GenericContainerScreen chestScreen) {
            Inventory inventory = chestScreen.getScreenHandler().getInventory();
            for (int i = 0; i < inventory.size(); ++i) {
                ItemStack stack = inventory.getStack(i);
                if (stack.getItem() == Items.STICKY_PISTON) {
                    if (mc.player == null) return;
                    if (mc.interactionManager == null) return;
                    
                    if (timer.passedMs((long) (delay.getValue() * 1000))) {
                        mc.interactionManager.clickSlot(chestScreen.getScreenHandler().syncId, i, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                        timer.reset();
                        return;
                    }
                }
            }
        } else {
            selectCompass();
        }
    }

    private void handleSpookyTimePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerListS2CPacket) {
            // Упрощенная проверка - просто перезапускаем поиск компаса
            selectCompass();
        }

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = Formatting.strip(packet.content().getString());
            if (message == null) return;
            if (message.contains("сервер переполнен") || message.contains("Подождите") || message.contains("поток игроков") || message.contains("перезагружается")) {
                selectCompass();
            }
        }
    }

    private enum ServerMode {
        SpookyTimeDuels
    }
}