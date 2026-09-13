package com.bettercpvp.crystalauraplus.mixin;

import com.bettercpvp.crystalauraplus.CrystalAuraPlusAddon;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.player.TitleScreenCredits;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Colors the addon author name (aqua, like §b) on the title screen credits.
 * 给主界面「My Meteor Addon by 作者」里的作者名上色（§b 青色）。
 */
@Mixin(TitleScreenCredits.class)
public abstract class TitleScreenCreditsMixin {
    @ModifyArg(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;formatted(Lnet/minecraft/util/Formatting;)Lnet/minecraft/text/MutableText;", ordinal = 2), index = 0)
    private static Formatting meteor$colorAuthor(MeteorAddon addon, Formatting original) {
        return addon instanceof CrystalAuraPlusAddon ? Formatting.AQUA : original;
    }
}
