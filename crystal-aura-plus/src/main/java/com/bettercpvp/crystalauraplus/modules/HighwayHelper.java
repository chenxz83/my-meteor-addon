package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;

/**
 * Highway Helper - steers your yaw toward a selected direction (8 compass directions) while moving forward.
 * 高速助手 - 前进时自动把方向修正到选定的方向（按 Minecraft 坐标系与偏航角换算）。
 *
 * Minecraft yaw: 0=+Z(南), 90=-X(西), 180=-Z(北), -90=+X(东)
 */
public class HighwayHelper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("direction")
        .description("The direction to steer towards while moving forward. 前进时要保持的方向。")
        .defaultValue(Direction.ZPlus)
        .build()
    );

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
        .description("Don't correct when your yaw is within this many degrees of the target direction. 与目标方向偏差小于此值时不再修正。")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    public HighwayHelper() {
        super(Categories.Movement, "highway-helper", "Steers your yaw toward a selected direction while moving forward. 前进时自动保持选定方向。");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.options.forwardKey.isPressed()) return;

        float yaw = mc.player.getYaw();
        float targetYaw = (float) direction.get().yaw;
        float diff = MathHelper.wrapDegrees(targetYaw - yaw);

        if (Math.abs(diff) <= threshold.get()) return;

        float correction = (float) MathHelper.clamp((double) diff, -step.get(), step.get());
        mc.player.setYaw(yaw + correction);
    }

    public enum Direction {
        XPlus(-90),          // 东 +X
        ZPlus(0),            // 南 +Z
        XMinus(90),          // 西 -X
        ZMinus(180),         // 北 -Z
        XPlusZPlus(-45),     // 东南 +X +Z
        XPlusZMinus(-135),   // 东北 +X -Z
        XMinusZPlus(45),     // 西南 -X +Z
        XMinusZMinus(135);   // 西北 -X -Z

        public final double yaw;

        Direction(double yaw) {
            this.yaw = yaw;
        }

        @Override
        public String toString() {
            return switch (this) {
                case XPlus -> "X+";
                case ZPlus -> "Z+";
                case XMinus -> "X-";
                case ZMinus -> "Z-";
                case XPlusZPlus -> "X+ Z+";
                case XPlusZMinus -> "X+ Z-";
                case XMinusZPlus -> "X- Z+";
                case XMinusZMinus -> "X- Z-";
            };
        }
    }
}
