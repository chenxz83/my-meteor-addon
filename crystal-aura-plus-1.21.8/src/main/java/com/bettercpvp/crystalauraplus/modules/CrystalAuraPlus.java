/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.bettercpvp.crystalauraplus.modules;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CrystalAuraPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgTarget = settings.createGroup("Targeting");
    private final SettingGroup sgObsidian = settings.createGroup("Obsidian");
    private final SettingGroup sgNotify = settings.createGroup("Notify");

    private final Setting<TargetMode> targetMode = sgTarget.add(new EnumSetting.Builder<TargetMode>()
        .name("target-mode")
        .description("Whether to damage all targets or focus a single target. 伤害所有目标还是只聚焦单个目标。")
        .defaultValue(TargetMode.Multi)
        .build()
    );

    private final Setting<TargetPriority> targetPriority = sgTarget.add(new EnumSetting.Builder<TargetPriority>()
        .name("target-priority")
        .description("Which target to prioritize in single-target mode. 单目标模式下的目标优先级。")
        .defaultValue(TargetPriority.Nearest)
        .visible(() -> targetMode.get() == TargetMode.Single)
        .build()
    );

    private final Setting<Boolean> showDamage = sgTarget.add(new BoolSetting.Builder()
        .name("show-damage")
        .description("Shows the predicted damage to the best target in the HUD. HUD 显示对最佳目标的预计伤害。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showHealth = sgTarget.add(new BoolSetting.Builder()
        .name("show-health")
        .description("Shows the best target's health in the HUD. HUD 显示最佳目标血量。")
        .defaultValue(false)
        .build()
    );

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range in which to target players. 目标选择范围。")
        .defaultValue(10)
        .min(0)
        .sliderMax(16)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predicts target movement. 预判目标移动。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("Minimum damage the crystal needs to deal to your target. 对目标造成伤害的最低要求。")
        .defaultValue(6)
        .min(0)
        .build()
    );

    private final Setting<Boolean> ignoreSelfDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self-damage")
        .description("Ignores all self damage restrictions (anti-suicide and max-damage). 忽略所有自伤限制（防自杀与最大自伤）。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-damage")
        .description("Maximum damage crystals can deal to yourself. 水晶对自己造成的最大伤害上限。")
        .defaultValue(6)
        .range(0, 36)
        .sliderMax(36)
        .visible(() -> !ignoreSelfDamage.get())
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break crystals if they will kill you. 水晶会炸死自己时停止放置与破坏。")
        .defaultValue(true)
        .visible(() -> !ignoreSelfDamage.get())
        .build()
    );

    private final Setting<Boolean> ignoreNakeds = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-nakeds")
        .description("Ignore players with no items. 忽略空手玩家。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Won't target players on your friends list. 不会攻击好友列表中的玩家。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> activationKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("activation-key")
        .description("Hold this key to attack. 按住此键才攻击，松开即停；不设置则始终攻击。")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Keybind> aggressiveKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("aggressive-key")
        .description("Press to toggle aggressive mode. 按下切换狂暴模式（无视自伤、强制脸贴、降低最低伤害）。")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side towards the crystals being hit/placed. 服务器端转向水晶。")
        .defaultValue(true)
        .build()
    );

    private final Setting<YawStepMode> yawStepMode = sgGeneral.add(new EnumSetting.Builder<YawStepMode>()
        .name("yaw-steps-mode")
        .description("When to run the yaw steps check. 偏航步进检查时机。")
        .defaultValue(YawStepMode.Break)
        .visible(rotate::get)
        .build()
    );

    private final Setting<Double> yawSteps = sgGeneral.add(new DoubleSetting.Builder()
        .name("yaw-steps")
        .description("Maximum number of degrees its allowed to rotate in one tick. 每 tick 最大旋转角度。")
        .defaultValue(180)
        .range(1, 180)
        .visible(rotate::get)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack. 攻击的实体类型。")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER, EntityType.WARDEN, EntityType.WITHER)
        .build()
    );

    // Switch

    private final Setting<AutoSwitchMode> autoSwitch = sgSwitch.add(new EnumSetting.Builder<AutoSwitchMode>()
        .name("auto-switch")
        .description("Switches to crystals in your hotbar once a target is found. 发现目标后自动切换到水晶。")
        .defaultValue(AutoSwitchMode.Normal)
        .build()
    );

    private final Setting<Integer> switchDelay = sgSwitch.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("The delay in ticks to wait to break a crystal after switching hotbar slot. 切换槽位后破坏水晶的延迟。")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Boolean> noGapSwitch = sgSwitch.add(new BoolSetting.Builder()
        .name("no-gap-switch")
        .description("Won't auto switch if you're holding a gapple. 手持金苹果时不自动切换。")
        .defaultValue(true)
        .visible(() -> autoSwitch.get() == AutoSwitchMode.Normal)
        .build()
    );

    private final Setting<Boolean> noBowSwitch = sgSwitch.add(new BoolSetting.Builder()
        .name("no-bow-switch")
        .description("Won't auto switch if you're holding a bow. 手持弓时不自动切换。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiWeakness = sgSwitch.add(new BoolSetting.Builder()
        .name("anti-weakness")
        .description("Switches to tools with so you can break crystals with the weakness effect. 有虚弱效果时切换工具以便破坏水晶。")
        .defaultValue(true)
        .build()
    );

    // Place

    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("If the CA should place crystals. 是否放置水晶。")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay in ticks to wait to place a crystal after it's exploded. 爆炸后放置水晶的延迟。")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Range in which to place crystals. 放置水晶的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to place crystals when behind blocks. 隔墙放置水晶的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder()
        .name("1.12-placement")
        .description("Uses 1.12 crystal placement. 使用 1.12 水晶放置方式。")
        .defaultValue(false)
        .build()
    );

    private final Setting<SupportMode> support = sgPlace.add(new EnumSetting.Builder<SupportMode>()
        .name("support")
        .description("Places a support block in air if no other position have been found. 空中无放置位置时垫方块。")
        .defaultValue(SupportMode.Disabled)
        .build()
    );

    private final Setting<Integer> supportDelay = sgPlace.add(new IntSetting.Builder()
        .name("support-delay")
        .description("Delay in ticks after placing support block. 垫方块后的延迟。")
        .defaultValue(1)
        .min(0)
        .visible(() -> support.get() != SupportMode.Disabled)
        .build()
    );

    private final Setting<Boolean> smartPlace = sgPlace.add(new BoolSetting.Builder()
        .name("smart-place")
        .description("Only places crystals when the target can receive damage (skips targets with hit invulnerability). 智能放置：跳过无敌帧目标。")
        .defaultValue(false)
        .build()
    );

    // Obsidian (auto place obsidian support blocks)

    private final Setting<Boolean> autoPlaceObsidian = sgObsidian.add(new BoolSetting.Builder()
        .name("auto-place-obsidian")
        .description("Automatically places obsidian to support crystals placed in the air. 自动放置黑曜石支撑空中水晶。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> obsidianRange = sgObsidian.add(new DoubleSetting.Builder()
        .name("obsidian-range")
        .description("The range in which to place obsidian support blocks. 放置黑曜石支撑块的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .visible(autoPlaceObsidian::get)
        .build()
    );

    private final Setting<Integer> obsidianDelay = sgObsidian.add(new IntSetting.Builder()
        .name("obsidian-delay")
        .description("Delay in ticks after placing an obsidian support block. 放置黑曜石后的延迟。")
        .defaultValue(1)
        .min(0)
        .sliderMax(20)
        .visible(autoPlaceObsidian::get)
        .build()
    );

    private final Setting<Boolean> obsidianSwapBack = sgObsidian.add(new BoolSetting.Builder()
        .name("obsidian-swap-back")
        .description("Swaps back to your previous hotbar slot after placing obsidian. 放完黑曜石切回原快捷栏槽位。")
        .defaultValue(true)
        .visible(autoPlaceObsidian::get)
        .build()
    );

    // Face place

    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-place")
        .description("Will face-place when target is below a certain health or armor durability threshold. 目标血量或护甲低于阈值时脸贴。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiBurrow = sgFacePlace.add(new BoolSetting.Builder()
        .name("anti-burrow")
        .description("Automatically face-places when the target is burrowed. 目标蹲坑（头顶有方块）时自动脸贴攻击。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder()
        .name("face-place-health")
        .description("The health the target has to be at to start face placing. 开始脸贴的目标血量阈值。")
        .defaultValue(8)
        .min(1)
        .sliderMin(1)
        .sliderMax(36)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder()
        .name("face-place-durability")
        .description("The durability threshold percentage to be able to face-place. 脸贴的护甲耐久百分比阈值。")
        .defaultValue(2)
        .min(1)
        .sliderMin(1)
        .sliderMax(100)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-place-missing-armor")
        .description("Automatically starts face placing when a target misses a piece of armor. 目标缺少护甲时自动脸贴。")
        .defaultValue(false)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder()
        .name("force-face-place")
        .description("Starts face place when this button is pressed. 按住此键强制脸贴。")
        .defaultValue(Keybind.none())
        .build()
    );

    // Break

    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
        .name("break")
        .description("If the CA should break crystals. 是否破坏水晶。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The delay in ticks to wait to break a crystal after it's placed. 放置后破坏水晶的延迟。")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> smartDelay = sgBreak.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Only breaks crystals when the target can receive damage. 仅当目标可受伤时才破坏水晶。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range in which to break crystals. 破坏水晶的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to break crystals when behind blocks. 隔墙破坏水晶的范围。")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> onlyBreakOwn = sgBreak.add(new BoolSetting.Builder()
        .name("only-own")
        .description("Only breaks own crystals. 只破坏自己放置的水晶。")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder()
        .name("break-attempts")
        .description("How many times to hit a crystal before stopping to target it. 单颗水晶的最大攻击次数。")
        .defaultValue(2)
        .sliderMin(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Integer> ticksExisted = sgBreak.add(new IntSetting.Builder()
        .name("ticks-existed")
        .description("Amount of ticks a crystal needs to have lived for it to be attacked by CrystalAura. 水晶存在多少 tick 后才攻击。")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Integer> attackFrequency = sgBreak.add(new IntSetting.Builder()
        .name("attack-frequency")
        .description("Maximum hits to do per second. 每秒最大攻击次数。")
        .defaultValue(25)
        .min(1)
        .sliderRange(1, 30)
        .build()
    );

    private final Setting<Boolean> fastBreak = sgBreak.add(new BoolSetting.Builder()
        .name("fast-break")
        .description("Ignores break delay and tries to break the crystal as soon as it's spawned in the world. 快速破坏：无视延迟，水晶生成即打。")
        .defaultValue(true)
        .build()
    );

    // Pause

    public final Setting<PauseMode> pauseOnUse = sgPause.add(new EnumSetting.Builder<PauseMode>()
        .name("pause-on-use")
        .description("Which processes should be paused while using an item. 使用物品时暂停哪些操作。")
        .defaultValue(PauseMode.Place)
        .build()
    );

    public final Setting<PauseMode> pauseOnMine = sgPause.add(new EnumSetting.Builder<PauseMode>()
        .name("pause-on-mine")
        .description("Which processes should be paused while mining a block. 挖掘方块时暂停哪些操作。")
        .defaultValue(PauseMode.None)
        .build()
    );

    private final Setting<Boolean> pauseOnLag = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Whether to pause if the server is not responding. 服务器卡顿时是否暂停。")
        .defaultValue(true)
        .build()
    );

    public final Setting<List<Module>> pauseModules = sgPause.add(new ModuleListSetting.Builder()
        .name("pause-modules")
        .description("Pauses while any of the selected modules are active. 所选模块激活时暂停。")
        .defaultValue(BedAura.class)
        .build()
    );

    public final Setting<Double> pauseHealth = sgPause.add(new DoubleSetting.Builder()
        .name("pause-health")
        .description("Pauses when you go below a certain health. 血量低于此值时暂停。")
        .defaultValue(5)
        .range(0,36)
        .sliderRange(0,36)
        .build()
    );

    private final Setting<Boolean> selfPopPause = sgPause.add(new BoolSetting.Builder()
        .name("self-pop-pause")
        .description("Pauses the module briefly after you pop a totem. 自己爆图腾后短暂停手。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> selfPopDelay = sgPause.add(new IntSetting.Builder()
        .name("self-pop-delay")
        .description("How many ticks to pause after popping a totem. 爆图腾后停手的 tick 数。")
        .defaultValue(10)
        .min(0)
        .sliderMax(40)
        .visible(selfPopPause::get)
        .build()
    );

    // Render

    public final Setting<SwingMode> swingMode = sgRender.add(new EnumSetting.Builder<SwingMode>()
        .name("swing-mode")
        .description("How to swing when placing. 放置时的挥手方式。")
        .defaultValue(SwingMode.Both)
        .build()
    );

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("The mode to render in. 渲染模式。")
        .defaultValue(RenderMode.Normal)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-place")
        .description("Renders a block overlay over the block the crystals are being placed on. 渲染水晶放置位置。")
        .defaultValue(true)
        .visible(() -> renderMode.get() == RenderMode.Normal)
        .build()
    );

    private final Setting<Integer> placeRenderTime = sgRender.add(new IntSetting.Builder()
        .name("place-time")
        .description("How long to render placements. 放置渲染时长。")
        .defaultValue(10)
        .min(0)
        .sliderMax(20)
        .visible(() -> renderMode.get() == RenderMode.Normal && renderPlace.get())
        .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
        .name("render-break")
        .description("Renders a block overlay over the block the crystals are broken on. 渲染水晶破坏位置。")
        .defaultValue(false)
        .visible(() -> renderMode.get() == RenderMode.Normal)
        .build()
    );

    private final Setting<Integer> breakRenderTime = sgRender.add(new IntSetting.Builder()
        .name("break-time")
        .description("How long to render breaking for. 破坏渲染时长。")
        .defaultValue(13)
        .min(0)
        .sliderMax(20)
        .visible(() -> renderMode.get() == RenderMode.Normal && renderBreak.get())
        .build()
    );

    private final Setting<Integer> smoothness = sgRender.add(new IntSetting.Builder()
        .name("smoothness")
        .description("How smoothly the render should move around. 渲染平滑度。")
        .defaultValue(10)
        .min(0)
        .sliderMax(20)
        .visible(() -> renderMode.get() == RenderMode.Smooth)
        .build()
    );

    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder()
        .name("height")
        .description("How tall the gradient should be. 渐变高度。")
        .defaultValue(0.7)
        .min(0)
        .sliderMax(1)
        .visible(() -> renderMode.get() == RenderMode.Gradient)
        .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
        .name("render-time")
        .description("How long to render placements. 渲染时长。")
        .defaultValue(10)
        .min(0)
        .sliderMax(20)
        .visible(() -> renderMode.get() == RenderMode.Smooth || renderMode.get() == RenderMode.Fading)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered. 形状渲染方式。")
        .defaultValue(ShapeMode.Both)
        .visible(() -> renderMode.get() != RenderMode.None)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the block overlay. 方框侧面颜色。")
        .defaultValue(new SettingColor(255, 255, 255, 45))
        .visible(() -> shapeMode.get().sides() && renderMode.get() != RenderMode.None)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the block overlay. 方框线条颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> shapeMode.get().lines() && renderMode.get() != RenderMode.None)
        .build()
    );

    private final Setting<Boolean> renderDamageText = sgRender.add(new BoolSetting.Builder()
        .name("damage")
        .description("Renders crystal damage text in the block overlay. 渲染水晶伤害文字。")
        .defaultValue(true)
        .visible(() -> renderMode.get() != RenderMode.None)
        .build()
    );

    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder()
        .name("damage-color")
        .description("The color of the damage text. 伤害文字颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> renderMode.get() != RenderMode.None && renderDamageText.get())
        .build()
    );

    private final Setting<Double> damageTextScale = sgRender.add(new DoubleSetting.Builder()
        .name("damage-scale")
        .description("How big the damage text should be. 伤害文字大小。")
        .defaultValue(1.25)
        .min(1)
        .sliderMax(4)
        .visible(() -> renderMode.get() != RenderMode.None && renderDamageText.get())
        .build()
    );

    private final Setting<Boolean> renderTargetInfo = sgRender.add(new BoolSetting.Builder()
        .name("render-target-info")
        .description("Renders the target's name, health and predicted damage above their head. 在目标头顶渲染名字、血量和预计伤害。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> targetInfoScale = sgRender.add(new DoubleSetting.Builder()
        .name("target-info-scale")
        .description("How big the target info text should be. 目标信息文字的大小。")
        .defaultValue(1.25)
        .min(1)
        .sliderMax(4)
        .visible(renderTargetInfo::get)
        .build()
    );

    private final Setting<SettingColor> targetInfoColor = sgRender.add(new ColorSetting.Builder()
        .name("target-info-color")
        .description("The color of the target info text. 目标信息文字的颜色。")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(renderTargetInfo::get)
        .build()
    );

    // Notify

    private final Setting<Boolean> killNotify = sgNotify.add(new BoolSetting.Builder()
        .name("kill-notify")
        .description("Sends a chat message when you kill a target. 击杀目标时在聊天栏提示。")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> popNotify = sgNotify.add(new BoolSetting.Builder()
        .name("pop-notify")
        .description("Sends a chat message when a target pops a totem. 目标爆图腾时在聊天栏提示。")
        .defaultValue(true)
        .build()
    );

    // Fields

    private Item mainItem, offItem;

    private int breakTimer, placeTimer, switchTimer, ticksPassed;
    private final List<LivingEntity> targets = new ArrayList<>();

    private final Vec3d vec3d = new Vec3d(0, 0, 0);
    private final Vec3d playerEyePos = new Vec3d(0, 0, 0);
    private final Vector3d vec3 = new Vector3d();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final Box box = new Box(0, 0, 0, 0, 0, 0);

    private final Vec3d vec3dRayTraceEnd = new Vec3d(0, 0, 0);
    private RaycastContext raycastContext;

    private final IntSet placedCrystals = new IntOpenHashSet();
    private boolean placing;
    private int placingTimer;
    public int kaTimer;
    private final BlockPos.Mutable placingCrystalBlockPos = new BlockPos.Mutable();

    private final IntSet removed = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();
    private final Int2IntMap waitingToExplode = new Int2IntOpenHashMap();
    private int attacks;

    private double serverYaw;

    private LivingEntity bestTarget;
    private double bestTargetDamage;
    private int bestTargetTimer;

    private boolean didRotateThisTick;
    private boolean isLastRotationPos;
    private final Vec3d lastRotationPos = new Vec3d(0, 0 ,0);
    private double lastYaw, lastPitch;
    private int lastRotationTimer;

    private int placeRenderTimer, breakRenderTimer;
    private final BlockPos.Mutable placeRenderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable breakRenderPos = new BlockPos.Mutable();
    private Box renderBoxOne, renderBoxTwo;

    private double renderDamage;

    private boolean aggressiveMode;
    private boolean aggressiveKeyWasPressed;
    private int selfPopTimer;
    private final Map<UUID, Integer> recentTargetTicks = new HashMap<>();
    private final Set<UUID> notifiedDeaths = new HashSet<>();

    // Session stats (read by the HUD element)
    public int kills;
    public int totemPops;

    public CrystalAuraPlus() {
        super(Categories.Combat, "\u6c34\u6676\u5149\u73af+", "\u589e\u5f3a\u7248\u6c34\u6676\u5149\u73af\uff0c\u57fa\u4e8e Meteor Client \u7684\u6c34\u6676\u5149\u73af (Crystal Aura) \u5f3a\u5316\u800c\u6765\uff0c\u65b0\u589e\u5355\u76ee\u6807\u805a\u7126\u3001\u76ee\u6807\u4f18\u5148\u7ea7\u3001\u667a\u80fd\u653e\u7f6e\u4e0e\u76ee\u6807\u4fe1\u606f\u663e\u793a\u3002", "crystal-aura-plus", "ca-plus", "ca+");
    }

    @Override
    public void onActivate() {
        breakTimer = 0;
        placeTimer = 0;
        ticksPassed = 0;

        raycastContext = new RaycastContext(new Vec3d(0, 0, 0), new Vec3d(0, 0, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        placing = false;
        placingTimer = 0;
        kaTimer = 0;

        attacks = 0;

        serverYaw = mc.player.getYaw();

        bestTargetDamage = 0;
        bestTargetTimer = 0;

        lastRotationTimer = getLastRotationStopDelay();

        placeRenderTimer = 0;
        breakRenderTimer = 0;

        aggressiveMode = false;
        aggressiveKeyWasPressed = false;
        selfPopTimer = 0;
        recentTargetTicks.clear();
        notifiedDeaths.clear();
    }

    @Override
    public void onDeactivate() {
        targets.clear();

        placedCrystals.clear();

        attemptedBreaks.clear();
        waitingToExplode.clear();

        removed.clear();

        bestTarget = null;

        aggressiveMode = false;
        aggressiveKeyWasPressed = false;
        selfPopTimer = 0;
        recentTargetTicks.clear();
        notifiedDeaths.clear();

        kills = 0;
        totemPops = 0;
    }

    private int getLastRotationStopDelay() {
        return Math.max(10, placeDelay.get() / 2 + breakDelay.get() / 2 + 10);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        // Update last rotation
        didRotateThisTick = false;
        lastRotationTimer++;

        // Aggressive mode toggle
        boolean aggressivePressed = aggressiveKey.get().isPressed();
        if (aggressivePressed && !aggressiveKeyWasPressed) {
            aggressiveMode = !aggressiveMode;
            info(aggressiveMode ? "\u72c2\u66b4\u6a21\u5f0f\u5df2\u5f00\u542f" : "\u72c2\u66b4\u6a21\u5f0f\u5df2\u5173\u95ed");
        }
        aggressiveKeyWasPressed = aggressivePressed;

        // Self pop pause timer
        if (selfPopTimer > 0) selfPopTimer--;

        // Decrement placing timer
        if (placing) {
            if (placingTimer > 0) placingTimer--;
            else placing = false;
        }

        if (kaTimer > 0) kaTimer--;

        if (ticksPassed < 20) ticksPassed++;
        else {
            ticksPassed = 0;
            attacks = 0;
        }

        // Decrement best target timer
        if (bestTargetTimer > 0) bestTargetTimer--;
        bestTargetDamage = 0;

        // Decrement break, place and switch timers
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (switchTimer > 0) switchTimer--;

        // Decrement render timers
        if (placeRenderTimer > 0) placeRenderTimer--;
        if (breakRenderTimer > 0) breakRenderTimer--;

        mainItem = mc.player.getMainHandStack().getItem();
        offItem = mc.player.getOffHandStack().getItem();

        // Update waiting to explode crystals and mark them as existing if reached threshold
        for (IntIterator it = waitingToExplode.keySet().iterator(); it.hasNext();) {
            int id = it.nextInt();
            int ticks = waitingToExplode.get(id);

            if (ticks > 3) {
                it.remove();
                removed.remove(id);
            }
            else {
                waitingToExplode.put(id, ticks + 1);
            }
        }

        // Set player eye pos
        ((IVec3d) playerEyePos).meteor$set(mc.player.getPos().x, mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getPos().z);

        // Find targets, break and place
        findTargets();

        // Track recently targeted players for kill notifications
        for (LivingEntity target : targets) recentTargetTicks.put(target.getUuid(), 0);
        recentTargetTicks.replaceAll((uuid, ticks) -> ticks + 1);
        recentTargetTicks.entrySet().removeIf(entry -> entry.getValue() > 100);

        // Kill notification
        if (killNotify.get()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!recentTargetTicks.containsKey(player.getUuid())) continue;
                if ((player.deathTime > 0 || player.getHealth() <= 0) && notifiedDeaths.add(player.getUuid())) {
                    kills++;
                    if (killNotify.get()) info("\u5df2\u51fb\u6740 %s!", player.getName().getString());
                }
            }
        }

        boolean canAttack = selfPopTimer <= 0 && (!activationKey.get().isSet() || activationKey.get().isPressed());

        if (!targets.isEmpty() && canAttack) {
            if (!didRotateThisTick) doBreak();
            if (!didRotateThisTick) doPlace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST - 666)
    private void onPreTickLast(TickEvent.Pre event) {
        // Rotate to last rotation
        if (rotate.get() && lastRotationTimer < getLastRotationStopDelay() && !didRotateThisTick) {
            Rotations.rotate(isLastRotationPos ? Rotations.getYaw(lastRotationPos) : lastYaw, isLastRotationPos ? Rotations.getPitch(lastRotationPos) : lastPitch, -100, null);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null) return;

        if (entity.equals(mc.player)) {
            if (selfPopPause.get()) selfPopTimer = selfPopDelay.get();
        }
        else if (entity instanceof PlayerEntity player) {
            totemPops++;
            if (popNotify.get()) info("%s \u7206\u56fe\u817e\u4e86!", player.getName().getString());
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof EndCrystalEntity)) return;

        if (placing && event.entity.getBlockPos().equals(placingCrystalBlockPos)) {
            placing = false;
            placingTimer = 0;
            placedCrystals.add(event.entity.getId());
        }

        if (fastBreak.get() && !didRotateThisTick && attacks < attackFrequency.get()) {
            float damage = getBreakDamage(event.entity, true);
            if (damage > minDamage.get()) doBreak(event.entity);
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity instanceof EndCrystalEntity) {
            placedCrystals.remove(event.entity.getId());
            removed.remove(event.entity.getId());
            waitingToExplode.remove(event.entity.getId());
        }
    }

    private void setRotation(boolean isPos, Vec3d pos, double yaw, double pitch) {
        didRotateThisTick = true;
        isLastRotationPos = isPos;

        if (isPos) ((IVec3d) lastRotationPos).meteor$set(pos.x, pos.y, pos.z);
        else {
            lastYaw = yaw;
            lastPitch = pitch;
        }

        lastRotationTimer = 0;
    }

    // Break

    private void doBreak() {
        if (!doBreak.get() || breakTimer > 0 || switchTimer > 0 || attacks >= attackFrequency.get()) return;
        if (shouldPause(PauseMode.Break)) return;

        float bestDamage = 0;
        Entity crystal = null;

        // Find best crystal to break
        for (Entity entity : mc.world.getEntities()) {
            float damage = getBreakDamage(entity, true);

            if (damage > bestDamage) {
                bestDamage = damage;
                crystal = entity;
            }
        }

        // Break the crystal
        if (crystal != null) doBreak(crystal);
    }

    private float getBreakDamage(Entity entity, boolean checkCrystalAge) {
        if (!(entity instanceof EndCrystalEntity)) return 0;

        // Check only break own
        if (onlyBreakOwn.get() && !placedCrystals.contains(entity.getId())) return 0;

        // Check if it should already be removed
        if (removed.contains(entity.getId())) return 0;

        // Check attempted breaks
        if (attemptedBreaks.get(entity.getId()) > breakAttempts.get()) return 0;

        // Check crystal age
        if (checkCrystalAge && entity.age < ticksExisted.get()) return 0;

        // Check range
        if (isOutOfRange(entity.getPos(), entity.getBlockPos(), false)) return 0;

        // Check damage to self and anti suicide
        blockPos.set(entity.getBlockPos()).move(0, -1, 0);
        float selfDamage = DamageUtils.crystalDamage(mc.player, entity.getPos(), predictMovement.get(), blockPos);
        if (!ignoreSelfDamage.get() && !aggressiveMode && (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))) return 0;

        // Check damage to targets and face place
        float damage = getDamageToTargets(entity.getPos(), blockPos, true, false);
        boolean shouldFacePlace = shouldFacePlace();
        double minimumDamage = (shouldFacePlace || aggressiveMode) ? Math.min(minDamage.get(), 1.5d) : minDamage.get();

        if (damage < minimumDamage) return 0f;

        return damage;
    }

    private void doBreak(Entity crystal) {
        // Anti weakness
        if (antiWeakness.get()) {
            StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

            // Check for strength
            if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
                // Check if the item in your hand is already valid
                if (!isValidWeaknessItem(mc.player.getMainHandStack(), crystal)) {
                    // Find valid item to break with
                    if (!InvUtils.swap(InvUtils.findInHotbar(stack -> isValidWeaknessItem(stack, crystal)).slot(), false)) return;

                    switchTimer = 1;
                    return;
                }
            }
        }

        // Rotate and attack
        boolean attacked = true;

        if (rotate.get()) {
            double yaw = Rotations.getYaw(crystal);
            double pitch = Rotations.getPitch(crystal, Target.Feet);

            if (doYawSteps(yaw, pitch)) {
                setRotation(true, crystal.getPos(), 0, 0);
                Rotations.rotate(yaw, pitch, 50, () -> attackCrystal(crystal));

                breakTimer = breakDelay.get();
            }
            else {
                attacked = false;
            }
        }
        else {
            attackCrystal(crystal);
            breakTimer = breakDelay.get();
        }

        if (attacked) {
            // Update state
            removed.add(crystal.getId());
            attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);
            waitingToExplode.put(crystal.getId(), 0);

            // Break render
            breakRenderPos.set(crystal.getBlockPos().down());
            breakRenderTimer = breakRenderTime.get();
        }
    }

    private boolean isValidWeaknessItem(ItemStack itemStack, Entity crystal) {
        return DamageUtils.getAttackDamage(mc.player, crystal, itemStack) > 0;
    }

    private void attackCrystal(Entity entity) {
        // Attack
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;

        if (swingMode.get().client()) mc.player.swingHand(hand);
        if (swingMode.get().packet()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        attacks++;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    // Place

    private void doPlace() {
        if (!doPlace.get() || placeTimer > 0) return;
        if (shouldPause(PauseMode.Place)) return;

        // Return if there are no crystals in hotbar or offhand
        if (!InvUtils.testInHotbar(Items.END_CRYSTAL)) return;

        // Return if there are no crystals in either hand and auto switch mode is none
        if (autoSwitch.get() != AutoSwitchMode.None) {
            if (noGapSwitch.get() && autoSwitch.get() == AutoSwitchMode.Normal && offItem != Items.END_CRYSTAL) {
                if (mainItem == Items.ENCHANTED_GOLDEN_APPLE
                || offItem == Items.ENCHANTED_GOLDEN_APPLE
                || mainItem == Items.GOLDEN_APPLE
                || offItem == Items.GOLDEN_APPLE) return;
            }
            if (noBowSwitch.get() && (mainItem == Items.BOW || offItem == Items.BOW)) return;
        } else if (mainItem != Items.END_CRYSTAL && offItem != Items.END_CRYSTAL) return;

        // Check for multiplace
        for (Entity entity : mc.world.getEntities()) {
            if (getBreakDamage(entity, false) > 0) return;
        }

        // Setup variables
        AtomicDouble bestDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBlockPos = new AtomicReference<>(new BlockPos.Mutable());
        AtomicBoolean isSupport = new AtomicBoolean(support.get() != SupportMode.Disabled || autoPlaceObsidian.get());

        // Find best position to place the crystal on
        double iterRange = Math.max(placeRange.get(), autoPlaceObsidian.get() ? obsidianRange.get() : 0);
        BlockIterator.register((int) Math.ceil(iterRange), (int) Math.ceil(iterRange), (bp, blockState) -> {
            // Check if its bedrock or obsidian and return if isSupport is false
            boolean hasBlock = blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN);
            if (!hasBlock && (!isSupport.get() || !blockState.isReplaceable())) return;

            // Check if there is air on top
            blockPos.set(bp.getX(), bp.getY() + 1, bp.getZ());
            if (!mc.world.getBlockState(blockPos).isAir()) return;

            if (placement112.get()) {
                blockPos.move(0, 1, 0);
                if (!mc.world.getBlockState(blockPos).isAir()) return;
            }

            // Check range
            ((IVec3d) vec3d).meteor$set(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
            blockPos.set(bp).move(0, 1, 0);
            if (autoPlaceObsidian.get() && !hasBlock) {
                if (isOutOfRange(vec3d, blockPos, obsidianRange.get(), obsidianRange.get())) return;
            }
            else if (isOutOfRange(vec3d, blockPos, true)) return;

            // Check damage to self and anti suicide
            float selfDamage = DamageUtils.crystalDamage(mc.player, vec3d, predictMovement.get(), bp);
            if (!ignoreSelfDamage.get() && !aggressiveMode && (selfDamage > maxDamage.get() || (antiSuicide.get() && selfDamage >= EntityUtils.getTotalHealth(mc.player)))) return;

            // Check damage to targets and face place
            float damage = getDamageToTargets(vec3d, bp, false, !hasBlock && support.get() == SupportMode.Fast);

            boolean shouldFacePlace = shouldFacePlace();
            double minimumDamage = Math.min(minDamage.get(), (shouldFacePlace || aggressiveMode) ? 1.5 : minDamage.get());

            if (damage < minimumDamage) return;

            // Check if it can be placed
            double x = bp.getX();
            double y = bp.getY() + 1;
            double z = bp.getZ();
            ((IBox) box).meteor$set(x, y, z, x + 1, y + (placement112.get() ? 1 : 2), z + 1);

            if (intersectsWithEntities(box)) return;

            // Compare damage
            if (damage > bestDamage.get() || (isSupport.get() && hasBlock)) {
                bestDamage.set(damage);
                bestBlockPos.get().set(bp);
            }

            if (hasBlock) isSupport.set(false);
        });

        // Place the crystal
        BlockIterator.after(() -> {
            if (bestDamage.get() == 0) return;

            BlockHitResult result = getPlaceInfo(bestBlockPos.get());

            ((IVec3d) vec3d).meteor$set(
                    result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 1.0 / 2.0,
                    result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 1.0 / 2.0,
                    result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 1.0 / 2.0
            );

            if (rotate.get()) {
                double yaw = Rotations.getYaw(vec3d);
                double pitch = Rotations.getPitch(vec3d);

                if (yawStepMode.get() == YawStepMode.Break || doYawSteps(yaw, pitch)) {
                    setRotation(true, vec3d, 0, 0);
                    Rotations.rotate(yaw, pitch, 50, () -> placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null));

                    placeTimer += placeDelay.get();
                }
            }
            else {
                placeCrystal(result, bestDamage.get(), isSupport.get() ? bestBlockPos.get() : null);
                placeTimer += placeDelay.get();
            }
        });
    }

    private BlockHitResult getPlaceInfo(BlockPos blockPos) {
        ((IVec3d) vec3d).meteor$set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction side : Direction.values()) {
            ((IVec3d) vec3dRayTraceEnd).meteor$set(
                    blockPos.getX() + 0.5 + side.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + side.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );

            ((IRaycastContext) raycastContext).meteor$set(vec3d, vec3dRayTraceEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);

            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                return result;
            }
        }

        Direction side = blockPos.getY() > vec3d.y ? Direction.DOWN : Direction.UP;
        return new BlockHitResult(vec3d, side, blockPos, false);
    }

    private void placeCrystal(BlockHitResult result, double damage, BlockPos supportBlock) {
        // Switch
        Item targetItem = supportBlock == null ? Items.END_CRYSTAL : Items.OBSIDIAN;

        FindItemResult item = InvUtils.findInHotbar(targetItem);
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        int movedFromSlot = -1;

        // If the support block isn't in the hotbar, move it from the main inventory to the selected slot
        if (!item.found() && supportBlock != null && autoSwitch.get() != AutoSwitchMode.None) {
            item = InvUtils.find(targetItem);
            if (!item.found()) return;

            movedFromSlot = item.slot();
            InvUtils.move().from(item.slot()).toHotbar(prevSlot);
        }

        if (!item.found()) return;

        if (autoSwitch.get() != AutoSwitchMode.None && !item.isOffhand() && movedFromSlot == -1) {
            InvUtils.swap(item.slot(), false);
        }

        Hand hand = movedFromSlot != -1 ? Hand.MAIN_HAND : item.getHand();
        if (hand == null) return;

        // Place
        if (supportBlock == null) {
            // Place crystal
            mc.interactionManager.sendSequencedPacket(mc.world, (sequence) ->new PlayerInteractBlockC2SPacket(hand, result, sequence));

            if (swingMode.get().client()) mc.player.swingHand(hand);
            if (swingMode.get().packet()) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            placing = true;
            placingTimer = 4;
            kaTimer = 8;
            placingCrystalBlockPos.set(result.getBlockPos()).move(0, 1, 0);

            placeRenderPos.set(result.getBlockPos());
            renderDamage = damage;

            if (renderMode.get() == RenderMode.Normal) {
                placeRenderTimer = placeRenderTime.get();
            } else {
                placeRenderTimer = renderTime.get();
                if (renderMode.get() == RenderMode.Fading) {
                    RenderUtils.renderTickingBlock(
                        placeRenderPos, sideColor.get(),
                        lineColor.get(), shapeMode.get(),
                        0, renderTime.get(), true,
                        false
                    );
                }
            }
        }
        else {
            // Place support block
            if (movedFromSlot != -1) {
                BlockUtils.place(supportBlock, Hand.MAIN_HAND, prevSlot, false, 0, swingMode.get().client(), true, false);
            }
            else {
                BlockUtils.place(supportBlock, item, false, 0, swingMode.get().client(), true, false);
            }

            int delay = autoPlaceObsidian.get() ? obsidianDelay.get() : supportDelay.get();
            placeTimer += delay;

            if (autoPlaceObsidian.get() && obsidianSwapBack.get() && !item.isOffhand()) {
                if (movedFromSlot != -1) {
                    // Move the crystals (swapped into the obsidian's old slot) back to the selected slot
                    InvUtils.move().from(movedFromSlot).toHotbar(prevSlot);
                }
                else {
                    InvUtils.swap(prevSlot, false);
                }
            }

            if (delay == 0) placeCrystal(result, damage, null);
        }

        // Switch back
        if (autoSwitch.get() == AutoSwitchMode.Silent) InvUtils.swap(prevSlot, false);
    }

    // Yaw steps

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            serverYaw = ((PlayerMoveC2SPacket) event.packet).getYaw((float) serverYaw);
        }
    }

    public boolean doYawSteps(double targetYaw, double targetPitch) {
        targetYaw = MathHelper.wrapDegrees(targetYaw) + 180;
        double serverYaw = MathHelper.wrapDegrees(this.serverYaw) + 180;

        if (distanceBetweenAngles(serverYaw, targetYaw) <= yawSteps.get()) return true;

        double delta = Math.abs(targetYaw - serverYaw);
        double yaw = this.serverYaw;

        if (serverYaw < targetYaw) {
            if (delta < 180) yaw += yawSteps.get();
            else yaw -= yawSteps.get();
        }
        else {
            if (delta < 180) yaw -= yawSteps.get();
            else yaw += yawSteps.get();
        }

        setRotation(false, null, yaw, targetPitch);
        Rotations.rotate(yaw, targetPitch, -100, null); // Priority -100 so it sends the packet as the last one, im pretty sure it doesn't matte but idc
        return false;
    }

    private static double distanceBetweenAngles(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    // Face place

    private boolean shouldFacePlace() {
        if (aggressiveMode) return true;

        if (!facePlace.get()) return false;

        if (forceFacePlace.get().isPressed()) return true;

        if (antiBurrow.get() && isAnyTargetBurrowed()) return true;

        // Checks if the provided crystal position should face place to any target
        for (LivingEntity target : targets) {
            if (EntityUtils.getTotalHealth(target) <= facePlaceHealth.get()) return true;

            for (EquipmentSlot slot : AttributeModifierSlot.ARMOR) {
                ItemStack itemStack = target.getEquippedStack(slot);

                if (itemStack == null || itemStack.isEmpty()) {
                    if (facePlaceArmor.get()) return true;
                }
                else {
                    if ((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() * 100 <= facePlaceDurability.get()) return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("deprecation") // Use of AbstractBlock.AbstractBlockState#blocksMovement
    private boolean isAnyTargetBurrowed() {
        for (LivingEntity target : targets) {
            BlockPos.Mutable pos = new BlockPos.Mutable();
            pos.set(target.getBlockPos()).move(0, 1, 0);
            if (mc.world.getBlockState(pos).blocksMovement()) return true;
            pos.move(0, 1, 0);
            if (mc.world.getBlockState(pos).blocksMovement()) return true;
        }

        return false;
    }

    // Others

    private boolean shouldPause(PauseMode process) {
        if (mc.player.isUsingItem() || mc.options.useKey.isPressed()) {
            if (pauseOnUse.get().equals(process)) return true;
        }

        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1.0f) return true;
        for (Module module : pauseModules.get()) if (module.isActive()) return true;
        if (pauseOnMine.get().equals(process) && mc.interactionManager.isBreakingBlock()) return true;
        return (EntityUtils.getTotalHealth(mc.player) <= pauseHealth.get());
    }

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, boolean place) {
        return isOutOfRange(vec3d, blockPos, (place ? placeRange : breakRange).get(), (place ? placeWallsRange : breakWallsRange).get());
    }

    private boolean isOutOfRange(Vec3d vec3d, BlockPos blockPos, double range, double wallsRange) {
        ((IRaycastContext) raycastContext).meteor$set(playerEyePos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult result = mc.world.raycast(raycastContext);

        if (result == null || !result.getBlockPos().equals(blockPos)) // Is behind wall
            return !PlayerUtils.isWithin(vec3d, wallsRange);
        return !PlayerUtils.isWithin(vec3d, range);
    }

    private LivingEntity getNearestTarget() {
        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            double distance = PlayerUtils.squaredDistanceTo(target);

            if (distance < nearestDistance) {
                nearestTarget = target;
                nearestDistance = distance;
            }
        }

        return nearestTarget;
    }

    private float getDamageToTargets(Vec3d vec3d, BlockPos obsidianPos, boolean breaking, boolean fast) {
        if (targetMode.get() == TargetMode.Single) return getSingleTargetDamage(vec3d, obsidianPos, breaking);

        float damage = 0;

        if (fast) {
            LivingEntity target = getNearestTarget();
            if (!(smartDelay.get() && breaking && target.hurtTime > 0)
                && !(smartPlace.get() && !breaking && target.hurtTime > 0))
                damage = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos);
        }
        else {
            for (LivingEntity target : targets) {
                if (smartDelay.get() && breaking && target.hurtTime > 0) continue;
                if (smartPlace.get() && !breaking && target.hurtTime > 0) continue;

                float dmg = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos);

                // Update best target
                if (dmg > bestTargetDamage) {
                    bestTarget = target;
                    bestTargetDamage = dmg;
                    bestTargetTimer = 10;
                }

                damage += dmg;
            }
        }

        return damage;
    }

    private float getSingleTargetDamage(Vec3d vec3d, BlockPos obsidianPos, boolean breaking) {
        LivingEntity selected = null;
        double selectedDamage = 0;

        for (LivingEntity target : targets) {
            if (smartDelay.get() && breaking && target.hurtTime > 0) continue;
            if (smartPlace.get() && !breaking && target.hurtTime > 0) continue;

            float dmg = DamageUtils.crystalDamage(target, vec3d, predictMovement.get(), obsidianPos);

            boolean prefer = selected == null;
            if (!prefer) {
                switch (targetPriority.get()) {
                    case Nearest -> prefer = PlayerUtils.squaredDistanceTo(target) < PlayerUtils.squaredDistanceTo(selected);
                    case LowestHealth -> prefer = EntityUtils.getTotalHealth(target) < EntityUtils.getTotalHealth(selected);
                    case MostDamage -> prefer = dmg > selectedDamage;
                }
            }

            if (prefer) {
                selected = target;
                selectedDamage = dmg;
            }
        }

        if (selected != null) {
            bestTarget = selected;
            bestTargetDamage = selectedDamage;
            bestTargetTimer = 10;
        }

        return (float) selectedDamage;
    }

    @Override
    public String getInfoString() {
        String info = "";

        if (aggressiveMode) info += "\u26a1";

        if (bestTarget != null && bestTargetTimer > 0) {
            if (!info.isEmpty()) info += " ";
            info += EntityUtils.getName(bestTarget);
            if (showDamage.get()) info += String.format(" %.1f", bestTargetDamage);
            if (showHealth.get()) info += String.format(" ♥%.1f", EntityUtils.getTotalHealth(bestTarget));
        }

        return info.isEmpty() ? null : info;
    }

    private void findTargets() {
        targets.clear();

        // Living Entities
        for (Entity entity : mc.world.getEntities()) {
            // Ignore non-living
            if (!(entity instanceof LivingEntity livingEntity)) continue;

            // Player
            if (livingEntity instanceof PlayerEntity player) {
                if (player.getAbilities().creativeMode || livingEntity == mc.player) continue;
                if (!player.isAlive()) continue;
                if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;

                if (ignoreNakeds.get()) {
                    if (player.getOffHandStack().isEmpty()
                        && player.getMainHandStack().isEmpty()
                        && player.getEquippedStack(EquipmentSlot.FEET).isEmpty()
                        && player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                        && player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                        && player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    ) continue;
                }
            }

            // Animals, water animals, monsters, bats, misc
            if (!(entities.get().contains(livingEntity.getType()))) continue;

            // Close enough to damage
            if (livingEntity.squaredDistanceTo(mc.player) > targetRange.get() * targetRange.get()) continue;

            targets.add(livingEntity);
        }
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !removed.contains(entity.getId()));
    }

    // Rendering

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderMode.get() == RenderMode.None) return;

        switch (renderMode.get()) {
            case Normal -> {
                if (renderPlace.get() && placeRenderTimer > 0) {
                    event.renderer.box(placeRenderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
                if (renderBreak.get() && breakRenderTimer > 0) {
                    event.renderer.box(breakRenderPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }

            case Smooth -> {
                if (placeRenderTimer <= 0) return;

                if (renderBoxOne == null) renderBoxOne = new Box(placeRenderPos);
                if (renderBoxTwo == null) renderBoxTwo = new Box(placeRenderPos);
                else ((IBox) renderBoxTwo).meteor$set(placeRenderPos);

                double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / smoothness.get();
                double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / smoothness.get();
                double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / smoothness.get();

                ((IBox) renderBoxOne).meteor$set(
                    renderBoxOne.minX + offsetX,
                    renderBoxOne.minY + offsetY,
                    renderBoxOne.minZ + offsetZ,
                    renderBoxOne.maxX + offsetX,
                    renderBoxOne.maxY + offsetY,
                    renderBoxOne.maxZ + offsetZ
                );

                event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }

            case Gradient -> {
                if (placeRenderTimer <= 0) return;

                Color bottom = new Color(0, 0, 0, 0);

                int x = placeRenderPos.getX();
                int y = placeRenderPos.getY() + 1;
                int z = placeRenderPos.getZ();

                if (shapeMode.get().sides()) {
                    event.renderer.quadHorizontal(x, y, z, x + 1, z + 1, sideColor.get());
                    event.renderer.gradientQuadVertical(x, y, z, x + 1, y - height.get(), z, bottom, sideColor.get());
                    event.renderer.gradientQuadVertical(x, y, z, x, y - height.get(), z + 1, bottom, sideColor.get());
                    event.renderer.gradientQuadVertical(x + 1, y, z, x + 1, y - height.get(), z + 1, bottom, sideColor.get());
                    event.renderer.gradientQuadVertical(x, y, z + 1, x + 1, y - height.get(), z + 1, bottom, sideColor.get());
                }

                if (shapeMode.get().lines()) {
                    event.renderer.line(x, y, z, x + 1, y, z, lineColor.get());
                    event.renderer.line(x, y, z, x, y, z + 1, lineColor.get());
                    event.renderer.line(x + 1, y, z, x + 1, y, z + 1, lineColor.get());
                    event.renderer.line(x, y, z + 1, x + 1, y, z + 1, lineColor.get());

                    event.renderer.line(x, y, z, x, y - height.get(), z, lineColor.get(), bottom);
                    event.renderer.line(x + 1, y, z, x + 1, y - height.get(), z, lineColor.get(), bottom);
                    event.renderer.line(x, y, z + 1, x, y - height.get(), z + 1, lineColor.get(), bottom);
                    event.renderer.line(x + 1, y, z + 1, x + 1, y - height.get(), z + 1, lineColor.get(), bottom);
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderTargetInfo.get() && bestTarget != null && bestTargetTimer > 0 && !bestTarget.isDead()) {
            renderTargetInfo();
        }

        if (renderMode.get() == RenderMode.None || !renderDamageText.get()) return;
        if (placeRenderTimer <= 0 && breakRenderTimer <= 0) return;

        if (renderMode.get() == RenderMode.Smooth) {
            if (renderBoxOne == null) return;
            vec3.set(renderBoxOne.minX + 0.5, renderBoxOne.minY + 0.5, renderBoxOne.minZ + 0.5);
        } else vec3.set(placeRenderPos.getX() + 0.5, placeRenderPos.getY() + 0.5, placeRenderPos.getZ() + 0.5);

        if (NametagUtils.to2D(vec3, damageTextScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = String.format("%.1f", renderDamage);
            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, damageColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    private void renderTargetInfo() {
        vec3.set(bestTarget.getX(), bestTarget.getY() + 2.2, bestTarget.getZ());

        if (NametagUtils.to2D(vec3, targetInfoScale.get())) {
            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1, false, true);

            String text = EntityUtils.getName(bestTarget) + " ♥" + String.format("%.1f", EntityUtils.getTotalHealth(bestTarget));
            if (bestTargetDamage > 0) text += " [" + String.format("%.1f", bestTargetDamage) + "]";

            double w = TextRenderer.get().getWidth(text) / 2;
            TextRenderer.get().render(text, -w, 0, targetInfoColor.get(), true);

            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public enum YawStepMode {
        Break,
        All,
    }

    public enum AutoSwitchMode {
        Normal,
        Silent,
        None
    }

    public enum SupportMode {
        Disabled,
        Accurate,
        Fast
    }

    public enum PauseMode {
        Both,
        Place,
        Break,
        None;

        public boolean equals(PauseMode process) {
            return this == process || this == PauseMode.Both;
        }
    }

    public enum SwingMode {
        Both,
        Packet,
        Client,
        None;

        public boolean packet() {
            return this == Packet || this == Both;
        }

        public boolean client() {
            return this == Client || this == Both;
        }
    }

    public enum RenderMode {
        Normal,
        Smooth,
        Fading,
        Gradient,
        None
    }

    public enum TargetMode {
        Multi,
        Single
    }

    public enum TargetPriority {
        Nearest,
        LowestHealth,
        MostDamage
    }
}
