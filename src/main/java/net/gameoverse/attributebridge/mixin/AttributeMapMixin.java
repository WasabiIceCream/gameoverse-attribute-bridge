package net.gameoverse.attributebridge.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.gameoverse.attributebridge.AttributeBridge;
import net.gameoverse.attributebridge.BridgedAttributeInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Gives every entity a {@link BridgedAttributeInstance} for each bridged source attribute it has, when the entity also
 * has the target attribute. The value lookups read through it, since vanilla answers them from the supplier's defaults
 * for an instance that doesn't exist yet.
 */
@Mixin(AttributeMap.class)
public abstract class AttributeMapMixin {

    @Shadow
    @Final
    private Map<Holder<Attribute>, AttributeInstance> attributes;

    @Shadow
    @Final
    private AttributeSupplier supplier;

    @Shadow
    public abstract AttributeInstance getInstance(Holder<Attribute> attribute);

    @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
    private void bridge_getInstance(Holder<Attribute> attribute, CallbackInfoReturnable<AttributeInstance> cir) {
        AttributeBridge.init();
        AttributeBridge.Link link = AttributeBridge.get(attribute);
        if (link == null) return;
        AttributeInstance existing = this.attributes.get(attribute);
        if (existing instanceof BridgedAttributeInstance) {
            cir.setReturnValue(existing);
            return;
        }
        if (!this.supplier.hasAttribute(attribute)) return;
        Map<Holder<Attribute>, AttributeInstance> targets = new HashMap<>();
        for (Holder<Attribute> target : link.targets()) {
            AttributeInstance instance = this.getInstance(target);
            if (instance != null) targets.put(target, instance);
        }
        if (!targets.containsKey(link.primary())) return;

        BridgedAttributeInstance bridged = new BridgedAttributeInstance(link, targets, this.supplier.getBaseValue(attribute));
        if (existing != null) {
            // Created before the bridge was ready (not expected): carry its modifiers over.
            for (AttributeModifier modifier : existing.getModifiers()) {
                if (existing.getPermanentModifiers().contains(modifier)) bridged.addPermanentModifier(modifier);
                else bridged.addTransientModifier(modifier);
            }
        }
        this.attributes.put(attribute, bridged);
        cir.setReturnValue(bridged);
    }

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void bridge_getValue(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
        if (AttributeBridge.get(attribute) != null) {
            AttributeInstance instance = this.getInstance(attribute);
            if (instance != null) cir.setReturnValue(instance.getValue());
        }
    }

    @Inject(method = "hasModifier", at = @At("HEAD"), cancellable = true)
    private void bridge_hasModifier(Holder<Attribute> attribute, Identifier id, CallbackInfoReturnable<Boolean> cir) {
        if (AttributeBridge.get(attribute) != null) {
            AttributeInstance instance = this.getInstance(attribute);
            if (instance != null) cir.setReturnValue(instance.hasModifier(id));
        }
    }

    @Inject(method = "getModifierValue", at = @At("HEAD"), cancellable = true)
    private void bridge_getModifierValue(Holder<Attribute> attribute, Identifier id, CallbackInfoReturnable<Double> cir) {
        if (AttributeBridge.get(attribute) != null) {
            AttributeInstance instance = this.getInstance(attribute);
            AttributeModifier modifier = instance == null ? null : instance.getModifier(id);
            if (modifier != null) cir.setReturnValue(modifier.amount());
        }
    }
}
