package net.gameoverse.attributebridge;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import org.jspecify.annotations.Nullable;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import net.critical_strike.api.AttributeIdentifiers;
import net.critical_strike.api.CriticalStrikeAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.rpg_foundation.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.spell_engine.api.entity.SpellEngineAttributes;

/**
 * The other mods' attributes that are stored as another attribute instead (see {@link BridgedAttributeInstance}).
 * <p>
 * Each modifier operation of a source attribute has its own routes: the target attribute, the operation it becomes,
 * and a scale. An operation with no route is dropped. Critical Strike's, Spell Engine's and Ranged Weapon API's
 * attributes have a base of 100 and are read as a percentage of it; Pufferfish's Attributes' have no base, their
 * {@code add_value} is a flat amount and their {@code add_multiplied_base} a percentage. On both, an
 * {@code add_multiplied_base} modifier is the same bonus as an {@code add_value} one of the same amount on the Apothic
 * target, which is how nearly every source modifier is written.
 */
public final class AttributeBridge {

    public record Route(Holder<Attribute> target, Operation operation, double scale) {}

    /**
     * @param mirror If non-null, the source attribute reads as this function of the primary target's value, for mods
     *               that read the source to compute something other than the effect the target now applies (Spell
     *               Engine's physical spell crits and haste, Ranged Weapon API's draw speed). If null, it reads as its
     *               base, and the mod's own use of it does nothing.
     * @param skipId A modifier id not carried over (one already included in the target's base value).
     */
    public record Link(Holder<Attribute> source, Map<Operation, List<Route>> routes, @Nullable DoubleUnaryOperator mirror, @Nullable Identifier skipId) {

        /**
         * The target whose name and value represent the source: the {@code add_multiplied_base} route's, as sources are
         * nearly always written that way.
         */
        public Holder<Attribute> primary() {
            List<Route> r = this.routes.getOrDefault(Operation.ADD_MULTIPLIED_BASE, List.of());
            if (!r.isEmpty()) return r.get(0).target();
            return this.routes.values().stream().flatMap(List::stream).findFirst().orElseThrow().target();
        }

        public List<Holder<Attribute>> targets() {
            List<Holder<Attribute>> list = new ArrayList<>();
            this.routes.values().forEach(rs -> rs.forEach(r -> {
                if (!list.contains(r.target())) list.add(r.target());
            }));
            return list;
        }

        public List<Route> routes(AttributeModifier modifier) {
            return this.routes.getOrDefault(modifier.operation(), List.of());
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
        Map<Attribute, Link> map = links;
        return map == null ? null : map.get(attribute);
    }

    /**
     * Builds the links on first use from an entity's attribute map, by which point every mod has registered its
     * attributes. Before that, {@link #get} finds nothing.
     */
    public static void init() {
        if (links != null) return;
        // The source holders are only set once each mod has registered its attributes.
        if (CriticalStrikeAttributes.CHANCE.attributeEntry == null || CriticalStrikeAttributes.DAMAGE.attributeEntry == null
            || SpellEngineAttributes.EVASION_CHANCE.entry == null || SpellEngineAttributes.HEALING_TAKEN.entry == null
            || EntityAttributes_RangedWeapon.HASTE.entry == null) {
            return;
        }
        Map<Attribute, Link> map = new IdentityHashMap<>();
        // Critical Strike reads chance as (value - 100) / 100 and damage as value / 100.
        percent100(map, CriticalStrikeAttributes.CHANCE.attributeEntry, ALObjects.Attributes.CRIT_CHANCE, t -> 100 * (1 + t), null);
        // Its innate +50% crit damage is Apothic's base of 150%.
        percent100(map, CriticalStrikeAttributes.DAMAGE.attributeEntry, ALObjects.Attributes.CRIT_DAMAGE, t -> 100 * t, AttributeIdentifiers.INNATE_BONUS);
        percent100(map, SpellEngineAttributes.EVASION_CHANCE.entry, ALObjects.Attributes.DODGE_CHANCE, null, null);
        percent100(map, SpellEngineAttributes.HEALING_TAKEN.entry, ALObjects.Attributes.HEALING_RECEIVED, null, null);
        // Ranged Weapon API reads haste as value / 100, a draw speed multiplier like Apothic's.
        percent100(map, EntityAttributes_RangedWeapon.HASTE.entry, ALObjects.Attributes.DRAW_SPEED, t -> 100 * t, null);
        PufferfishLinks.add(map);
        TooManyBowsLinks.add(map);
        links = map;
    }

