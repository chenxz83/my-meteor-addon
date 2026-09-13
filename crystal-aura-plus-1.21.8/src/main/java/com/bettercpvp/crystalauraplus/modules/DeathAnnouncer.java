package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Death Announcer - announces in chat when any player dies, with their coordinates.
 * 死亡播报 - 任何玩家死亡都在聊天中播报（含死亡坐标），方便找尸体。
 */
public class DeathAnnouncer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("show-coords")
        .description("Show the death coordinates in the message. 在播报中显示死亡坐标。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Don't announce friends' deaths. 不播报好友的死亡。")
        .defaultValue(false)
        .build()
    );

    private final Set<UUID> announced = new HashSet<>();

    public DeathAnnouncer() {
        super(Categories.Misc, "death-announcer", "Announces in chat when any player dies, with coordinates. 玩家死亡时聊天播报（含坐标）。");
    }

    @Override
    public void onDeactivate() {
        announced.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            UUID uuid = player.getUuid();

            if (player.deathTime > 0) {
                if (!announced.add(uuid)) continue;
                if (player == mc.player) continue;
                if (ignoreFriends.get() && meteordevelopment.meteorclient.systems.friends.Friends.get().isFriend(player)) continue;

                if (showCoords.get()) {
                    info("%s 死亡了! 坐标: %d %d %d", player.getName().getString(), (int) player.getX(), (int) player.getY(), (int) player.getZ());
                }
                else {
                    info("%s 死亡了!", player.getName().getString());
                }
            }
            else {
                announced.remove(uuid);
            }
        }
    }
}
