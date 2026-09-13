package com.bettercpvp.crystalauraplus;

import com.bettercpvp.crystalauraplus.hud.CrystalAuraInfoHud;
import com.bettercpvp.crystalauraplus.modules.AntiPiston;
import com.bettercpvp.crystalauraplus.modules.AutoPearl;
import com.bettercpvp.crystalauraplus.modules.AutoReply;
import com.bettercpvp.crystalauraplus.modules.AutoSignPlus;
import com.bettercpvp.crystalauraplus.modules.BackstabWarning;
import com.bettercpvp.crystalauraplus.modules.CombatLog;
import com.bettercpvp.crystalauraplus.modules.CrystalAuraPlus;
import com.bettercpvp.crystalauraplus.modules.CrystalESP;
import com.bettercpvp.crystalauraplus.modules.CrystalIncoming;
import com.bettercpvp.crystalauraplus.modules.DeathAnnouncer;
import com.bettercpvp.crystalauraplus.modules.ExplosionPreview;
import com.bettercpvp.crystalauraplus.modules.HighwayHelper;
import com.bettercpvp.crystalauraplus.modules.ItemESP;
import com.bettercpvp.crystalauraplus.modules.KillMsg;
import com.bettercpvp.crystalauraplus.modules.TotemAlert;
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
        Modules.get().add(new TotemAlert());
        Modules.get().add(new AutoPearl());
        Modules.get().add(new ItemESP());
        Modules.get().add(new BackstabWarning());
        Modules.get().add(new CrystalIncoming());
        Modules.get().add(new DeathAnnouncer());
        Modules.get().add(new CombatLog());
        Modules.get().add(new AutoReply());
        Modules.get().add(new HighwayHelper());
        Modules.get().add(new AntiPiston());
        Modules.get().add(new ExplosionPreview());

        // HUD
        Hud.get().register(CrystalAuraInfoHud.INFO);
    }

    @Override
    public String getPackage() {
        return "com.bettercpvp.crystalauraplus";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("chenxz83", "my-meteor-addon");
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
