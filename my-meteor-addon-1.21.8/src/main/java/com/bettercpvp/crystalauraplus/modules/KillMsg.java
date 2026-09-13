package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.value.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Kill Msg - sends a customizable chat message whenever you kill a player.
 * 击杀播报 - 每击杀一个玩家就在聊天中发送自定义消息（支持 StarScript）。
 *
 * StarScript variables: {victim} 被杀玩家名, {pops} 其爆的图腾数, {kills} 本次击杀数, {player} 你的名字
 */
public class KillMsg extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("The message to send on kill. Supports StarScript: {victim} 被杀玩家名, {pops} 爆的图腾数, {kills} 本次击杀数, {player} 你的名字.")
        .defaultValue("{victim} died after popping {pops} totems.")
        .build()
    );

    private int ticks;
    private int kills;

    private final Map<UUID, Integer> totemPops = new HashMap<>();
    private final Map<UUID, Integer> lastAttackTick = new HashMap<>();
    private final Set<Integer> ourCrystals = new HashSet<>();
    private final Map<BlockPos, Integer> pendingPlacements = new HashMap<>();
    private final List<Vec3d> explosionPos = new ArrayList<>();
    private final List<Integer> explosionTick = new ArrayList<>();
    private final Set<UUID> announced = new HashSet<>();

    private String victimName = "";
    private int victimPops = 0;

    public KillMsg() {
        super(Categories.Misc, "kill-msg", "Sends a customizable chat message whenever you kill a player. 每击杀一个玩家就在聊天中发送自定义消息。");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        kills = 0;

        totemPops.clear();
        lastAttackTick.clear();
        ourCrystals.clear();
        pendingPlacements.clear();
        explosionPos.clear();
        explosionTick.clear();
        announced.clear();

        // Register StarScript variables
        MeteorStarscript.ss.set("victim", () -> Value.string(victimName));
        MeteorStarscript.ss.set("pops", () -> Value.number(victimPops));
        MeteorStarscript.ss.set("kills", () -> Value.number(kills));
    }

    // Track totem pops
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;

        Entity entity = p.getEntity(mc.world);
        if (entity instanceof PlayerEntity player) {
            totemPops.put(player.getUuid(), totemPops.getOrDefault(player.getUuid(), 0) + 1);
        }
    }

    // Track our attacks and our crystal placements
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        // Melee attacks
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.meteor$getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            Entity entity = packet.meteor$getEntity();
            if (entity instanceof PlayerEntity player) lastAttackTick.put(player.getUuid(), ticks);
        }

        // Crystal placements
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet
            && (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL)) {
            pendingPlacements.put(packet.getBlockHitResult().getBlockPos(), ticks);
        }
    }

    // Mark our crystals
    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        Integer placedAt = pendingPlacements.remove(event.entity.getBlockPos());
        if (placedAt != null && ticks - placedAt <= 5) ourCrystals.add(event.entity.getId());
    }

    // Track our crystal explosions
    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity && ourCrystals.remove(event.entity.getId())) {
            explosionPos.add(event.entity.getPos());
            explosionTick.add(ticks);

            if (explosionPos.size() > 20) {
                explosionPos.remove(0);
                explosionTick.remove(0);
            }
        }
    }

    // Kill detection
    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        pendingPlacements.entrySet().removeIf(entry -> ticks - entry.getValue() > 5);

        for (PlayerEntity player : mc.world.getPlayers()) {
            UUID uuid = player.getUuid();

            if (player.deathTime > 0) {
                if (announced.add(uuid) && isOurKill(player)) {
                    sendKillMessage(player);
                }
            }
            else {
                announced.remove(uuid);
            }
        }
    }

    private boolean isOurKill(PlayerEntity player) {
        // Recently attacked by us
        Integer atk = lastAttackTick.get(player.getUuid());
        if (atk != null && ticks - atk <= 60) return true;

        // One of our crystals recently exploded near them
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        for (int i = explosionPos.size() - 1; i >= 0; i--) {
            if (ticks - explosionTick.get(i) > 40) break;
            if (explosionPos.get(i).squaredDistanceTo(px, py, pz) <= 64) return true;
        }

        return false;
    }

    private void sendKillMessage(PlayerEntity victim) {
        kills++;
        victimName = victim.getName().getString();
        victimPops = totemPops.getOrDefault(victim.getUuid(), 0);

        Script script = MeteorStarscript.compile(message.get());
        if (script == null) return;

        String text = MeteorStarscript.run(script);
        if (text == null || text.isEmpty()) return;

        mc.player.networkHandler.sendChatMessage(text);
    }
}
