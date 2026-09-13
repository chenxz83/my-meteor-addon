package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Combat Log - logs kills, deaths, totem pops and joins/leaves to a file.
 * 战斗日志 - 把击杀、死亡、爆图腾、进服/出服记录到文件，方便复盘。
 */
public class CombatLog extends Module {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> logKills = sgGeneral.add(new BoolSetting.Builder()
        .name("log-kills")
        .description("Log your kills. 记录击杀。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logDeaths = sgGeneral.add(new BoolSetting.Builder()
        .name("log-deaths")
        .description("Log your deaths. 记录自己的死亡。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logPops = sgGeneral.add(new BoolSetting.Builder()
        .name("log-pops")
        .description("Log totem pops. 记录图腾弹出。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logJoins = sgGeneral.add(new BoolSetting.Builder()
        .name("log-joins")
        .description("Log joining and leaving servers. 记录进服/出服。")
        .defaultValue(false)
        .build()
    );

    private int ticks;
    private final Map<UUID, Integer> lastAttackTick = new HashMap<>();
    private final Set<UUID> announcedDeaths = new HashSet<>();
    private boolean selfDeathLogged;

    public CombatLog() {
        super(Categories.Misc, "combat-log", "Logs kills, deaths, totem pops and joins/leaves to a file. 把战斗记录写入文件。");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        lastAttackTick.clear();
        announcedDeaths.clear();
        selfDeathLogged = false;
    }

    private void write(String line) {
        File file = new File(MeteorClient.FOLDER, "combat-log.txt");
        try (Writer writer = new FileWriter(file, true)) {
            writer.write("[" + LocalDateTime.now().format(TIME) + "] " + line + System.lineSeparator());
        } catch (IOException e) {
            MeteorClient.LOG.error("Combat Log failed to write to file.", e);
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!logKills.get()) return;
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.meteor$getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            Entity entity = packet.meteor$getEntity();
            if (entity instanceof PlayerEntity player) lastAttackTick.put(player.getUuid(), ticks);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!logPops.get()) return;
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;

        Entity entity = p.getEntity(mc.world);
        if (entity instanceof PlayerEntity player) {
            write(player.getName().getString() + " popped a totem");
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (logJoins.get()) write("Joined server");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (logJoins.get()) write("Left server");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        // Self death
        if (logDeaths.get() && mc.player != null) {
            if (mc.player.deathTime > 0) {
                if (!selfDeathLogged) {
                    selfDeathLogged = true;
                    write("You died at " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ());
                }
            }
            else {
                selfDeathLogged = false;
            }
        }

        if (!logKills.get()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            if (player.deathTime > 0) {
                Integer atk = lastAttackTick.get(player.getUuid());
                if (atk != null && ticks - atk <= 60 && announcedDeaths.add(player.getUuid())) {
                    write("You killed " + player.getName().getString() + " at " + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ());
                }
            }
            else {
                announcedDeaths.remove(player.getUuid());
            }
        }
    }
}