    /**
     * A source with a base of 100 read as a percentage of it: add_value x is x / 100, add_multiplied_base x is x.
     */
    private static void percent100(Map<Attribute, Link> map, Holder<Attribute> source, Holder<Attribute> target, @Nullable DoubleUnaryOperator mirror, @Nullable Identifier skipId) {
        Map<Operation, List<Route>> routes = new EnumMap<>(Operation.class);
        routes.put(Operation.ADD_VALUE, List.of(new Route(target, Operation.ADD_VALUE, 1 / 100D)));
        routes.put(Operation.ADD_MULTIPLIED_BASE, List.of(new Route(target, Operation.ADD_VALUE, 1)));
        routes.put(Operation.ADD_MULTIPLIED_TOTAL, List.of(new Route(target, Operation.ADD_MULTIPLIED_TOTAL, 1)));
        map.put(source.value(), new Link(source, routes, mirror, skipId));
    }

    /**
     * The modifier as a tooltip should show it: a bridged source's modifier as it applies to the primary target (when
     * that target is an Apothic percentage attribute, the only case where the source's own formatting can differ), and
     * an {@code add_value} modifier on a percentage attribute in the percent style Apothic uses (see
     * {@link PercentageAttribute#forDisplay}). The tooltip keeps the source attribute, which already has the target's
     * name.
     */
    public static AttributeModifier forDisplay(Holder<Attribute> attribute, AttributeModifier modifier) {
        Link link = get(attribute);
        if (link != null) {
            for (Route route : link.routes(modifier)) {
                if (route.target() == link.primary() && route.target().value() instanceof PercentageAttribute) {
                    AttributeModifier converted = new AttributeModifier(modifier.id(), modifier.amount() * route.scale(), route.operation());
                    return PercentageAttribute.forDisplay(route.target(), converted);
                }
            }
            return modifier;
        }
        return PercentageAttribute.forDisplay(attribute, modifier);
    }

