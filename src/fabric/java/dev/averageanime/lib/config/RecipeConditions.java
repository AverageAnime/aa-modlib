package dev.averageanime.lib.config;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;

/** The id carries the mod's namespace so two consumers never collide. */
public final class RecipeConditions {

    private RecipeConditions() {}

    public static void register(String modId) {
        EnabledCondition.TYPE = ResourceConditionType.create(
                ResourceLocation.fromNamespaceAndPath(modId, "enabled"), EnabledCondition.CODEC);
        ResourceConditions.register(EnabledCondition.TYPE);
    }
}
