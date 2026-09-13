package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Highway Helper - steers your yaw toward a selected direction while moving forward (works with Auto Walk / flight),
 * and can automatically dodge obstacles ahead by strafing (A/D).
 * 高速助手 - 前进时自动把方向修正到选定方向（支持配合自动走路/平飞挂机），可自动按 A/D 躲避前方障碍物。
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

    private final Setting<Boolean> autoWalkMode = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-walk-mode")
        .description("Work with Auto Walk/flight: keep the direction even when the forward key is not pressed, as long as you are moving. 与自动走路/平飞配合：不按前进键但只要在移动也会保持方向。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> avoidObstacles = sgGeneral.add(new BoolSetting.Builder()
        .name("avoid-obstacles")
        .description("Automatically dodge obstacles ahead by pressing A/D. 自动按 A/D 躲避前方障碍物。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> avoidRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("avoid-range")
        .description("How many blocks ahead to scan for obstacles. 检测前方多少格内的障碍物。")
        .defaultValue(5)
        .min(1)
        .sliderMax(8)
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

    private boolean strafeLeft;
    private boolean strafeRight;

    public HighwayHelper() {
        super(Categories.Movement, "highway-helper", "Steers your yaw toward a selected direction while moving forward, and can dodge obstacles ahead (works with Auto Walk). 前进时自动保持选定方向并可躲避前方障碍物（配合自动走路/平飞）。");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.options == null) return;

        boolean movingForward = mc.options.forwardKey.isPressed()
            || (autoWalkMode.get() && mc.player.getVelocity().horizontalLength() > 0.1);

        if (!movingForward) {
            setStrafe(false, false);
            return;
        }

        // Steer yaw toward the selected direction
        float yaw = mc.player.getYaw();
        float targetYaw = (float) direction.get().yaw;
        float diff = MathHelper.wrapDegrees(targetYaw - yaw);

        if (Math.abs(diff) > threshold.get()) {
            float correction = (float) MathHelper.clamp((double) diff, -step.get(), step.get());
            mc.player.setYaw(yaw + correction);
            yaw += correction;
        }

        // Obstacle avoidance
        if (avoidObstacles.get()) {
            updateDodge(yaw);
        }
        else {
            setStrafe(false, false);
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.options != null) setStrafe(false, false);
    }

    private void updateDodge(float yaw) {
        int range = (int) Math.round(avoidRange.get());
        double px = mc.player.getX(), pz = mc.player.getZ();
        int baseY = (int) Math.floor(mc.player.getY());

        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);   // forward
        double rx = -Math.cos(rad), rz = -Math.sin(rad);  // right side

        for (int d = 1; d <= range; d++) {
            double cx = px + fx * d, cz = pz + fz * d;

            for (int col = -1; col <= 1; col++) {
                double ox = cx + rx * 0.3 * col, oz = cz + rz * 0.3 * col;
                BlockPos feet = BlockPos.ofFloored(ox, baseY, oz);

                if (isBlocking(feet) || isBlocking(feet.up())) {
                    boolean wantLeft = false, wantRight = false;

                    if (col < 0) {
                        // obstacle on the left side of the path -> dodge right (D)
                        wantRight = true;
                    }
                    else if (col > 0) {
                        // obstacle on the right side of the path -> dodge left (A)
                        wantLeft = true;
                    }
                    else {
                        // obstacle dead ahead -> pick a passable side
                        int sx = (int) Math.round(rx), sz = (int) Math.round(rz);
                        BlockPos obs = BlockPos.ofFloored(cx, baseY, cz);
                        boolean rightFree = isSidePassable(obs, sx, sz, fx, fz);
                        boolean leftFree = isSidePassable(obs, -sx, -sz, fx, fz);
                        if (rightFree) wantRight = true;
                        else if (leftFree) wantLeft = true;
                    }

                    setStrafe(wantLeft, wantRight);
                    return;
                }
            }
        }

        setStrafe(false, false);
    }

    // The side block and the block diagonally ahead of it must both be passable.
    private boolean isSidePassable(BlockPos obs, int sx, int sz, double fx, double fz) {
        int fdx = (int) Math.round(fx), fdz = (int) Math.round(fz);
        return isPassable(obs.add(sx, 0, sz)) && isPassable(obs.add(sx + fdx, 0, sz + fdz));
    }

    private boolean isPassable(BlockPos pos) {
        return !isBlocking(pos) && !isBlocking(pos.up());
    }

    private boolean isBlocking(BlockPos pos) {
        return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    private void setStrafe(boolean left, boolean right) {
        if (left != strafeLeft) {
            mc.options.leftKey.setPressed(left);
            strafeLeft = left;
        }
        if (right != strafeRight) {
            mc.options.rightKey.setPressed(right);
            strafeRight = right;
        }
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
