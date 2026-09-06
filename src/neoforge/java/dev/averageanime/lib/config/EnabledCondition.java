package dev.averageanime.lib.config;

import com.mojang.serialization.MapCodec;
import dev.averageanime.lib.recipe.RecipeSubjects;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record EnabledCondition(List<String> itemIds) implements ICondition {

    public static final MapCodec<EnabledCondition> CODEC =
            EnabledConditions.codec(EnabledCondition::new, EnabledCondition::itemIds);

    @Override
    public boolean test(@NotNull IContext context) {
        return EnabledConditions.test(itemIds, RecipeSubjects::isAvailable);
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
