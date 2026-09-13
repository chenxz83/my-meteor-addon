package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;

/**
 * Highway Helper - keeps you moving straight along highways by gently snapping your yaw to the nearest 90 degrees.
 * 高速助手 - 前进时自动微调方向到最近的 90 度方向，跑高速不跑偏。
 */
public class HighwayHelper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> step = sgGeneral.add(new DoubleSetting.Builder()
        .name("step")
        .description("Maximum degrees to correct per tick. 每 tick 最多修正的角度。")
        .defaultValue(5)
        .min(1)
        .sliderMax(45)
        .build()
    );

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("threshold")
        .description("Don't correct when your yaw is within this many degrees of a 90 degree direction. 与 90 度方向偏差小于此值时不再修正。")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    public HighwayHelper() {
        super(Categories.Movement, "highway-helper", "Keeps you moving straight along highways by snapping your yaw to the nearest 90 degrees. 前进时自动保持直线方向。");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.options.forwardKey.isPressed()) return;

        float yaw = mc.player.getYaw();
        float target = Math.round(yaw / 90f) * 90f;
        float diff = MathHelper.wrapDegrees(target - yaw);

        if (Math.abs(diff) <= threshold.get()) return;

        float correction = (float) MathHelper.clamp((double) diff, -step.get(), step.get());
        mc.player.setYaw(yaw + correction);
    }
}
