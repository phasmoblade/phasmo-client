package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.player.MovementUtility;

public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        returnSneak = false;

        // Сброс GrimBow переменных
        grimBowOriginalOffhandItem = net.minecraft.item.ItemStack.EMPTY;
        grimBowIsSwapping = false;
        grimBowSwapTimer = 0;
        
        // Сброс GrimNew переменных
        grimNewTimer = 0;
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.NCP);
    private final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
    private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
    private final Setting<Boolean> food = new Setting<>("Food", true).addToGroup(selection);
    private final Setting<Boolean> projectiles = new Setting<>("Projectiles", true).addToGroup(selection);
    private final Setting<Boolean> shield = new Setting<>("Shield", true).addToGroup(selection);
    public final Setting<Boolean> soulSand = new Setting<>("SoulSand", true).addToGroup(selection);
    public final Setting<Boolean> honey = new Setting<>("Honey", true).addToGroup(selection);
    public final Setting<Boolean> slime = new Setting<>("Slime", true).addToGroup(selection);
    public final Setting<Boolean> ice = new Setting<>("Ice", true).addToGroup(selection);
    public final Setting<Boolean> sweetBerryBush = new Setting<>("SweetBerryBush", true).addToGroup(selection);
    public final Setting<Boolean> sneak = new Setting<>("Sneak", false).addToGroup(selection);
    public final Setting<Boolean> crawl = new Setting<>("Crawl", false).addToGroup(selection);

    // GrimLatest settings (убрано grimBoost, так как теперь работает через canNoSlow)

    // GrimBow settings (переименовано из GrimV4)
    private final Setting<Boolean> grimBowOnlyOnGround = new Setting<>("GrimBow Only On Ground", false, v -> mode.getValue() == Mode.GrimBow);

    private boolean returnSneak;
    
    // GrimBow variables (переименовано из GrimV4)
    private net.minecraft.item.ItemStack grimBowOriginalOffhandItem = net.minecraft.item.ItemStack.EMPTY;
    private boolean grimBowIsSwapping = false;
    private long grimBowSwapTimer = 0;
    
    // GrimNew variables
    private long grimNewTimer = 0;

    @Override
    public void onUpdate() {
        if (returnSneak) {
            mc.options.sneakKey.setPressed(false);
            mc.player.setSprinting(true);
            returnSneak = false;
        }

        if (mc.player.isUsingItem() && !mc.player.isRiding() && !mc.player.isFallFlying()) {
            switch (mode.getValue()) {
                case NCP -> {
                    // NCP режим - ничего не делаем
                }
                case StrictNCP -> sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                case Matrix -> {
                    if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                        mc.player.setVelocity(mc.player.getVelocity().x * 0.3, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.3);
                    } else if (mc.player.fallDistance > 0.2f)
                        mc.player.setVelocity(mc.player.getVelocity().x * 0.95f, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.95f);
                }
                case GrimBow -> {
                    // GrimBow - обрабатывается в onMove
                }
                case GrimNew -> {
                    // GrimNew - точная копия из Zenith
                    handleGrimNew();
                }
                case FunTime -> {
                    // FunTime - обход через смену слота
                    handleFunTime();
                }
            }
        }
    }


    @EventHandler
    public void onTick(EventTick event) {
        // GrimBow логика
        if (mode.getValue() == Mode.GrimBow && mc.player != null) {
            if (!mc.player.isUsingItem() && grimBowIsSwapping) {
                grimBowIsSwapping = false;
            }
        }
    }

    @EventHandler
    public void onMove(EventMove event) {
        // GrimBow логика (переименовано из GrimV4)
        if (mode.getValue() == Mode.GrimBow && mc.player != null && !mc.player.isFallFlying()) {
            if (!mc.player.isUsingItem() || !MovementUtility.isMoving()) return;

            if (grimBowOnlyOnGround.getValue() && !mc.player.isOnGround()) return;

            // Отменяем замедление (аналог e.cancel())
            event.setX(event.getX() * 1.0);
            event.setZ(event.getZ() * 1.0);

            // Проверяем таймер для переключения
            long currentTime = System.currentTimeMillis();
            if (currentTime - grimBowSwapTimer >= 30) {
                performGrimBowSwap();
                grimBowSwapTimer = currentTime;
            }
        }
        
        // GrimNew логика - как GrimBow но без лука
        if (mode.getValue() == Mode.GrimNew && mc.player != null && !mc.player.isFallFlying()) {
            if (!mc.player.isUsingItem() || !MovementUtility.isMoving()) return;

            // Отменяем замедление (аналог e.cancel())
            event.setX(event.getX() * 1.0);
            event.setZ(event.getZ() * 1.0);

            // Проверяем таймер для GrimNew действий
            long currentTime = System.currentTimeMillis();
            if (currentTime - grimNewTimer >= 50) { // 50мс интервал
                performGrimNewActions();
                grimNewTimer = currentTime;
            }
        }
        
    }

    public boolean canNoSlow() {
        if (!food.getValue() && mc.player.getActiveItem().getComponents().contains(DataComponentTypes.FOOD))
            return false;

        if (!shield.getValue() && mc.player.getActiveItem().getItem() == Items.SHIELD)
            return false;

        if (!projectiles.getValue()
                && (mc.player.getActiveItem().getItem() == Items.CROSSBOW || mc.player.getActiveItem().getItem() == Items.BOW || mc.player.getActiveItem().getItem() == Items.TRIDENT))
            return false;

        if (!mainHand.getValue() && mc.player.getActiveHand() == Hand.MAIN_HAND)
            return false;


        return true;
    }

    // GrimNew - улучшенная версия из Zenith
    private void handleGrimNew() {
        int useTime = mc.player.getItemUseTime();
        
        // Срабатываем только в первые 5 тиков для избежания детекции
        if (useTime < 5) {
            // Рандомизация - не всегда срабатываем
            if (Math.random() < 0.8) { // 80% шанс
                updateSlots();
                
                // Иногда добавляем закрытие экрана
                if (Math.random() < 0.6) { // 60% шанс
                    closeScreen(true);
                }
            }
        }
    }
    
    // GrimNew действия - как GrimBow но без лука
    private void performGrimNewActions() {
        // Отправляем пакеты как в оригинале Zenith
        if (Math.random() < 0.7) { // 70% шанс
            updateSlots();
        }
        
        if (Math.random() < 0.5) { // 50% шанс
            closeScreen(true);
        }
        
        // Иногда отправляем пакет смены слота
        if (Math.random() < 0.3) { // 30% шанс
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
    }
    
    // FunTime - обход через смену слота
    private void handleFunTime() {
        if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
    }
    
    // Методы из Zenith PlayerInventoryUtil - улучшенная версия
    private void updateSlots() {
        net.minecraft.screen.ScreenHandler screenHandler = mc.player.currentScreenHandler;
        
        // Иногда используем пустой стек вместо случайного предмета
        net.minecraft.item.ItemStack stack;
        if (Math.random() < 0.3) { // 30% шанс использовать пустой стек
            stack = net.minecraft.item.ItemStack.EMPTY;
        } else {
            stack = net.minecraft.registry.Registries.ITEM.get(mc.world.getRandom().nextInt(100)).getDefaultStack();
        }
        
        // Иногда используем другой слот
        int slot = Math.random() < 0.2 ? (int)(Math.random() * 9) : 0; // 20% шанс использовать другой слот
        
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            screenHandler.syncId, 
            screenHandler.getRevision(), 
            slot, 
            0, 
            SlotActionType.PICKUP_ALL, 
            stack, 
            Int2ObjectMaps.singleton(slot, stack)
        ));
    }
    
    private void closeScreen(boolean packet) {
        // Точная копия из Zenith PlayerInventoryUtil.closeScreen()
        if (packet) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            mc.player.closeHandledScreen();
        }
    }

    // GrimBow методы (переименовано из GrimV4)
    private void performGrimBowSwap() {
        net.minecraft.item.ItemStack currentOffhandItem = mc.player.getOffHandStack();
        int bowSlot = findBowInInventory();

        if (bowSlot != -1) {
            if (!grimBowIsSwapping) {
                grimBowOriginalOffhandItem = currentOffhandItem.copy();
                grimBowIsSwapping = true;
            }

            swapItemToOffhand(bowSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));

            if (!grimBowOriginalOffhandItem.isEmpty()) {
                int originalItemSlot = findItemInInventory(grimBowOriginalOffhandItem);
                if (originalItemSlot != -1) {
                    swapItemToOffhand(originalItemSlot);
                }
            }
        }
    }

    private int findBowInInventory() {
        // Ищем лук в хотбаре (слоты 0-8)
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BOW) {
                return i;
            }
        }

        // Ищем лук в инвентаре (слоты 9-35)
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BOW) {
                return i;
            }
        }

        return -1;
    }

    private int findItemInInventory(net.minecraft.item.ItemStack itemToFind) {
        // Ищем предмет в хотбаре (слоты 0-8)
        for (int i = 0; i < 9; i++) {
            if (net.minecraft.item.ItemStack.areItemsEqual(mc.player.getInventory().getStack(i), itemToFind)) {
                return i;
            }
        }

        // Ищем предмет в инвентаре (слоты 9-35)
        for (int i = 9; i < 36; i++) {
            if (net.minecraft.item.ItemStack.areItemsEqual(mc.player.getInventory().getStack(i), itemToFind)) {
                return i;
            }
        }

        return -1;
    }

    private void swapItemToOffhand(int inventorySlot) {
        int containerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        int offhandSlot = 40; // Слот левой руки

        // Меняем предмет в левую руку
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, containerSlot, offhandSlot, SlotActionType.SWAP, mc.player);
    }

    public enum Mode {
        NCP, StrictNCP, Matrix, GrimBow, GrimNew, FunTime
    }
}