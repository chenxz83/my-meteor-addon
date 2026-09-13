package com.bettercpvp.crystalauraplus.hud;

import com.bettercpvp.crystalauraplus.CrystalAuraPlusAddon;
import com.bettercpvp.crystalauraplus.modules.CrystalAuraPlus;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal Aura Plus status HUD.
 * 水晶光环+ 状态 HUD：显示水晶数量、当前目标以及击杀/爆图腾统计。
 */
public class CrystalAuraInfoHud extends HudElement {
    public static final HudElementInfo<CrystalAuraInfoHud> INFO = new HudElementInfo<>(
        CrystalAuraPlusAddon.HUD_GROUP,
        "crystal-aura-info",
        "水晶光环+ 状态",
        "显示水晶数量、当前目标与击杀/爆图腾统计。",
        CrystalAuraInfoHud::new
    );

    public CrystalAuraInfoHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        List<String> lines = new ArrayList<>();
        CrystalAuraPlus ca = Modules.get().get(CrystalAuraPlus.class);

        lines.add("水晶光环+");
        lines.add("水晶: " + InvUtils.find(Items.END_CRYSTAL).count());

        if (ca != null && ca.isActive()) {
            String info = ca.getInfoString();
            if (info != null && !info.isEmpty()) lines.add("目标: " + info);
            lines.add("击杀: " + ca.kills + " | 爆图腾: " + ca.totemPops);
        }

        double width = 0;
        for (String line : lines) width = Math.max(width, renderer.textWidth(line, true));

        double height = lines.size() * renderer.textHeight(true);
        setSize(width, height);

        double y = this.y;
        for (String line : lines) {
            renderer.text(line, x, y, Color.WHITE, true);
            y += renderer.textHeight(true);
        }
    }
}
