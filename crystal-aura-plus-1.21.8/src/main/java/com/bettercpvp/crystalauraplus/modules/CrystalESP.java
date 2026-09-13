package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * Crystal ESP - renders a box around end crystals through walls.
 * 水晶 ESP - 高亮显示末地水晶（穿墙可见），参考 Meteor Client 的 ESP 模块。
 */
public class CrystalESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range in which to render crystals. 渲染水晶的最大距离。")
        .defaultValue(64)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered. 形状渲染方式。")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Double> fillOpacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill. 填充透明度。")
        .visible(() -> shapeMode.get() != ShapeMode.Lines)
        .defaultValue(0.3)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgColors.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the box. 方框侧面颜色。")
        .defaultValue(new SettingColor(255, 0, 255, 45))
        .visible(() -> shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgColors.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the box. 方框线条颜色。")
        .defaultValue(new SettingColor(255, 0, 255))
        .visible(() -> shapeMode.get().lines())
        .build()
    );

    private int count;

    public CrystalESP() {
        super(Categories.Render, "crystal-esp", "Renders a box around end crystals through walls. 高亮显示末地水晶（穿墙可见）。");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;

            // Range check
            if (PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) > range.get() * range.get()) continue;

            // Interpolate position for smooth rendering
            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

            Box box = entity.getBoundingBox();
            event.renderer.box(
                x + box.minX, y + box.minY, z + box.minZ,
                x + box.maxX, y + box.maxY, z + box.maxZ,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0
            );

            count++;
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
