package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Anti-Piston - detects and highlights piston bed/crystal traps around you.
 * 防活塞陷阱 - 检测并高亮附近的活塞床/活塞水晶陷阱。
 */
public class AntiPiston extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The range to scan for pistons. 扫描活塞的范围。")
        .defaultValue(8)
        .min(1)
        .sliderMax(16)
        .build()
    );

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder()
        .name("alert")
        .description("Alert in chat when a trap is found. 发现陷阱时在聊天中提醒。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render a box around detected traps. 渲染检测到的陷阱位置。")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the trap box. 陷阱方框侧面颜色。")
        .defaultValue(new SettingColor(255, 0, 0, 60))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the trap box. 陷阱方框线条颜色。")
        .defaultValue(new SettingColor(255, 0, 0))
        .visible(render::get)
        .build()
    );

    private final List<BlockPos> traps = new ArrayList<>();
    private int cooldown;

    public AntiPiston() {
        super(Categories.Combat, "anti-piston", "Detects and highlights piston bed/crystal traps around you. 检测附近的活塞陷阱。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (cooldown > 0) cooldown--;

        traps.clear();

        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get();

        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, r, r, r)) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.STICKY_PISTON)) continue;

            Direction facing = state.get(PistonBlock.FACING);
            BlockPos target = pos.offset(facing, 2);

            boolean danger = mc.world.getBlockState(target).getBlock() instanceof BedBlock || mc.world.getBlockState(target).isOf(Blocks.TNT);

            if (!danger) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity && entity.getBlockPos().equals(target)) {
                        danger = true;
                        break;
                    }
                }
            }

            if (!danger) continue;

            traps.add(pos.toImmutable());

            if (alert.get() && cooldown <= 0) {
                info("警告: 附近发现活塞陷阱! (%d %d %d)", pos.getX(), pos.getY(), pos.getZ());
                mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                cooldown = 100;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        for (BlockPos pos : traps) {
            event.renderer.box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
