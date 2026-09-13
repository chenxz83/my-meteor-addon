package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Item ESP - highlights valuable dropped items through walls.
 * 物品高亮 - 穿墙高亮贵重掉落物（图腾、水晶、金苹果等）。
 */
public class ItemESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to highlight. 要高亮的物品。")
        .defaultValue(Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.NETHERITE_INGOT, Items.DIAMOND, Items.EXPERIENCE_BOTTLE)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range in which to render items. 渲染物品的最大距离。")
        .defaultValue(64)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgColors.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the box. 方框侧面颜色。")
        .defaultValue(new SettingColor(255, 255, 0, 60))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgColors.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the box. 方框线条颜色。")
        .defaultValue(new SettingColor(255, 255, 0))
        .build()
    );

    private int count;

    public ItemESP() {
        super(Categories.Render, "item-esp", "Highlights valuable dropped items through walls. 穿墙高亮贵重掉落物。");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;
            if (!items.get().contains(item.getStack().getItem())) continue;

            if (PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) > range.get() * range.get()) continue;

            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

            event.renderer.box(x - 0.15, y - 0.15, z - 0.15, x + 0.15, y + 0.15, z + 0.15, sideColor.get(), lineColor.get(), meteordevelopment.meteorclient.renderer.ShapeMode.Both, 0);

            count++;
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
