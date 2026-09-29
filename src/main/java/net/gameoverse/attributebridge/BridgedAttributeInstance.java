package net.gameoverse.attributebridge;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * An entity's instance of a bridged source attribute (see {@link AttributeBridge}). It holds no modifiers itself: every
 * modifier added to it, from gear, effects, enchantments, skills or saved data, goes onto the target attributes'
 * instances of the same entity instead, converted per its operation's routes. Removal by id is forwarded the same way,
 * so whatever added a modifier can still take it off.
 */
public class BridgedAttributeInstance extends AttributeInstance {

    private final AttributeBridge.Link link;
    private final Map<Holder<Attribute>, AttributeInstance> targets;

    /**
     * @param targets The entity's instances of the link's targets that exist (at least the primary one).
     */
    public BridgedAttributeInstance(AttributeBridge.Link link, Map<Holder<Attribute>, AttributeInstance> targets, double baseValue) {
        super(link.source(), instance -> {});
        this.link = link;
        this.targets = targets;
        this.setBaseValue(baseValue);
    }

    @Override
    public double getValue() {
        AttributeInstance primary = this.targets.get(this.link.primary());
        return this.link.mirror() == null || primary == null ? super.getValue() : this.link.mirror().applyAsDouble(primary.getValue());
    }

    @Override
    public AttributeModifier getModifier(Identifier id) {
        for (AttributeInstance target : this.targets.values()) {
            AttributeModifier modifier = target.getModifier(id);
            if (modifier != null) return modifier;
        }
        return null;
    }

    @Override
    public boolean hasModifier(Identifier id) {
        return this.getModifier(id) != null;
    }

    // Adds replace a modifier with the same id instead of throwing, in case a target already holds one.

    @Override
    public void addOrUpdateTransientModifier(AttributeModifier modifier) {
        this.forward(modifier, false);
    }

    @Override
    public void addTransientModifier(AttributeModifier modifier) {
        this.forward(modifier, false);
    }

    @Override
    public void addOrReplacePermanentModifier(AttributeModifier modifier) {
        this.forward(modifier, true);
    }

    @Override
    public void addPermanentModifier(AttributeModifier modifier) {
        this.forward(modifier, true);
    }

    private void forward(AttributeModifier modifier, boolean permanent) {
        if (this.link.skips(modifier)) return;
        for (AttributeBridge.Route route : this.link.routes(modifier)) {
            AttributeInstance target = this.targets.get(route.target());
            if (target == null) continue;
            AttributeModifier converted = new AttributeModifier(modifier.id(), modifier.amount() * route.scale(), route.operation());
            if (permanent) target.addOrReplacePermanentModifier(converted);
            else target.addOrUpdateTransientModifier(converted);
        }
    }

    @Override
    public boolean removeModifier(Identifier id) {
        boolean removed = false;
        for (AttributeInstance target : this.targets.values()) {
            removed |= target.removeModifier(id);
        }
        return removed;
    }

    /**
     * Saved data: players saved before the bridge have the source attribute's permanent modifiers (Critical Strike's
     * innate bonus) stored under it.
     */
    @Override
    public void apply(Packed packed) {
        for (AttributeModifier modifier : packed.modifiers()) {
            this.addOrReplacePermanentModifier(modifier);
        }
    }

    /**
     * Copying from another entity's map (respawn): the other bridged instance holds nothing, the targets copy themselves.
     */
    @Override
    public void replaceFrom(AttributeInstance other) {}
}
