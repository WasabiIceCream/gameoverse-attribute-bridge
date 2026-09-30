package net.gameoverse.attributebridge.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import net.gameoverse.attributebridge.AttributeBridge;
import net.minecraft.core.Holder;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Trinkets (optional) formats trinket stat lines itself, by operation only: {@code add_value} as a plain number. Its
 * lines get the same conversion as item tooltips ({@link AttributeBridge#forDisplay}), so Too Many Bows' Dead Eye's
 * Pendant reads "+8% Critical Chance" instead of "+0.08", and Apothic percentage attributes on any trinket read as
 * percentages. Works on a copy; display only.
 */
@Pseudo
@Mixin(targets = "eu.pb4.trinkets.impl.ItemStackTooltipUtil")
public class TrinketsTooltipMixin {

    @ModifyVariable(method = "addAttributes", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> bridge_display(Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> modifiers) {
        Multimap<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> copy = LinkedListMultimap.create();
        for (Map.Entry<Holder<Attribute>, Tuple<AttributeModifier, ItemAttributeModifiers.Display>> e : modifiers.entries()) {
            Tuple<AttributeModifier, ItemAttributeModifiers.Display> t = e.getValue();
            copy.put(e.getKey(), new Tuple<>(AttributeBridge.forDisplay(e.getKey(), t.getA()), t.getB()));
        }
        return copy;
    }
}
