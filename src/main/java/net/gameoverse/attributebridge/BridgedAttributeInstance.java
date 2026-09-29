package net.gameoverse.attributebridge;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * An entity's instance of a bridged source attribute (see {@link AttributeBridge}). It holds no modifiers itself: every
 * modifier added to it, from gear, effects, enchantments, skills or saved data, goes onto the target attribute's instance
 * of the same entity instead, converted. Removal by id is forwarded the same way, so whatever added a modifier can still
 * take it off.
 */
public class BridgedAttributeInstance extends AttributeInstance {

    private final AttributeBridge.Link link;
    private final AttributeInstance target;

    public BridgedAttributeInstance(AttributeBridge.Link link, AttributeInstance target, double baseValue) {
        super(link.source(), instance -> {});
        this.link = link;
        this.target = target;
        this.setBaseValue(baseValue);
    }

    @Override
    public double getValue() {
        return this.link.mirror() == null ? super.getValue() : this.link.mirror().applyAsDouble(this.target.getValue());
    }

    @Override
    public AttributeModifier getModifier(Identifier id) {
        return this.target.getModifier(id);
    }

    @Override
    public boolean hasModifier(Identifier id) {
        return this.target.hasModifier(id);
    }

    // Adds replace a modifier with the same id instead of throwing, in case the target already holds one.

    @Override
    public void addOrUpdateTransientModifier(AttributeModifier modifier) {
        if (!this.link.skips(modifier)) this.target.addOrUpdateTransientModifier(this.link.convert(modifier));
    }

    @Override
    public void addTransientModifier(AttributeModifier modifier) {
        this.addOrUpdateTransientModifier(modifier);
    }

    @Override
    public void addOrReplacePermanentModifier(AttributeModifier modifier) {
        if (!this.link.skips(modifier)) this.target.addOrReplacePermanentModifier(this.link.convert(modifier));
    }

    @Override
    public void addPermanentModifier(AttributeModifier modifier) {
        this.addOrReplacePermanentModifier(modifier);
    }

    @Override
    public boolean removeModifier(Identifier id) {
        return this.target.removeModifier(id);
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
     * Copying from another entity's map (respawn): the other bridged instance holds nothing, the target copies itself.
     */
    @Override
    public void replaceFrom(AttributeInstance other) {}
}
