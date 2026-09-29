package net.gameoverse.attributebridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.critical_strike.api.CriticalDamageSource;
import net.gameoverse.attributebridge.SpellDamage;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.spell_engine.api.entity.EvasionLogic;

/**
 * Apothic's rolls, as the single version of each stat:
 * <ul>
 * <li>Crits skip spell damage, and mark the damage as a Critical Strike crit so "on critical hit" spell triggers fire.</li>
 * <li>A dodge is also a Spell Engine evasion: its animation, sound and "on evasion" spell triggers (Apothic's own dodge
 * sound is dropped, its smoke stays).</li>
 * <li>Bows and crossbows are drawn faster by Ranged Weapon API's hook, which reads the combined Draw Speed; Apothic's
 * own hook only handles the rest (tridents).</li>
 * </ul>
 */
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.impl.AttributeEvents")
public class ApothicEventsMixin {

    @Inject(method = "apothCriticalStrike", at = @At("HEAD"), cancellable = true)
    private static void bridge_noSpellCrit(LivingEntity target, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (SpellDamage.active()) cir.setReturnValue(amount);
    }

    @Inject(method = "apothCriticalStrike", at = @At("RETURN"))
    private static void bridge_markCrit(LivingEntity target, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (amount > 0 && cir.getReturnValueF() > amount) {
            ((CriticalDamageSource) source).rng_setCriticalDamageMultiplier(cir.getReturnValueF() / amount);
        }
    }

    @WrapOperation(method = "onIncomingDamage", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apothic_attributes/impl/AttributeEvents;dodge(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private static boolean bridge_meleeEvasion(LivingEntity target, DamageSource source, Operation<Boolean> original, @Local(argsOnly = true) float amount) {
        boolean dodged = original.call(target, source);
        if (dodged) EvasionLogic.onEvade(target, amount, source);
        return dodged;
    }

    @Inject(method = "dodgeProjectile", at = @At("RETURN"))
    private static void bridge_projectileEvasion(Projectile proj, HitResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = proj.getOwner() instanceof LivingEntity le ? le : null;
            EvasionLogic.onEvade(target, 0, target.damageSources().mobProjectile(proj, owner));
        }
    }

    @WrapOperation(method = "onDodge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void bridge_noDodgeSound(Level level, Entity player, Entity source, SoundEvent sound, SoundSource category, float volume, float pitch, Operation<Void> original) {}

    @Inject(method = "drawSpeed", at = @At("HEAD"), cancellable = true)
    private static void bridge_bowsByRangedWeaponApi(LivingEntity entity, ItemStack item, int duration, CallbackInfoReturnable<Integer> cir) {
        ItemUseAnimation anim = item.getUseAnimation();
        if (anim == ItemUseAnimation.BOW || anim == ItemUseAnimation.CROSSBOW) cir.setReturnValue(duration);
    }
}
