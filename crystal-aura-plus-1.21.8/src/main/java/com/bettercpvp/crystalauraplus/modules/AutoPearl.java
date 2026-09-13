package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Auto Pearl - automatically throws an ender pearl when your health is low or you are falling.
 * 自动珍珠 - 血量过低或坠落时自动扔末影珍珠逃生。
 */
public class AutoPearl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
        .name("health")
        .description("Throw a pearl when your health is below this value. Set to 0 to disable. 血量低于此值时扔珍珠（0=关闭）。")
        .defaultValue(8)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> falling = sgGeneral.add(new BoolSetting.Builder()
        .name("on-fall")
        .description("Throw a pearl when you are falling far. 坠落时自动扔珍珠。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("Throw a pearl after falling this far. 坠落超过此距离后扔珍珠。")
        .defaultValue(8)
        .min(3)
        .sliderMax(50)
        .visible(falling::get)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between pearl throws. 两次扔珍珠的最小间隔（tick）。")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build()
    );

    private int timer;

    public AutoPearl() {
        super(Categories.Combat, "auto-pearl", "Automatically throws an ender pearl when your health is low or you are falling. 血量过低或坠落时自动扔珍珠。");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer > 0) {
            timer--;
            return;
        }

        boolean shouldThrow = (health.get() > 0 && mc.player.getHealth() <= health.get())
            || (falling.get() && mc.player.fallDistance > fallDistance.get());

        if (!shouldThrow) return;

        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        Hand hand = pearl.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (!pearl.isOffhand()) InvUtils.swap(pearl.slot(), false);

        mc.interactionManager.interactItem(mc.player, hand);

        if (!pearl.isOffhand()) InvUtils.swap(prevSlot, false);

        timer = delay.get();
    }
}
