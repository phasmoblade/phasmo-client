package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.utility.Timer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.util.math.Vec3d;

public class AresMineHelper extends Module {
    private final Setting<Bind> crossbowButton = new Setting("Арбалет", new Bind(86, false, false));
    private final Setting<Bind> invulnScrollButton = new Setting("Свиток неуязвимости", new Bind(66, false, false));
    private final Setting<Bind> meteorScrollButton = new Setting("Свиток метеора", new Bind(78, false, false));
    private final Setting<Bind> slowScrollButton = new Setting("Свиток замедления", new Bind(77, false, false));
    private final Setting<Bind> celestialThreadButton = new Setting("Небесная нить", new Bind(71, false, false));
    private final Setting<Bind> blessingButton = new Setting("Благословение", new Bind(72, false, false));
    private final Setting<Bind> thorHammerJumpButton = new Setting("Прыжок молота тора", new Bind(89, false, false));
    private final Setting<Boolean> useCrossbow = new Setting("Использовать арбалет", true);
    private final Setting<Boolean> useInvulnScroll = new Setting("Использовать свиток неуязвимости", true);
    private final Setting<Boolean> useMeteorScroll = new Setting("Использовать свиток метеора", true);
    private final Setting<Boolean> useSlowScroll = new Setting("Использовать свиток замедления", true);
    private final Setting<Boolean> useCelestialThread = new Setting("Использовать небесную нить", true);
    private final Setting<Boolean> useBlessing = new Setting("Использовать благословение", true);
    private final Setting<Boolean> useThorHammerJump = new Setting("Использовать прыжок молота тора", true);
    private final Setting<Boolean> safeMode = new Setting("SafeMode", true);
    private final Setting<Boolean> requireModifierForLetters = new Setting("RequireModifier", true);
    private final Setting<Integer> thorHammerJumpPower = new Setting("Сила прыжка тора", 3, 1, 10);
    private final Timer useDelay = new Timer();
    private boolean isUsingItem = false;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public AresMineHelper() {
        super("AresMineHelper", Category.COMBAT);
    }

