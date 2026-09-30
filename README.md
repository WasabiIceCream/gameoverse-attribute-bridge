# Gameoverse Attribute Bridge

Makes Apothic Attributes' stats the single version of stats other mods also add, so players see one name, one
total and one roll:

| Other mod's attribute | Becomes |
|---|---|
| `critical_strike:chance` | `apothic_attributes:crit_chance` |
| `critical_strike:damage` | `apothic_attributes:crit_damage` |
| `spell_engine:evasion_chance` | `apothic_attributes:dodge_chance` |
| `spell_engine:healing_taken` | `apothic_attributes:healing_received` |
| `ranged_weapon:haste` | `apothic_attributes:draw_speed` |

## How

- `AttributeMapMixin` gives each entity a `BridgedAttributeInstance` for the source attribute. It forwards every
  modifier added to it (gear, effects, enchantments, skills, saved data) to the target attribute's instance,
  converted: the sources have a base of 100 and are read as a percentage of it, so `add_multiplied_base` x becomes
  `add_value` x, `add_value` x becomes x / 100, `add_multiplied_total` stays. Critical Strike's innate +50% crit
  damage is dropped (Apothic's base 150% is the same thing); its innate +5% chance carries over as the baseline.
- `AttributeMixin` gives the source attribute the target's name.
- Critical Strike's chance and damage and Ranged Weapon API's haste read back the Apothic totals, for Spell Engine's
  physical-school spell crits and Ranged Weapon API's bow draw hook. `CritLogicMixin` stops Critical Strike's own
  roll; `ApothicEventsMixin` stops Apothic's draw hook for bows and crossbows (Ranged Weapon API's handles them).
- Evasion and healing taken read as their base, so Spell Engine's own evasion roll and heal multiplier do nothing.
- `ApothicEventsMixin`: Apothic crits skip spell damage (`SpellImpactsMixin` marks it) and flag the damage source as a
  Critical Strike crit, for Spell Engine's "critical" triggers; Apothic dodges call Spell Engine's `onEvade`
  (animation, sound, EVASION triggers) and drop Apothic's own dodge sound.
- Client: declares every Apothic percentage attribute to Dynamic Tooltips, which draws item stat lines itself.
- Client: hides bridged source attributes (Critical Strike Chance, Evasion, Bow Crit Chance and the rest) from
  Apothic's Attributes GUI (the inventory panel), where they would repeat their Apothic target. Uses
  `AttributesGui.addHiddenFilter`, new in Apothic 3.0.1-fabric.6; with an older Apothic it does nothing.

## Pufferfish's Attributes (1.1.0)

Links are per operation: each modifier operation of a source has its own routes (target attribute, operation, scale),
and an operation with no route is dropped. Pufferfish's attributes have no base value (they read as NaN) and their
`add_value` is a flat amount, so only exact flat equivalents are kept (mining speed -> Mining Efficiency, knockback ->
Attack Knockback, fall reduction -> Safe Fall Distance, armor/protection shred's flat part -> Armor/Protection Pierce);
their percentages go to the Apothic, vanilla, Artifacts or Enderscape equivalent (healing, life steal, ranged damage,
experience, breaking speed, bow/crossbow projectile speed, sprinting/mount/consuming speed, stealth, jump, armor and
protection shred). Its unique attributes are left alone. Targets from optional mods are skipped when the mod is absent.
See `docs/skill-forest-design.md` for the full table.

## Too Many Bows (1.1.1)

| Too Many Bows attribute | Becomes |
|---|---|
| `too_many_bows:bow_draw_speed` | `apothic_attributes:draw_speed` |
| `too_many_bows:bow_damage` | `apothic_attributes:arrow_damage` |
| `too_many_bows:bow_crit_chance` | `apothic_attributes:crit_chance` |

Its bows are ordinary bows to the rest of the game, so Apothic's Draw Speed (through Ranged Weapon API's draw hook),
Arrow Damage and Crit Chance already applied to them, and its own attributes stacked on top: draw speed divided the pull
time a second time, bow damage multiplied the arrow's damage a second time, and bow crit chance was a separate crit
roll. The three now read as their base (1, 1, 0), so that code does nothing, and their modifiers go to the Apothic
stat. Draw speed and damage are base-1 multipliers on both sides, so `add_value` and `add_multiplied_base` x are both
+x (`add_multiplied_total` stays); bow crit chance is a 0 to 1 chance like Apothic's, so only its `add_value` carries
over. Skipped when Too Many Bows is absent.

Side effects: its trinkets (Draw Speed Glove, Sharpshot Ring, Stormbound Signet, Dead Eye's Pendant) and the Cursed
Flame Bow's sigil bonus become general stats: draw speed for every bow, crossbow and trident, arrow damage for every
arrow, and the pendant's crit chance for every attack, melee included.

Tooltips: `AttributeTooltipMixin` (items) and `TrinketsTooltipMixin` (Trinkets' own stat lines, which format
`add_value` as a plain number) show a bridged modifier as it applies to its Apothic target, in Apothic's percent style:
the pendant reads "+8% Crit Chance", the glove "+75% Draw Speed". Apothic percentage attributes on any trinket read as
percentages too.

## Better Combat

- `BetterCombatHandSwapMixin`: Better Combat's off-hand swing swapped the weapons' stats from their
  `attribute_modifiers` component only, so Apotheosis affixes, material traits and enchantment attribute effects
  stayed on the main-hand weapon. It now swaps each weapon's full per-slot modifiers (`ItemStack#forEachModifier`).
- `ApothicEventsMixin`: a player's melee hit can be dodged within their Better Combat weapon reach (Better Combat's
  own server range check), not only within Entity Interaction Range.
- `client-config/dynamictooltips/client.toml` is the source of truth for the copy AutoModpack sends to players
  (`host-modpack/main/config/dynamictooltips/`, force-synced by an `allowEditsInFiles` exception): Dynamic Tooltips'
  Entity Interaction Range line is off, since Better Combat's Attack Range line shows the real reach.

## Build

Compiles against the installed jars in `reference-jars/` (not committed): Apothic Attributes (Fabric port
3.0.1-fabric.6; runs on fabric.5+), Critical Strike, Spell Engine, Spell Power, Ranged Weapon API, Dynamic Tooltips, Better Combat. `./gradlew build`.

Tested in game 2026-09-28: tooltips, combined totals, a fist crit at 101% chance dealt exactly 1 x 1.66, a 100% dodge
took no damage, Ranged Weapon API read Apothic's Draw Speed.

MIT licensed.
