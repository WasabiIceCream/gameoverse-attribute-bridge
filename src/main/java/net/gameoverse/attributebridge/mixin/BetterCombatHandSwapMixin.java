package net.gameoverse.attributebridge.mixin;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Better Combat's off-hand swing swaps the two weapons' stats using only their {@code attribute_modifiers} component,
 * so everything added through {@link ItemStack#forEachModifier} (Apotheosis affixes and gems, material traits,
 * enchantment attribute effects) stayed on the main-hand weapon for the off-hand swing, and the off-hand weapon's never
 * applied. This swaps the full per-slot modifiers instead, the same ones vanilla applies when equipping.
 * <p>
 * Both calls happen with the stacks in their normal hands: before the swing (useOffHand) the entity has the main-hand
 * weapon's main-hand modifiers and the off-hand weapon's off-hand ones, and gets the reverse; after it, back again.
 * Everything is removed before anything is added, so modifiers shared by both hands (the {@code hand} slot group) end
 * up applied exactly once.
 */
@Mixin(PlayerAttackHelper.class)
public class BetterCombatHandSwapMixin {

    @Inject(method = "setAttributesForOffHandAttack", at = @At("HEAD"), cancellable = true)
    private static void bridge_fullModifiers(Player player, boolean useOffHand, CallbackInfo ci) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack inMainNow = useOffHand ? main : off, inOffNow = useOffHand ? off : main;
        ItemStack inMainNext = inOffNow, inOffNext = inMainNow;

        player.getAttributes().removeAttributeModifiers(modifiers(inMainNow, EquipmentSlot.MAINHAND));
        player.getAttributes().removeAttributeModifiers(modifiers(inOffNow, EquipmentSlot.OFFHAND));
        player.getAttributes().addTransientAttributeModifiers(modifiers(inMainNext, EquipmentSlot.MAINHAND));
        player.getAttributes().addTransientAttributeModifiers(modifiers(inOffNext, EquipmentSlot.OFFHAND));
        ci.cancel();
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> modifiers(ItemStack stack, EquipmentSlot slot) {
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        if (!stack.isEmpty()) stack.forEachModifier(slot, (BiConsumer<Holder<Attribute>, AttributeModifier>) map::put);
        return map;
    }
}
