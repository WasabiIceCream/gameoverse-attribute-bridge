package net.gameoverse.attributebridge;

/**
 * Whether Spell Engine is dealing a spell's damage right now. Spells roll their own crits (Spell Crit, or for physical
 * spells the combined Crit Chance), so Apothic's crit roll leaves that damage alone. Main-thread only, like attacks.
 */
public final class SpellDamage {

    private static int depth;

    private SpellDamage() {}

    public static boolean active() {
        return depth > 0;
    }

    public static void enter() {
        depth++;
    }

    public static void exit() {
        depth--;
    }
}
