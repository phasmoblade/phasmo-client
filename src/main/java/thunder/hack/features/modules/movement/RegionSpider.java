package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class RegionSpider extends Module {
    public RegionSpider() {
        super("RegionSpider", Category.MOVEMENT);
    }

    public static boolean speed = true;

    public static void reg() {
        if (Module.mc.player != null && speed) {
            if (Module.mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
                Module.mc.interactionManager.interactItem(Module.mc.player, Hand.MAIN_HAND);
                Module.mc.player.swingHand(Hand.MAIN_HAND);
                Vec3d velocity = Module.mc.player.getVelocity();
                Module.mc.player.setVelocity(velocity.x, velocity.y + 0.05, velocity.z);
            }
        }
    }
}