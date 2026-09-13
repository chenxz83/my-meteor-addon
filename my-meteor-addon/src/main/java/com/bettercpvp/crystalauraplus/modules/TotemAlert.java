package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Totem Alert - warns you when your totems or crystals are running low.
 * 图腾警报 - 图腾或水晶快用完时提醒你。
 */
public class TotemAlert extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> totemThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("totem-threshold")
        .description("Warn when you have this many totems or less. 图腾数量低于此值时提醒。")
        .defaultValue(3)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> crystalThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("crystal-threshold")
        .description("Warn when you have this many crystals or less. 水晶数量低于此值时提醒。")
        .defaultValue(16)
        .min(0)
        .sliderMax(64)
        .build()
    );

    private final Setting<Integer> alertInterval = sgGeneral.add(new IntSetting.Builder()
        .name("alert-interval")
        .description("Seconds between repeated alerts. 重复提醒的间隔（秒）。")
        .defaultValue(30)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a sound when alerting. 提醒时播放提示音。")
        .defaultValue(true)
        .build()
    );

    private int cooldown;

    public TotemAlert() {
        super(Categories.Misc, "totem-alert", "Warns you when your totems or crystals are running low. 图腾或水晶快用完时提醒。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int totems = InvUtils.find(Items.TOTEM_OF_UNDYING).count();
        int crystals = InvUtils.find(Items.END_CRYSTAL).count();

        boolean lowTotems = totemThreshold.get() > 0 && totems <= totemThreshold.get();
        boolean lowCrystals = crystalThreshold.get() > 0 && crystals <= crystalThreshold.get();

        if (!lowTotems && !lowCrystals) return;

        StringBuilder sb = new StringBuilder();
        if (lowTotems) sb.append("图腾仅剩 ").append(totems).append(" 个! ");
        if (lowCrystals) sb.append("水晶仅剩 ").append(crystals).append(" 个!");

        info(sb.toString().trim());
        if (sound.get()) {
            mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        cooldown = alertInterval.get() * 20;
    }
}
