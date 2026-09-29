package net.gameoverse.attributebridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.critical_strike.internal.CritLogic;
import net.critical_strike.internal.CriticalStriker;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Critical Strike never rolls its own crit: its chance now mirrors Apothic's Crit Chance, which Apothic rolls.
 * Its other behaviour (vanilla jump crits off) stays.
 */
@Mixin(CritLogic.class)
public class CritLogicMixin {

    @Inject(method = "modifyDamage", at = @At("HEAD"), cancellable = true)
    private static void bridge_noRoll(CriticalStriker critter, DamageSource source, float amount, CallbackInfoReturnable<CritLogic.Result> cir) {
        cir.setReturnValue(null);
    }
}
