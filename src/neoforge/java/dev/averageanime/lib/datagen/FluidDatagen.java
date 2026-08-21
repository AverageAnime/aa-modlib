package dev.averageanime.lib.datagen;

import dev.averageanime.lib.fluid.FluidEntry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.Function;

/**
 * The parts of fluid datagen that are the same for any mod.
 *
 * <p>A fluid's block model and its convention tag follow from its id and texture, so writing them by
 * hand is transcription. Call these from a provider rather than repeating the geometry.
 */
public final class FluidDatagen {

    private FluidDatagen() {}

    /**
     * The inset block model used for a placed fluid: a slightly shrunk inner cube so the surface is
     * visible from inside, wrapped in a full outer cube whose faces are culled against neighbours.
     *
     * <p>Textured from the fluid's flow sprite, resolved through {@link FluidEntry#textureFor} so a
     * fluid that borrows another's sprites still lands on the right one.
     */
    public static void insetBlockModel(BlockModelProvider provider, String modId, String fluidId) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                modId, "fluid/" + FluidEntry.textureFor(fluidId) + "_flow");

        var model = provider.withExistingParent("block/" + fluidId + "_block",
                        ResourceLocation.withDefaultNamespace("block/block"))
                .texture("particle", texture)
                .texture("down", texture)
                .texture("up", texture)
                .texture("side", texture);

        var inner = model.element().from(1, 1, 1).to(15, 15, 15);
        inner.face(Direction.DOWN).uvs(1, 1, 15, 15).texture("#down").end();
        inner.face(Direction.UP).uvs(1, 1, 15, 15).texture("#up").end();
        for (Direction side : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            inner.face(side).uvs(1, 1, 15, 15).texture("#side").end();
        }
        inner.end();

        var outer = model.element().from(0, 0, 0).to(16, 16, 16);
        for (Direction side : Direction.values()) {
            outer.face(side).texture("#down").cullface(side).end();
        }
        outer.end();
    }

    /**
     * The {@code c:<fluid>} convention tag, holding both the source and its flowing variant so a recipe
     * written against the tag accepts either.
     *
     * @param tagger how the calling provider turns a {@link TagKey} into an appendable builder
     */
    public static void conventionTag(Function<TagKey<Fluid>, TagAppender> tagger,
                                     String modId, String fluidId, String flowingId) {
        tagger.apply(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", fluidId)))
                .addOptional(ResourceLocation.fromNamespaceAndPath(modId, flowingId))
                .addOptional(ResourceLocation.fromNamespaceAndPath(modId, fluidId));
    }

    /** The slice of a tag builder this needs, so the caller is not tied to one provider base class. */
    public interface TagAppender {
        TagAppender addOptional(ResourceLocation id);
    }
}
