package com.bettercpvp.crystalauraplus.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.value.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * AutoSign+ - automatically writes custom text on signs, with StarScript support.
 * 自动告示牌+ - 放置告示牌时自动写入自定义文本（支持 StarScript，可显示日期与时间）。
 *
 * StarScript variables: {date} 日期, {time} 时间, 以及 Meteor 内置的所有变量（{player}, {ping}, {server.tps} 等）。
 */
public class AutoSignPlus extends Module {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLines = settings.createGroup("Lines");

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The tick delay between sign update packets. 发送告示牌更新包的间隔（tick）。")
        .defaultValue(10)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<String> line1 = sgLines.add(new StringSetting.Builder()
        .name("line-1")
        .description("The first line of the sign. Supports StarScript: {date} 日期, {time} 时间, {player} 你的名字. 告示牌第一行（支持 StarScript）。")
        .defaultValue("AutoSign+")
        .build()
    );

    private final Setting<String> line2 = sgLines.add(new StringSetting.Builder()
        .name("line-2")
        .description("The second line of the sign. Supports StarScript. 告示牌第二行（支持 StarScript）。")
        .defaultValue("{date}")
        .build()
    );

    private final Setting<String> line3 = sgLines.add(new StringSetting.Builder()
        .name("line-3")
        .description("The third line of the sign. Supports StarScript. 告示牌第三行（支持 StarScript）。")
        .defaultValue("{time}")
        .build()
    );

    private final Setting<String> line4 = sgLines.add(new StringSetting.Builder()
        .name("line-4")
        .description("The fourth line of the sign. Supports StarScript. 告示牌第四行（支持 StarScript）。")
        .defaultValue("")
        .build()
    );

    // Some servers (e.g., 2b2t) don't like the sign packet being sent too soon after the swing or block click packets, so queue them.
    private final Queue<UpdateSignC2SPacket> queue = new ArrayDeque<>();
    private int timer = 0;

    public AutoSignPlus() {
        super(Categories.World, "auto-sign-plus", "Automatically writes custom text on signs, with StarScript support. 放置告示牌时自动写入自定义文本（支持 StarScript，可显示日期与时间）。");
    }

    @Override
    public void onActivate() {
        MeteorStarscript.ss.set("date", () -> Value.string(LocalDate.now().format(DATE_FORMAT)));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || queue.peek() == null) {
            timer = 0;
            return;
        }

        if (timer < delay.get()) {
            timer++;
            return;
        }

        mc.player.networkHandler.sendPacket(queue.poll());
        timer = 0;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof AbstractSignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).meteor$getSign();

        queue.add(new UpdateSignC2SPacket(
            sign.getPos(), true,
            process(line1.get()),
            process(line2.get()),
            process(line3.get()),
            process(line4.get())
        ));

        event.cancel();
    }

    private String process(String source) {
        if (source == null || source.isEmpty()) return "";

        Script script = MeteorStarscript.compile(source);
        if (script == null) return "";

        String text = MeteorStarscript.run(script);
        if (text == null) return "";

        return text.length() > 15 ? text.substring(0, 15) : text;
    }
}
