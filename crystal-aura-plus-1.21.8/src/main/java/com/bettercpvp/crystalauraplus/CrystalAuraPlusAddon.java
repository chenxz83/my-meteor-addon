package com.bettercpvp.crystalauraplus;

import com.bettercpvp.crystalauraplus.hud.CrystalAuraInfoHud;
import com.bettercpvp.crystalauraplus.modules.AutoSignPlus;
import com.bettercpvp.crystalauraplus.modules.CrystalAuraPlus;
import com.bettercpvp.crystalauraplus.modules.CrystalESP;
import com.bettercpvp.crystalauraplus.modules.KillMsg;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CrystalAuraPlusAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final HudGroup HUD_GROUP = new HudGroup("Better CPVP");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Crystal Aura Plus (\u6c34\u6676\u5149\u73af+)");

        // Modules
        Modules.get().add(new CrystalAuraPlus());
        Modules.get().add(new CrystalESP());
        Modules.get().add(new KillMsg());
        Modules.get().add(new AutoSignPlus());

        // HUD
        Hud.get().register(CrystalAuraInfoHud.INFO);
    }

    @Override
    public String getPackage() {
        return "com.bettercpvp.crystalauraplus";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("chenxz83", "crystal-aura-plus");
    }

    @Override
    public String getCommit() {
        try (InputStream in = getClass().getResourceAsStream("/commit.txt")) {
            if (in == null) return null;

            String commit = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return commit.isEmpty() ? null : commit;
        } catch (IOException e) {
            return null;
        }
    }
}
