//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

public class Stopper extends Module {
    private final Setting<Float> speed = new Setting("HSpeed", 1.0F, 0.1F, 3.0F);
    private final Setting<Float> hspeed = new Setting("VSpeed", 0.42F, 0.1F, 3.0F);
    private final Setting<Float> fallDistance = new Setting("FallDistance", 0.2F, 0.1F, 1.0F);
    private boolean freeze = false;
    private boolean wasInAir = false;
    private double startFallY = (double)0.0F;
    private double lastY = (double)0.0F;
    private float fakeYaw;
    private float fakePitch;
    private float prevFakeYaw;
    private float prevFakePitch;
    private float prevScroll;
    private double fakeX;
    private double fakeY;
    private double fakeZ;
    private double prevFakeX;
    private double prevFakeY;
    private double prevFakeZ;

    public Stopper() {
        super("Stopper", Category.MOVEMENT);
    }

    public void onEnable() {
        mc.chunkCullingEnabled = false;
        this.freeze = false;
        this.wasInAir = false;
        this.startFallY = mc.player.getY();
        this.lastY = mc.player.getY();
        this.fakePitch = mc.player.getPitch();
        this.fakeYaw = mc.player.getPitch();
        this.prevFakePitch = this.fakePitch;
        this.prevFakeYaw = this.fakeYaw;
        this.fakeX = mc.player.getX();
        this.fakeY = mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose());
        this.fakeZ = mc.player.getX();
        this.prevFakeX = mc.player.getX();
        this.prevFakeY = mc.player.getY();
        this.prevFakeZ = mc.player.getZ();
    }

    public void onDisable() {
        if (!fullNullCheck()) {
            mc.chunkCullingEnabled = true;
            this.freeze = false;
        }
    }

    @EventHandler(
            priority = 100
    )
    public void onSync(EventSync e) {
        this.prevFakeYaw = this.fakeYaw;
        this.prevFakePitch = this.fakePitch;
        this.fakeYaw = mc.player.getYaw();
        this.fakePitch = mc.player.getYaw();
        double currentY = mc.player.getY();
        if (!mc.player.isOnGround()) {
            if (!this.wasInAir) {
                this.startFallY = currentY;
                this.wasInAir = true;
            }

            if (currentY < this.lastY && this.startFallY - currentY >= (double)(Float)this.fallDistance.getValue()) {
                this.freeze = true;
            }
        } else {
            this.wasInAir = false;
            if (this.freeze) {
                this.freeze = false;
            }
        }

        this.lastY = currentY;
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
        if (mc.player != null) {
            double[] motion = MovementUtility.forward((double)(Float)this.speed.getValue());
            this.prevFakeX = this.fakeX;
            this.prevFakeY = this.fakeY;
            this.prevFakeZ = this.fakeZ;
            this.fakeX += motion[0];
            this.fakeZ += motion[1];
            if (mc.options.jumpKey.isPressed()) {
                this.fakeY += (double)(Float)this.hspeed.getValue();
            }

            if (mc.options.sneakKey.isPressed()) {
                this.fakeY -= (double)(Float)this.hspeed.getValue();
            }

            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
            mc.player.input.jumping = false;
            mc.player.input.sneaking = false;
        }
    }

    @EventHandler(
            priority = -100
    )
    public void onMove(EventMove e) {
        if (this.freeze) {
            e.setX((double)0.0F);
            e.setY((double)0.0F);
            e.setZ((double)0.0F);
            e.cancel();
        }

    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (this.freeze && e.getPacket() instanceof PlayerMoveC2SPacket) {
            e.cancel();
        }

    }

    @EventHandler
    public void onScroll(EventMouse e) {
        if (e.getAction() == 2) {
            if (e.getButton() > 0) {
                this.speed.setValue((Float)this.speed.getValue() + 0.05F);
            } else {
                this.speed.setValue((Float)this.speed.getValue() - 0.05F);
            }

            this.prevScroll = (float)e.getButton();
        }

    }

    public float getFakeYaw() {
        return (float)Render2DEngine.interpolate((double)this.prevFakeYaw, (double)this.fakeYaw, (double)Render3DEngine.getTickDelta());
    }

    public float getFakePitch() {
        return (float)Render2DEngine.interpolate((double)this.prevFakePitch, (double)this.fakePitch, (double)Render3DEngine.getTickDelta());
    }

    public double getFakeX() {
        return Render2DEngine.interpolate(this.prevFakeX, this.fakeX, (double)Render3DEngine.getTickDelta());
    }

    public double getFakeY() {
        return Render2DEngine.interpolate(this.prevFakeY, this.fakeY, (double)Render3DEngine.getTickDelta());
    }

    public double getFakeZ() {
        return Render2DEngine.interpolate(this.prevFakeZ, this.fakeZ, (double)Render3DEngine.getTickDelta());
    }
}
