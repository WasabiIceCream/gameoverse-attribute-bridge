package net.gameoverse.attributebridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.gameoverse.attributebridge.SpellDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.spell_engine.internals.impact.SpellImpacts;

@Mixin(SpellImpacts.class)
public class SpellImpactsMixin {

    @WrapOperation(method = "performImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean bridge_markSpellDamage(Entity target, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        SpellDamage.enter();
        try {
            return original.call(target, level, source, amount);
        }
        finally {
            SpellDamage.exit();
        }
    }
}
