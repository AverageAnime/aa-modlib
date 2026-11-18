package dev.averageanime.lib.mixin.recipe;

import com.google.gson.JsonElement;
import dev.averageanime.lib.recipe.adapted.RecipeAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Hands the recipe JSON to {@link RecipeAdapter} at the end of the prepare phase. That is before either
 * loader evaluates conditions: NeoForge in {@code RecipeManager.apply}, Fabric at the head of the apply
 * phase. So the copies' remaining conditions are checked by the loader like any other recipe's.
 *
 * <p>{@code prepare} is declared on the generic superclass, so the descriptor names the {@code Map}
 * override rather than the erased bridge. The signature is the same on NeoForge and Fabric.
 */
@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class RecipePrepareAdaptMixin {

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At("RETURN"))
    private void aalib$adaptRecipes(ResourceManager resources, ProfilerFiller profiler,
                                    CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {
        if ((Object) this instanceof RecipeManager) RecipeAdapter.adapt(cir.getReturnValue());
    }
}
