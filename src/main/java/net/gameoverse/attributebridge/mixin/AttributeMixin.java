package net.gameoverse.attributebridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.gameoverse.attributebridge.AttributeBridge;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * A bridged source attribute shows its target's name everywhere (item and potion tooltips, trinket tooltips, skill
 * descriptions, stat panels), so players only ever see the Apothic name.
 */
@Mixin(Attribute.class)
public class AttributeMixin {

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void bridge_name(CallbackInfoReturnable<String> cir) {
        AttributeBridge.Link link = AttributeBridge.get((Attribute) (Object) this);
        if (link != null) cir.setReturnValue(link.target().value().getDescriptionId());
    }
}
