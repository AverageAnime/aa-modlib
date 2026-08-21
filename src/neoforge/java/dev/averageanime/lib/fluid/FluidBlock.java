package dev.averageanime.lib.fluid;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.averageanime.lib.client.FluidOverlayRenderer;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;


@SuppressWarnings("unused")
/**
 * Declares a fluid on NeoForge: source, flowing variant, liquid block, bucket and fluid type, plus the
 * client extensions for its texture, tint and fog.
 *
 * <p>Which registers those go into is configuration, so the same builder serves any mod. Fog colour
 * defaults to the average of the still texture rather than a hand-written constant.
 */
public class FluidBlock {

    /**
     * Where a fluid's four registrations go, plus the namespace they go under.
     *
     * <p>Passed in rather than imported because the registers belong to the consuming mod. Configure
     * once during mod construction, before any fluid is declared.
     */
    public record Registrars(String modId,
                             DeferredRegister<Fluid> fluids,
                             DeferredRegister<net.neoforged.neoforge.fluids.FluidType> fluidTypes,
                             DeferredRegister.Blocks blocks,
                             DeferredRegister.Items items) {}

    private static Registrars REGISTRARS;

    public static void configure(Registrars registrars) {
        REGISTRARS = registrars;
    }

    private static Registrars registrars() {
        if (REGISTRARS == null) {
            throw new IllegalStateException(
                    "FluidBlock.configure(...) must be called before any fluid is declared");
        }
        return REGISTRARS;
    }

    private final String name;
    private String texture;
    private Vector3f fogColor = null;
    private int density = 1400;
    private int viscosity = 1500;
    private int lightLevel = 0;
    private int slopeFindDistance = 4;
    private int levelDecreasePerBlock = 3;
    private float fogStart = FluidEntry.DEFAULT_FOG_START;
    private float fogEnd = FluidEntry.DEFAULT_FOG_END;
    private SoundEvent drinkSound = SoundEvents.HONEY_DRINK;

    public FluidBlock(String name) {
        this.name = name;
        this.texture = name;
    }

    public FluidBlock tex(String texture) {
        this.texture = texture;
        return this;
    }

    public FluidBlock color(float r, float g, float b) {
        this.fogColor = new Vector3f(r, g, b);
        return this;
    }

    public FluidBlock color(Vector3f color) {
        this.fogColor = color;
        return this;
    }

    public FluidBlock physics(int density, int viscosity) {
        this.density = density;
        this.viscosity = viscosity;
        return this;
    }

    public FluidBlock flow(int slopeFindDistance, int levelDecreasePerBlock) {
        this.slopeFindDistance = slopeFindDistance;
        this.levelDecreasePerBlock = levelDecreasePerBlock;
        return this;
    }

    public FluidBlock lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    public FluidBlock fog(float fogStart, float fogEnd) {
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        return this;
    }

    public FluidBlock drinkSound(SoundEvent sound) {
        this.drinkSound = sound;
        return this;
    }

    public FluidType build() {
        Vector3f finalColor = fogColor;
        if (finalColor == null) {
            finalColor = FluidTextureColor.of(registrars().modId(), texture);
        }

        net.neoforged.neoforge.fluids.FluidType.Properties properties = net.neoforged.neoforge.fluids.FluidType.Properties.create()
                .lightLevel(lightLevel)
                .sound(SoundAction.get("drink"), drinkSound)
                .density(density)
                .viscosity(viscosity);

        return new FluidType(name, texture, finalColor, properties, slopeFindDistance, levelDecreasePerBlock, fogStart, fogEnd);
    }

    @SuppressWarnings("NullableProblems")
    public static class FluidType {
        public final DeferredHolder<net.neoforged.neoforge.fluids.FluidType, net.neoforged.neoforge.fluids.FluidType> FLUID_TYPE;
        public final DeferredHolder<Fluid, FlowingFluid> SOURCE;
        public final DeferredHolder<Fluid, FlowingFluid> FLOWING;
        public final DeferredBlock<LiquidBlock> BLOCK;
        public final DeferredItem<Item> BUCKET;
        private final ResourceLocation stillTexture;
        private final ResourceLocation flowingTexture;
        private final int slopeFindDistance;
        private final int levelDecreasePerBlock;

        public FluidType(String name, String texture, Vector3f fogColor, net.neoforged.neoforge.fluids.FluidType.Properties fluidTypeProperties,
                         int slopeFindDistance, int levelDecreasePerBlock, float fogStart, float fogEnd) {
            this.stillTexture = ResourceLocation.fromNamespaceAndPath(registrars().modId(), "fluid/" + texture + "_still");
            this.flowingTexture = ResourceLocation.fromNamespaceAndPath(registrars().modId(), "fluid/" + texture + "_flow");
            this.slopeFindDistance = slopeFindDistance;
            this.levelDecreasePerBlock = levelDecreasePerBlock;

            FLUID_TYPE = registrars().fluidTypes().register(name, () -> new net.neoforged.neoforge.fluids.FluidType(fluidTypeProperties) {
                @SuppressWarnings("removal")
                @Override
                public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {

                        @Override
                        public ResourceLocation getStillTexture() {
                            return stillTexture;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return flowingTexture;
                        }

                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                            return fogColor;
                        }

                        @Override
                        public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                                    float partialTick, float nearDistance, float farDistance, FogShape shape) {
                            RenderSystem.setShaderFogColor(fogColor.x(), fogColor.y(), fogColor.z());
                            RenderSystem.setShaderFogStart(fogStart);
                            RenderSystem.setShaderFogEnd(fogEnd);
                            RenderSystem.setShaderFogShape(FogShape.CYLINDER);
                        }

                        @Override
                        public void renderOverlay(Minecraft minecraft, PoseStack poseStack) {
                            TextureAtlasSprite sprite = minecraft
                                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
                            FluidOverlayRenderer.render(minecraft, poseStack, sprite);
                        }
                    });
                }
            });

            SOURCE = registrars().fluids().register(name, () -> new BaseFlowingFluid.Source(createFluidPropertiesInternal()));
            FLOWING = registrars().fluids().register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(createFluidPropertiesInternal()));

            BLOCK = registrars().blocks().register(name + "_block",
                    () -> new LiquidBlock(SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
            BUCKET = registrars().items().register(name + "_bucket",
                    () -> new BucketItem(SOURCE.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }

        private BaseFlowingFluid.Properties createFluidPropertiesInternal() {
            return new BaseFlowingFluid.Properties(
                    FLUID_TYPE,
                    SOURCE,
                    FLOWING)
                    .slopeFindDistance(this.slopeFindDistance)
                    .levelDecreasePerBlock(this.levelDecreasePerBlock)
                    .block(BLOCK)
                    .bucket(BUCKET)
                    .tickRate(25);
        }
    }
}