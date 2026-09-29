package net.gameoverse.attributebridge.client;

import dev.muon.dynamictooltips.api.DynamicTooltipsAPI;
import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Dynamic Tooltips draws item stat lines itself, so Apothic Attributes' percent-style formatting (see its
 * {@code PercentTooltipMixin}) never reaches them. Declaring each percentage attribute to it shows "+55% Crit Chance"
 * there too, instead of "+0.55 Crit Chance".
 */
public class AttributeBridgeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("dynamictooltips")) {
            DynamicTooltipsCompat.declarePercentAttributes();
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
