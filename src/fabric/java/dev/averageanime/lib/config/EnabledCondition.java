package dev.averageanime.lib.config;

import com.mojang.serialization.MapCodec;
import dev.averageanime.lib.recipe.RecipeSubjects;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;

import java.util.List;

public record EnabledCondition(List<String> itemIds) implements ResourceCondition {

    public static final MapCodec<EnabledCondition> CODEC =
            EnabledConditions.codec(EnabledCondition::new, EnabledCondition::itemIds);

    /** Set by {@link RecipeConditions#register}, which is the only thing that knows the namespace. */
    public static ResourceConditionType<EnabledCondition> TYPE;

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(HolderLookup.Provider registries) {
        return EnabledConditions.test(itemIds, RecipeSubjects::isAvailable);
    }
}
