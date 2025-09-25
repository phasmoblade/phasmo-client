package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.Module.Category;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public class AutoOil extends Module {
    private final Setting<Integer> hitThreshold = new Setting("HitThreshold", 12, 1, 30);
    private final Setting<Integer> returnDelay = new Setting("ReturnDelay", 5, 1, 20);
    private int hitCount = 0;
    private final Timer returnTimer = new Timer();
    private boolean needsReturn = false;
    private boolean hasOilInInventory = false;
    private int originalSwordSlot = -1;
    private ItemStack swordStack;

    public AutoOil() {
        super("AutoOil", Category.COMBAT);
        this.swordStack = ItemStack.EMPTY;
    }

    public void onEnable() {
        super.onEnable();
        this.hitCount = 0;
        this.needsReturn = false;
        this.hasOilInInventory = this.findOilSlot() != -1;
        if (!this.hasOilInInventory) {
            this.debug("Масло не найдено в инвентаре. Модуль не будет работать.");
        }

    }

    public void onUpdate() {
        if (!this.hasOilInInventory && this.findOilSlot() != -1) {
            this.hasOilInInventory = true;
        }

        if (this.needsReturn && this.returnTimer.passedMs((long)((Integer)this.returnDelay.getValue() * 50))) {
            this.returnSword();
            this.needsReturn = false;
        }

    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (mc.player != null) {
            if (!this.needsReturn) {
                if (this.hasOilInInventory) {
                    ItemStack held = mc.player.getMainHandStack();
                    if (this.isSword(held)) {
                        ++this.hitCount;
                        if (this.hitCount >= (Integer)this.hitThreshold.getValue()) {
                            int oilSlot = this.findOilSlot();
                            if (oilSlot == -1) {
                                this.hasOilInInventory = false;
                                this.debug("Масло не найдено в инвентаре. Пропускаем применение.");
                                this.hitCount = 0;
                                return;
                            }

                            this.originalSwordSlot = mc.player.getInventory().selectedSlot;
                            this.swordStack = held.copy();
                            this.applyOil();
                            this.hitCount = 0;
                            this.needsReturn = true;
                            this.returnTimer.reset();
                        }

                    }
                }
            }
        }
    }

    private boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            String name = stack.getName().getString().toLowerCase();
            return name.contains("sword") || name.contains("меч");
        }
    }

    private void applyOil() {
        int oilSlot = this.findOilSlot();
        if (oilSlot == -1) {
            this.debug("Масло не найдено в инвентаре");
        } else {
            int swordSlot = mc.player.getInventory().selectedSlot + 36;
            this.originalSwordSlot = mc.player.getInventory().selectedSlot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, oilSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, swordSlot, 0, SlotActionType.PICKUP, mc.player);
            this.debug("Масло нанесено на меч после " + String.valueOf(this.hitThreshold.getValue()) + " ударов");
        }
    }

    private void returnSword() {
        if (this.originalSwordSlot != -1) {
            int swordSlot = this.originalSwordSlot + 36;
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, swordSlot, 0, SlotActionType.PICKUP, mc.player);
                this.debug("Меч возвращен в руку");
            } else {
                this.debug("Курсор пуст, нечего возвращать");
            }

            this.originalSwordSlot = -1;
        }
    }

    private int findEmptySlot() {
        for(int i = 9; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i < 9 ? i + 36 : i;
            }
        }

        return -1;
    }

    private int findOilSlot() {
        for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getName().getString().toLowerCase();
                if (name.contains("oil") || name.contains("масло")) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }

        return -1;
    }
}
