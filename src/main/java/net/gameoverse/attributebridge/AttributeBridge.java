package net.gameoverse.attributebridge;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.jspecify.annotations.Nullable;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.critical_strike.api.AttributeIdentifiers;
import net.critical_strike.api.CriticalStrikeAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.rpg_foundation.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.spell_engine.api.entity.SpellEngineAttributes;

/**
 * The other mods' attributes that are stored as an Apothic Attributes one instead (see {@link BridgedAttributeInstance}).
 * <p>
 * Each source attribute has a base of 100 and is read as a percentage of it; each target adds its modifiers to a small
 * base (0 or 1). An {@code add_multiplied_base} modifier on the source is the same bonus as an {@code add_value} one of
 * the same amount on the target, which is how nearly every source modifier is written.
 */
public final class AttributeBridge {

    /**
     * @param mirror      If non-null, the source attribute reads as this function of the target's value, for the mods
     *                    that read the source to compute something other than the effect Apothic now applies (Spell
     *                    Engine's physical spell crits and haste, Ranged Weapon API's draw speed). If null, it reads as its
     *                    base, and the mod's own use of it does nothing.
     * @param skipId      A modifier id not carried over (one already included in the target's base value).
     */
    public record Link(Holder<Attribute> source, Holder<Attribute> target, double sourceBase, @Nullable DoubleUnaryOperator mirror, @Nullable Identifier skipId) {

        public AttributeModifier convert(AttributeModifier modifier) {
            return switch (modifier.operation()) {
                case ADD_VALUE -> new AttributeModifier(modifier.id(), modifier.amount() / this.sourceBase, Operation.ADD_VALUE);
                case ADD_MULTIPLIED_BASE -> new AttributeModifier(modifier.id(), modifier.amount(), Operation.ADD_VALUE);
                case ADD_MULTIPLIED_TOTAL -> modifier;
            };
        }

        public boolean skips(AttributeModifier modifier) {
            return modifier.id().equals(this.skipId);
        }
    }

    private static @Nullable Map<Attribute, Link> links;

    private AttributeBridge() {}

    public static @Nullable Link get(Holder<Attribute> attribute) {
        return get(attribute.value());
    }

    public static @Nullable Link get(Attribute attribute) {
        Map<Attribute, Link> map = links();
        return map == null ? null : map.get(attribute);
    }

    private static @Nullable Map<Attribute, Link> links() {
        if (links == null) {
            // The source holders are only set once each mod has registered its attributes.
            if (CriticalStrikeAttributes.CHANCE.attributeEntry == null || CriticalStrikeAttributes.DAMAGE.attributeEntry == null
                || SpellEngineAttributes.EVASION_CHANCE.entry == null || SpellEngineAttributes.HEALING_TAKEN.entry == null
                || EntityAttributes_RangedWeapon.HASTE.entry == null) {
                return null;
            }
            List<Link> list = List.of(
                // Critical Strike reads chance as (value - 100) / 100 and damage as value / 100.
                new Link(CriticalStrikeAttributes.CHANCE.attributeEntry, ALObjects.Attributes.CRIT_CHANCE, 100, t -> 100 * (1 + t), null),
                // Its innate +50% crit damage is Apothic's base of 150%.
                new Link(CriticalStrikeAttributes.DAMAGE.attributeEntry, ALObjects.Attributes.CRIT_DAMAGE, 100, t -> 100 * t, AttributeIdentifiers.INNATE_BONUS),
                new Link(SpellEngineAttributes.EVASION_CHANCE.entry, ALObjects.Attributes.DODGE_CHANCE, 100, null, null),
                new Link(SpellEngineAttributes.HEALING_TAKEN.entry, ALObjects.Attributes.HEALING_RECEIVED, 100, null, null),
                // Ranged Weapon API reads haste as value / 100, a draw speed multiplier like Apothic's.
                new Link(EntityAttributes_RangedWeapon.HASTE.entry, ALObjects.Attributes.DRAW_SPEED, 100, t -> 100 * t, null));
            Map<Attribute, Link> map = new IdentityHashMap<>();
            for (Link link : list) {
                map.put(link.source().value(), link);
            }
            links = map;
        }
        return links;
    }
}
