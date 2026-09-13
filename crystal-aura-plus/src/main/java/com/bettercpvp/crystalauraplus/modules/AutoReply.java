package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import org.meteordev.starscript.Script;

/**
 * Auto Reply - automatically replies in chat when a message contains a keyword.
 * 自动回复 - 聊天消息包含关键词时自动回复。
 */
public class AutoReply extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> trigger = sgGeneral.add(new StringSetting.Builder()
        .name("trigger")
        .description("Reply when a chat message contains this text (e.g. \"whispers to you\"). 消息包含此文本时触发回复。")
        .defaultValue("whispers to you")
        .build()
    );

    private final Setting<String> reply = sgGeneral.add(new StringSetting.Builder()
        .name("reply")
        .description("The message to send. Supports StarScript ({player}, {time}, {server.tps}...). 要发送的回复（支持 StarScript）。")
        .defaultValue("I'm busy right now.")
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Seconds between replies. 两次回复的最小间隔（秒）。")
        .defaultValue(5)
        .min(0)
        .sliderMax(60)
        .build()
    );

    private int timer;

    public AutoReply() {
        super(Categories.Misc, "auto-reply", "Automatically replies in chat when a message contains a keyword. 聊天消息含关键词时自动回复。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer > 0) timer--;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (timer > 0) return;

        String message = event.getMessage().getString();
        if (!message.contains(trigger.get())) return;

        Script script = MeteorStarscript.compile(reply.get());
        if (script == null) return;

        String text = MeteorStarscript.run(script);
        if (text == null || text.isEmpty()) return;

        mc.player.networkHandler.sendChatMessage(text);
        timer = cooldown.get() * 20;
    }
}
