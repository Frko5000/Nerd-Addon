// skidded by ___hj from skid hack



package com.nerds.addon.modules;

import com.nerds.addon.NerdAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class FastSwim extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("multiplier")
        .description("how much faster you swim")
        .defaultValue(2.0)
        .min(1.0)
        .sliderMax(10.0)
        .build()
    );

    private static final double BASE_SWIM_SPEED = 0.1;

    public FastSwim() {
        super(NerdAddon.CATEGORY, "fast-swim", "Makes you swim faster");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isTouchingWater()) return;
        if (mc.player.input == null) return;

        float forward = ((mc.options.forwardKey.isPressed() ? 1f : 0f) - (mc.options.backKey.isPressed() ? 1f : 0f));
        float sideways = ((mc.options.leftKey.isPressed() ? 1f : 0f) - (mc.options.rightKey.isPressed() ? 1f : 0f));
        if (forward == 0 && sideways == 0) return;

        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);

        double targetX = (-Math.sin(yawRad) * forward + Math.cos(yawRad) * sideways);
        double targetZ = ( Math.cos(yawRad) * forward + Math.sin(yawRad) * sideways);

        double len = Math.sqrt(targetX * targetX + targetZ * targetZ);
        if (len > 1e-4) {
            targetX = (targetX / len) * BASE_SWIM_SPEED * multiplier.get();
            targetZ = (targetZ / len) * BASE_SWIM_SPEED * multiplier.get();
        }

        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(targetX, vel.y, targetZ);
    }
}