    public void onUpdate() {
        if (mc.player != null) {
            if (mc.currentScreen != null) {
                this.isUsingItem = false;
            } else if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null || !mc.inGameHud.getChatHud().isChatFocused()) {
                if (this.useDelay.passedMs(300L)) {
                    if (!this.isUsingItem || !mc.player.isUsingItem()) {
                        if ((Boolean)this.useCrossbow.getValue() && this.isKeyPressed(this.crossbowButton) && this.canSafelyUseItem(this.crossbowButton)) {
                            int crossbowSlot = this.findItemSlot(Items.CROSSBOW);
                            if (crossbowSlot != -1) {
                                this.useItem(crossbowSlot);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useInvulnScroll.getValue() && this.isKeyPressed(this.invulnScrollButton) && this.canSafelyUseItem(this.invulnScrollButton)) {
                            int invulnScroll = this.findItemByName("неуязвимости");
                            if (invulnScroll != -1) {
                                this.useItem(invulnScroll);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useMeteorScroll.getValue() && this.isKeyPressed(this.meteorScrollButton) && this.canSafelyUseItem(this.meteorScrollButton)) {
                            int meteorScroll = this.findItemByName("метеора");
                            if (meteorScroll == -1) {
                                meteorScroll = this.findItemByName("meteor");
                            }

                            if (meteorScroll != -1) {
                                this.useItem(meteorScroll);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useSlowScroll.getValue() && this.isKeyPressed(this.slowScrollButton) && this.canSafelyUseItem(this.slowScrollButton)) {
                            int slowScroll = this.findItemByName("замедления");
                            if (slowScroll == -1) {
                                slowScroll = this.findItemByName("slow");
                            }

                            if (slowScroll != -1) {
                                this.useItem(slowScroll);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useCelestialThread.getValue() && this.isKeyPressed(this.celestialThreadButton) && this.canSafelyUseItem(this.celestialThreadButton)) {
                            int celestialThread = this.findItemByName("нить");
                            if (celestialThread == -1) {
                                celestialThread = this.findItemByName("thread");
                            }

                            if (celestialThread != -1) {
                                this.useItem(celestialThread);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useBlessing.getValue() && this.isKeyPressed(this.blessingButton) && this.canSafelyUseItem(this.blessingButton)) {
                            int blessing = this.findItemByName("благословение");
                            if (blessing == -1) {
                                blessing = this.findItemByName("blessing");
                            }

                            if (blessing != -1) {
                                this.useItem(blessing);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if ((Boolean)this.useThorHammerJump.getValue() && this.isKeyPressed(this.thorHammerJumpButton) && this.canSafelyUseItem(this.thorHammerJumpButton)) {
                            int thorHammer = this.findItemByName("молот тора");
                            if (thorHammer == -1) {
                                thorHammer = this.findItemByName("thor");
                            }

                            if (thorHammer != -1) {
                                this.performThorHammerJump(thorHammer);
                                this.useDelay.reset();
                                this.isUsingItem = true;
                                return;
                            }
                        }

                        if (!mc.player.isUsingItem()) {
                            this.isUsingItem = false;
                        }

                    }
                }
            }
        }
    }

    private void performThorHammerJump(int hammerSlot) {
        int currentSlot = mc.player.getInventory().selectedSlot;
        int targetSlot = hammerSlot < 36 ? hammerSlot : hammerSlot - 36;
        Vec3d playerPos = mc.player.getPos();
        double jumpPower = (double)(Integer)this.thorHammerJumpPower.getValue() * (double)0.5F;
        if (hammerSlot >= 36 && hammerSlot < 45) {
            mc.player.getInventory().selectedSlot = targetSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            this.executor.schedule(() -> {
                if (mc.player != null) {
                    mc.player.addVelocity((double)0.0F, jumpPower, (double)0.0F);
                    mc.player.getInventory().selectedSlot = currentSlot;
                    this.debug("Thor hammer jump executed!");
                }

            }, 1L, TimeUnit.MILLISECONDS);
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hammerSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + currentSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            this.executor.schedule(() -> {
                if (mc.player != null) {
                    mc.player.addVelocity((double)0.0F, jumpPower, (double)0.0F);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + currentSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hammerSlot, 0, SlotActionType.PICKUP, mc.player);
                    this.debug("Thor hammer jump executed with inventory swap!");
                }

            }, 1L, TimeUnit.MILLISECONDS);
        }

    }

    private int findItemSlot(Item item) {
        for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.getItem() == item) {
                return i < 9 ? i + 36 : i;
            }
        }

        return -1;
    }

    private int findItemByName(String namePart) {
        String[] keywords = new String[]{namePart.toLowerCase()};
        if (namePart.equalsIgnoreCase("неуязвимости")) {
            keywords = new String[]{"неуязвим", "invulner", "непоб", "safety", "защит", "protect"};
        } else if (namePart.equalsIgnoreCase("метеора")) {
            keywords = new String[]{"метеор", "meteor", "огн", "fire", "пожар"};
        } else if (namePart.equalsIgnoreCase("замедления")) {
            keywords = new String[]{"замедл", "slow", "туман", "fog", "mist"};
        } else if (namePart.equalsIgnoreCase("нить")) {
            keywords = new String[]{"небесн", "нить", "celestial", "thread", "sky", "целест"};
        } else if (namePart.equalsIgnoreCase("благословение")) {
            keywords = new String[]{"благослов", "blessing", "holy", "свят", "booster", "boost"};
        } else if (namePart.equalsIgnoreCase("молот тора")) {
            keywords = new String[]{"молот", "тор", "hammer", "thor", "мьёльнир", "mjolnir", "mace"};
        }

        for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty()) {
                String name = stack.getName().getString().toLowerCase();

                for(String keyword : keywords) {
                    if (name.contains(keyword)) {
                        return i < 9 ? i + 36 : i;
                    }
                }
            }
        }

        return -1;
    }

    private void useItem(int slot) {
        int currentSlot = mc.player.getInventory().selectedSlot;
        int targetSlot = slot < 36 ? slot : slot - 36;
        if (slot >= 36 && slot < 45) {
            mc.player.getInventory().selectedSlot = targetSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            this.debug("Using item in hotbar slot " + targetSlot);
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + currentSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            this.debug("Using item from inventory slot " + slot);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + currentSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        }

    }

    private boolean isChatActive() {
        if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null && mc.inGameHud.getChatHud().isChatFocused()) {
            return true;
        } else if (mc.currentScreen != null) {
            return true;
        } else {
            return mc.options.chatKey.isPressed() || mc.player.getItemUseTime() > 0 || mc.player.isUsingItem();
        }
    }

    public boolean isKeyPressed(Setting<Bind> bindSetting) {
        Bind bind = (Bind)bindSetting.getValue();
        int key = bind.getKey();
        if (key == -1) {
            return false;
        } else if (bind.isMouse()) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == 1;
        } else {
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
        }
    }

    private boolean canSafelyUseItem(Setting<Bind> bindSetting) {
        if (!(Boolean)this.safeMode.getValue()) {
            return true;
        } else {
            int keyCode = ((Bind)bindSetting.getValue()).getKey();
            if (keyCode >= 65 && keyCode <= 90) {
                if (this.isChatActive()) {
                    return false;
                }

                if ((Boolean)this.requireModifierForLetters.getValue()) {
                    boolean modifierPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), 341) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 345) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 340) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 344) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 342) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 346) == 1;
                    return modifierPressed;
                }
            }

            return true;
        }
    }

    public void onDisable() {
        this.executor.shutdownNow();
    }

    public void debug(String message) {
    }
}