    static Optional<Holder<Attribute>> attribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(id)).map(h -> (Holder<Attribute>) h);
    }

    /**
     * Pufferfish's Attributes (optional): its attributes that duplicate an Apothic, vanilla, Artifacts or Enderscape one.
     * Its flat {@code add_value} amounts are dropped where the target has no flat equivalent.
     */
    private static final class PufferfishLinks {

        static void add(Map<Attribute, Link> map) {
            // percentage (add_multiplied_base x) -> add_value x on a target read as a multiplier or fraction
            pct(map, "healing", "apothic_attributes:healing_received", true);
            pct(map, "life_steal", "apothic_attributes:life_steal", false);
            pct(map, "ranged_damage", "apothic_attributes:projectile_damage", true);
            pct(map, "experience", "apothic_attributes:experience_gained", true);
            pct(map, "breaking_speed", "minecraft:block_break_speed", true);
            pct(map, "bow_projectile_speed", "apothic_attributes:arrow_velocity", true);
            pct(map, "crossbow_projectile_speed", "apothic_attributes:arrow_velocity", true);
            pct(map, "sprinting_speed", "artifacts:sprinting_speed", true);
            pct(map, "mount_speed", "artifacts:mount_speed", true);
            pct(map, "stealth", "enderscape:stealth", false);
            pct(map, "consuming_speed", "artifacts:eating_speed", true, "artifacts:drinking_speed");
            // flat and percentage go to different targets
            split(map, "armor_shred", "apothic_attributes:armor_pierce", "apothic_attributes:armor_shred");
            split(map, "protection_shred", "apothic_attributes:prot_pierce", "apothic_attributes:prot_shred");
            // exact flat equivalents
            flat(map, "mining_speed", "minecraft:mining_efficiency");
            flat(map, "knockback", "minecraft:attack_knockback");
            flat(map, "fall_reduction", "minecraft:safe_fall_distance");
            // Jump: a percentage of jump power, as vanilla's jump strength multiplied.
            route(map, "jump", Map.of(Operation.ADD_MULTIPLIED_BASE, List.of(t("minecraft:jump_strength", Operation.ADD_MULTIPLIED_BASE)),
                Operation.ADD_MULTIPLIED_TOTAL, List.of(t("minecraft:jump_strength", Operation.ADD_MULTIPLIED_TOTAL))));
        }

        /**
         * @param totals Whether add_multiplied_total carries over (the target's value is a multiplier, base above 0).
         */
        static void pct(Map<Attribute, Link> map, String source, String target, boolean totals, String... more) {
            List<String> targets = new ArrayList<>(List.of(target));
            targets.addAll(List.of(more));
            List<Route> base = new ArrayList<>(), total = new ArrayList<>();
            for (String id : targets) {
                base.add(t(id, Operation.ADD_VALUE));
                total.add(t(id, Operation.ADD_MULTIPLIED_TOTAL));
            }
            Map<Operation, List<Route>> routes = new EnumMap<>(Operation.class);
            routes.put(Operation.ADD_MULTIPLIED_BASE, base);
            if (totals) routes.put(Operation.ADD_MULTIPLIED_TOTAL, total);
            route(map, source, routes);
        }

        static void split(Map<Attribute, Link> map, String source, String flatTarget, String pctTarget) {
            route(map, source, Map.of(Operation.ADD_VALUE, List.of(t(flatTarget, Operation.ADD_VALUE)),
                Operation.ADD_MULTIPLIED_BASE, List.of(t(pctTarget, Operation.ADD_VALUE))));
        }

        static void flat(Map<Attribute, Link> map, String source, String target) {
            route(map, source, Map.of(Operation.ADD_VALUE, List.of(t(target, Operation.ADD_VALUE))));
        }

        /**
         * A route whose target is missing (an optional mod isn't installed) is left out; so is a link with no routes.
         */
        static void route(Map<Attribute, Link> map, String source, Map<Operation, List<Route>> routes) {
            Optional<Holder<Attribute>> src = attribute("puffish_attributes:" + source);
            if (src.isEmpty()) return;
            Map<Operation, List<Route>> present = new EnumMap<>(Operation.class);
            routes.forEach((op, rs) -> {
                List<Route> kept = rs.stream().filter(r -> r.target() != null).toList();
                if (!kept.isEmpty()) present.put(op, kept);
            });
            if (!present.isEmpty()) map.put(src.get().value(), new Link(src.get(), present, null, null));
        }

        static Route t(String target, Operation operation) {
            return new Route(attribute(target).orElse(null), operation, 1);
        }
    }

    /**
     * Too Many Bows (optional): its bow draw speed, bow damage and bow crit chance are Apothic's Draw Speed, Arrow Damage
     * and Crit Chance, which already apply to its bows (Ranged Weapon API's draw hook, Apothic's arrow hooks). They read
     * as their base (1, 1, 0), so its own pull-time division, damage multiplier and crit roll do nothing.
     */
    private static final class TooManyBowsLinks {

        static void add(Map<Attribute, Link> map) {
            // Base 1 multipliers on both sides: add_value x and add_multiplied_base x are both +x.
            multiplier(map, "bow_draw_speed", ALObjects.Attributes.DRAW_SPEED);
            multiplier(map, "bow_damage", ALObjects.Attributes.ARROW_DAMAGE);
            // A chance from 0 to 1 with a base of 0, the same scale as Apothic's; a multiplied bonus on 0 does nothing.
            link(map, "bow_crit_chance", Map.of(Operation.ADD_VALUE, List.of(new Route(ALObjects.Attributes.CRIT_CHANCE, Operation.ADD_VALUE, 1))));
        }

        static void multiplier(Map<Attribute, Link> map, String source, Holder<Attribute> target) {
            Map<Operation, List<Route>> routes = new EnumMap<>(Operation.class);
            routes.put(Operation.ADD_VALUE, List.of(new Route(target, Operation.ADD_VALUE, 1)));
            routes.put(Operation.ADD_MULTIPLIED_BASE, List.of(new Route(target, Operation.ADD_VALUE, 1)));
            routes.put(Operation.ADD_MULTIPLIED_TOTAL, List.of(new Route(target, Operation.ADD_MULTIPLIED_TOTAL, 1)));
            link(map, source, routes);
        }

        static void link(Map<Attribute, Link> map, String source, Map<Operation, List<Route>> routes) {
            attribute("too_many_bows:" + source).ifPresent(src -> map.put(src.value(), new Link(src, new EnumMap<>(routes), null, null)));
        }
    }
}
