package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Backstab Warning - alerts you when a player is sneaking up behind you.
 * 背刺警报 - 有玩家从背后快速接近时提醒你。
 */
public class BackstabWarning extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to check players. 检测玩家的范围。")
        .defaultValue(12)
        .min(1)
        .sliderMax(32)
        .build()
    );

    private final Setting<Integer> alertInterval = sgGeneral.add(new IntSetting.Builder()
        .name("alert-interval")
        .description("Seconds between repeated alerts. 重复提醒的间隔（秒）。")
        .defaultValue(5)
        .min(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a sound when alerting. 提醒时播放提示音。")
        .defaultValue(true)
        .build()
    );

    private final Map<UUID, Double> lastDistances = new HashMap<>();
    private int cooldown;

    public BackstabWarning() {
        super(Categories.Combat, "backstab-warning", "Alerts you when a player is sneaking up behind you. 有玩家从背后快速接近时提醒。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (cooldown > 0) cooldown--;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isDead()) continue;
            if (Friends.get().isFriend(player)) continue;

            double dist = player.squaredDistanceTo(mc.player);
            if (dist > range.get() * range.get()) continue;

            // Behind check: the angle between our look direction and the direction to the player
            double yawTo = Rotations.getYaw(player);
            double diff = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - yawTo));
            if (diff < 135) continue;

            // Approaching check: they got significantly closer than last tick
            Double prev = lastDistances.get(player.getUuid());
            lastDistances.put(player.getUuid(), dist);

            if (prev != null && prev - dist > 4 && cooldown <= 0) {
                info("警告: %s 正从背后接近你!", player.getName().getString());
                if (sound.get()) {
                    mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
                cooldown = alertInterval.get() * 20;
            }
        }
    }
}
