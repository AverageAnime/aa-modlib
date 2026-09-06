package dev.averageanime.lib.config;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The id carries the mod's namespace so two consumers never collide. */
public final class RecipeConditions {

    private RecipeConditions() {}

    public static void register(IEventBus modEventBus, String modId) {
        DeferredRegister<MapCodec<? extends ICondition>> conditions =
                DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, modId);
        conditions.register("enabled", () -> EnabledCondition.CODEC);
        conditions.register(modEventBus);
    }
}
