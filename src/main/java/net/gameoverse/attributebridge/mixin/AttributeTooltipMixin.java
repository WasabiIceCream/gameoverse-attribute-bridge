package net.gameoverse.attributebridge.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.gameoverse.attributebridge.AttributeBridge;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Item tooltips show a bridged source's modifier as it applies to its target, so "+0.08" on a source that is really
 * Crit Chance reads "+8% Critical Chance" (see {@link AttributeBridge#forDisplay}). Display only.
 */
@Mixin(ItemAttributeModifiers.Display.Default.class)
public class AttributeTooltipMixin {

    @ModifyVariable(method = "apply", at = @At("HEAD"), argsOnly = true)
    private AttributeModifier bridge_display(AttributeModifier modifier, Consumer<Component> consumer, Player player, Holder<Attribute> attribute) {
        return AttributeBridge.forDisplay(attribute, modifier);
    }
}
