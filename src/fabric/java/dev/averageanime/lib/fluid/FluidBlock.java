package dev.averageanime.lib.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;


/**
 * Declares a fluid on Fabric: source, flowing variant, liquid block and bucket, registered eagerly
 * because Fabric has no deferred phase.
 *
 * <p>Fog is kept in a map keyed by fluid rather than on the fluid type, because Fabric has no
 * equivalent of NeoForge's client extensions; the fog mixins read it back out.
 */
public class FluidBlock {

    /**
     * Namespace the fluid and its derived entries register under. Passed in rather than imported so the
     * same builder serves any mod; configure once during mod initialisation.
     */
    private static String MOD_ID;

    public static void configure(String modId) {
        MOD_ID = modId;
    }

    private static String modId() {
        if (MOD_ID == null) {
            throw new IllegalStateException(
                    "FluidBlock.configure(modId) must be called before any fluid is declared");
        }
        return MOD_ID;
    }

    /** Per-fluid submersion fog parameters (color + shader fog distances). */
    public record FogParams(Vector3f color, float start, float end) {}

    private static final Map<Fluid, FogParams> FOG_MAP = new HashMap<>();

    private final String name;
    private String texture;
    private int slopeFindDistance = 4;
    private int levelDecreasePerBlock = 3;
    private float fogStart = FluidEntry.DEFAULT_FOG_START;
    private float fogEnd = FluidEntry.DEFAULT_FOG_END;

    public FlowingFluid SOURCE;
    public FlowingFluid FLOWING;
    public LiquidBlock BLOCK;
    public Item BUCKET;

    /** Null for fluids that are not this mod's — the "is ours" test used by the client mixins. */
    @Environment(EnvType.CLIENT)
    public static Vector3f getFogColor(Fluid fluid) {
        FogParams params = FOG_MAP.get(fluid);
        return params == null ? null : params.color();
    }

    @Environment(EnvType.CLIENT)
    public static FogParams getFogParams(Fluid fluid) {
        return FOG_MAP.get(fluid);
    }

    public FluidBlock(String name) {
        this.name = name;
        this.texture = name;
    }

    /** Reuse another fluid's sprites instead of requiring {@code <name>_still|_flow} textures. */
    public FluidBlock tex(String texture) {
        this.texture = texture;
        return this;
    }

    public FluidBlock flow(int slopeFindDistance, int levelDecreasePerBlock) {
        this.slopeFindDistance = slopeFindDistance;
        this.levelDecreasePerBlock = levelDecreasePerBlock;
        return this;
    }

    public FluidBlock fog(float fogStart, float fogEnd) {
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        return this;
    }

    public FluidBlock build() {
        FluidType fluidType = new FluidType(FluidType.Properties.create()
                .density(1400)
                .viscosity(1500));
        Registry.register(PortingLibFluids.FLUID_TYPES,
                ResourceLocation.fromNamespaceAndPath(modId(), name), fluidType);

        FluidBlock self = this;
        BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(
                () -> fluidType,
                () -> self.SOURCE,
                () -> self.FLOWING)
                .slopeFindDistance(slopeFindDistance)
                .levelDecreasePerBlock(levelDecreasePerBlock)
                .block(() -> self.BLOCK)
                .bucket(() -> self.BUCKET)
                .tickRate(25);

        SOURCE = Registry.register(BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(modId(), name),
                new BaseFlowingFluid.Source(props));

        FLOWING = Registry.register(BuiltInRegistries.FLUID,
                ResourceLocation.fromNamespaceAndPath(modId(), "flowing_" + name),
                new BaseFlowingFluid.Flowing(props));

        BLOCK = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(modId(), name + "_block"),
                new LiquidBlock(SOURCE, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

        BUCKET = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(modId(), name + "_bucket"),
                new BucketItem(SOURCE, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

        return this;
    }

    @Environment(EnvType.CLIENT)
    public void registerClientRendering() {
        ResourceLocation stillTexture = ResourceLocation.fromNamespaceAndPath(modId(), "fluid/" + texture + "_still");
        ResourceLocation flowingTexture = ResourceLocation.fromNamespaceAndPath(modId(), "fluid/" + texture + "_flow");
        FluidRenderHandlerRegistry.INSTANCE.register(SOURCE, FLOWING,
                new SimpleFluidRenderHandler(stillTexture, flowingTexture));

        FogParams params = new FogParams(FluidTextureColor.of(modId(), texture), fogStart, fogEnd);
        FOG_MAP.put(SOURCE, params);
        FOG_MAP.put(FLOWING, params);
    }
}
