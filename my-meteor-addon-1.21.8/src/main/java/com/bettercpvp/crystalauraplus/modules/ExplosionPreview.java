package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Explosion Preview - renders the explosion range of crystals, beds, anchors and TNT around you.
 * 爆炸范围预览 - 渲染附近水晶/床/重生锚/TNT 的爆炸伤害范围。
 */
public class ExplosionPreview extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> crystals = sgGeneral.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Render end crystal explosion ranges. 渲染末地水晶爆炸范围。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Render bed/anchor/TNT explosion ranges. 渲染床/重生锚/TNT 爆炸范围。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range in which to render explosions. 渲染爆炸范围的最大距离。")
        .defaultValue(32)
        .min(1)
        .sliderMax(64)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgColors.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the explosion box. 爆炸范围侧面颜色。")
        .defaultValue(new SettingColor(255, 100, 0, 30))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgColors.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the explosion box. 爆炸范围线条颜色。")
        .defaultValue(new SettingColor(255, 100, 0))
        .build()
    );

    public ExplosionPreview() {
        super(Categories.Render, "explosion-preview", "Renders the explosion range of crystals, beds, anchors and TNT. 渲染附近爆炸物伤害范围。");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        double rangeSq = range.get() * range.get();

        if (crystals.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) continue;
                if (PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) > rangeSq) continue;

                drawExplosion(event, entity.getX() + 0.5, entity.getY() + 1, entity.getZ() + 0.5, 6);
            }
        }

        if (blocks.get()) {
            BlockPos playerPos = mc.player.getBlockPos();
            int r = (int) Math.min(range.get(), 8);

            for (BlockPos pos : BlockPos.iterateOutwards(playerPos, r, r, r)) {
                BlockState state = mc.world.getBlockState(pos);

                double radius;
                if (state.getBlock() instanceof BedBlock) radius = 5;
                else if (state.isOf(Blocks.RESPAWN_ANCHOR)) radius = 5;
                else if (state.isOf(Blocks.TNT)) radius = 4;
                else continue;

                if (PlayerUtils.squaredDistanceToCamera(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) continue;

                drawExplosion(event, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
            }
        }
    }

    private void drawExplosion(Render3DEvent event, double x, double y, double z, double radius) {
        event.renderer.box(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    }
}
