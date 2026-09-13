package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/**
 * Crystal Incoming - alerts you when an enemy crystal near you could damage you.
 * 水晶来袭预警 - 附近有会伤到你的敌方水晶时提前警报。
 */
public class CrystalIncoming extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to check for crystals. 检测水晶的范围。")
        .defaultValue(8)
        .min(1)
        .sliderMax(16)
        .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("Alert only if the crystal would deal at least this damage to you. 水晶对你造成的最低伤害阈值。")
        .defaultValue(4)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> alertInterval = sgGeneral.add(new IntSetting.Builder()
        .name("alert-interval")
        .description("Seconds between repeated alerts. 重复提醒的间隔（秒）。")
        .defaultValue(3)
        .min(1)
        .sliderMax(30)
        .build()
    );

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a sound when alerting. 提醒时播放提示音。")
        .defaultValue(true)
        .build()
    );

    private final BlockPos.Mutable obsidianPos = new BlockPos.Mutable();
    private int cooldown;

    public CrystalIncoming() {
        super(Categories.Combat, "crystal-incoming", "Alerts you when an enemy crystal near you could damage you. 附近有会伤到你的水晶时警报。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (entity.squaredDistanceTo(mc.player) > range.get() * range.get()) continue;

            obsidianPos.set(entity.getBlockPos()).move(0, -1, 0);
            float damage = DamageUtils.crystalDamage(mc.player, entity.getEntityPos(), false, obsidianPos);

            if (damage >= minDamage.get()) {
                info("警告: 附近有水晶会对你造成 %.1f 伤害!", damage);
                if (sound.get()) {
                    mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
                cooldown = alertInterval.get() * 20;
                return;
            }
        }
    }
}
