package dev.averageanime.lib.datagen;

import dev.averageanime.lib.fluid.FluidEntry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;

import java.util.function.BiConsumer;

public final class DatagenHelpers {

    private DatagenHelpers() {}

    /** A shrunk inner cube inside a full outer cube whose faces cull against neighbours. */
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

    /** The {@code c:<fluid>} convention tag, holding both the source and its flowing variant. */
    public static void conventionTag(BiConsumer<TagKey<Fluid>, ResourceLocation> add,
                                     String modId, String fluidId, String flowingId) {
        conventionTag(add, fluidId, modId, fluidId, flowingId);
    }

    /**
     * A mod that calls its fluid {@code liquid_silicon} still belongs under {@code c:molten_silicon} for
     * anything keyed on the molten convention, which is why the tag path is named apart from the fluid.
     */
    public static void conventionTag(BiConsumer<TagKey<Fluid>, ResourceLocation> add, String tagPath,
                                     String modId, String fluidId, String flowingId) {
        TagKey<Fluid> tag = TagKey.create(Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath("c", tagPath));
        add.accept(tag, ResourceLocation.fromNamespaceAndPath(modId, flowingId));
        add.accept(tag, ResourceLocation.fromNamespaceAndPath(modId, fluidId));
    }

    /** @param add the provider's own {@code add(Block, LootTable.Builder)} */
    public static void selfDrop(BiConsumer<Block, LootTable.Builder> add, Block block) {
        add.accept(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion())));
    }
}
