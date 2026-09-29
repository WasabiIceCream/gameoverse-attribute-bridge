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
3.0.1-fabric.5+), Critical Strike, Spell Engine, Spell Power, Ranged Weapon API, Dynamic Tooltips, Better Combat. `./gradlew build`.

Tested in game 2026-09-28: tooltips, combined totals, a fist crit at 101% chance dealt exactly 1 x 1.66, a 100% dodge
took no damage, Ranged Weapon API read Apothic's Draw Speed.

MIT licensed.
