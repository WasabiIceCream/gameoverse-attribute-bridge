package net.gameoverse.attributebridge.client;

import dev.muon.dynamictooltips.api.DynamicTooltipsAPI;
import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import net.gameoverse.attributebridge.BridgedAttributeInstance;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Dynamic Tooltips draws item stat lines itself, so Apothic Attributes' percent-style formatting (see its
 * {@code PercentTooltipMixin}) never reaches them. Declaring each percentage attribute to it shows "+55% Crit Chance"
 * there too, instead of "+0.55 Crit Chance".
 * <p>
 * Apothic's Attributes GUI (inventory panel) lists every attribute the player has; a bridged source attribute's entry
 * ("Critical Strike Chance", "Evasion", "Bow Crit Chance"...) would only repeat its Apothic target, so those are hidden
 * there.
 */
public class AttributeBridgeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("dynamictooltips")) {
            DynamicTooltipsCompat.declarePercentAttributes();
        }
        try {
            AttributesGuiCompat.hideBridgedAttributes();
        }
        catch (LinkageError e) {
            // Apothic Attributes before 3.0.1-fabric.6 has no Attributes GUI (or no hidden filters): nothing to hide.
        }
    }

    private static class AttributesGuiCompat {

        static void hideBridgedAttributes() {
            AttributesGui.addHiddenFilter(instance -> instance instanceof BridgedAttributeInstance);
        }
    }

    private static class DynamicTooltipsCompat {

        static void declarePercentAttributes() {
            BuiltInRegistries.ATTRIBUTE.listElements()
                .filter(holder -> holder.value() instanceof PercentageAttribute)
                .forEach(holder -> DynamicTooltipsAPI.declarePercentAttribute(holder.key().identifier(), 100));
        }
    }
}
